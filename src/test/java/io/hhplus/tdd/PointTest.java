package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.*;
import io.hhplus.tdd.service.PointServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PointTest {
    //
    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointServiceImpl pointService;

    @Test
    void testFindUserPoint() {
        // given
        long userId = 1L;
        UserPoint expectedUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);

        // when
        UserPoint result = pointService.point(userId);

        // then
        assertThat(result).isEqualTo(expectedUserPoint);
    }

    @Test
    void testFindEmptyUserPoint() {
        // given
        long userId = 1L;
        UserPoint emptyUserPoint = UserPoint.empty(userId);
        when(userPointTable.selectById(userId)).thenReturn(emptyUserPoint);

        // when
        UserPoint result = pointService.point(userId);

        // then
        assertThat(result.point()).isEqualTo(0L);
        assertThat(result.id()).isEqualTo(userId);
    }

    @Test
    void testFindPointHistories() {
        // given
        long userId = 1L;
        List<PointHistory> expectedHistory = Arrays.asList(
            new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
            new PointHistory(2L, userId, 500L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistory);

        // when
        List<PointHistory> result = pointService.history(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedHistory);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    void testFindEmptyPointHistories() {
        // given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(Collections.emptyList());

        // when
        List<PointHistory> result = pointService.history(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testChargePoint() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint existingPoint = new UserPoint(userId, 500L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId))
            .thenReturn(existingPoint);
        when(userPointTable.insertOrUpdate(userId, 1500L)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(1500L);
    }

    @Test
    void testChargePointWhenUserPointIsEmpty() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint emptyPoint = UserPoint.empty(userId);
        UserPoint chargedPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId))
            .thenReturn(emptyPoint)
            .thenReturn(chargedPoint);
        when(userPointTable.insertOrUpdate(userId, 1000L)).thenReturn(chargedPoint);

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertThat(result.point()).isEqualTo(1000L);
    }

    @Test
    void testUsePoint() {
        // given
        long userId = 1L;
        long useAmount = 300L;
        UserPoint existingPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint updatedPoint = new UserPoint(userId, 700L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId))
            .thenReturn(existingPoint)
            .thenReturn(updatedPoint);
        when(userPointTable.insertOrUpdate(userId, 700L)).thenReturn(updatedPoint);

        // when
        UserPoint result = pointService.use(userId, useAmount);

        // then
        assertThat(result.point()).isEqualTo(700L);
        verify(userPointTable).insertOrUpdate(userId, 700L);
        verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    void testUsePointWhenPointIsNotEnough() {
        // given
        long userId = 1L;
        long useAmount = 1500L;
        UserPoint existingPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        
        when(userPointTable.selectById(userId)).thenReturn(existingPoint);

        // when, then
        assertThatThrownBy(()->{
            pointService.use(userId, useAmount);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPointHistoriesAreInserted() {
        //given
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 500L;

        when(userPointTable.selectById(userId))
                .thenReturn(UserPoint.empty(userId))
                .thenReturn(new UserPoint(1L, 1000L, System.currentTimeMillis()))
                .thenReturn(new UserPoint(1L, 500L, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(userId, 1000L))
                .thenReturn(new UserPoint(1L, 1000L, System.currentTimeMillis()));
        when(userPointTable.insertOrUpdate(userId, 0L))
                .thenReturn(new UserPoint(1L, 0L, System.currentTimeMillis()));
        when(pointHistoryTable.insert(anyLong(), anyLong(), any(TransactionType.class), anyLong()))
                .thenReturn(new PointHistory(1L, userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis()));

        //when
        pointService.charge(userId, chargeAmount);
        pointService.use(userId, chargeAmount);

        //then
        verify(pointHistoryTable, times(2)).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
    }
}
