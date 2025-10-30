package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.service.PointServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PointConcurrencyTest {
    //
    @Autowired
    private PointServiceImpl pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Test
    void testChargingConcurrency() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 10;
        long chargeAmount = 100L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        UserPoint finalPoint = pointService.point(userId);
        assertThat(finalPoint.point()).isEqualTo(1000L);

        // 모든 충전 히스토리가 기록되어야 함
        List<PointHistory> histories = pointService.history(userId);
        assertThat(histories).hasSize(threadCount);
    }

    @Test
    void testUsingConcurrency() throws InterruptedException {
        // given
        long userId = 2L;
        pointService.charge(userId, 1000L); // 초기 1000 포인트

        int threadCount = 10;
        long useAmount = 50L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        //then
        UserPoint finalPoint = pointService.point(userId);
        assertThat(finalPoint.point()).isEqualTo(500L);

        List<PointHistory> histories = pointService.history(userId);
        assertThat(histories).hasSize(11);
    }

    @Test
    void testConcurrentChargeAndUseGuaranteesOrder() throws InterruptedException {
        // given
        long userId = 3L;
        pointService.charge(userId, 1000L); // 초기 1000 포인트

        int operationCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(operationCount * 2);

        // when
        // 100씩 사용.
        for (int i = 0; i < operationCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, 100L);
                } finally {
                    latch.countDown();
                }
            });
        // 50 씩 사용
            executorService.submit(() -> {
                try {
                    pointService.use(userId, 50L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        UserPoint finalPoint = pointService.point(userId);
        assertThat(finalPoint.point()).isEqualTo(1500L);

        List<PointHistory> histories = pointService.history(userId);
        assertThat(histories).hasSize(21);
    }
}
