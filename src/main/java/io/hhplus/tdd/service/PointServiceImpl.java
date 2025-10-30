package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService{
    //
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ReadWriteLock rw = new ReentrantReadWriteLock();


    private <T> T withRead(Supplier<T> action) {
        //
        rw.readLock().lock();
        try { return action.get(); }
        finally { rw.readLock().unlock(); }
    }

    private <T> T withWrite(Supplier<T> action) {
        //
        rw.writeLock().lock();
        try { return action.get(); }
        finally { rw.writeLock().unlock(); }
    }


    @Override
    public UserPoint point(long id) {
        //
        return withRead(()->
            userPointTable.selectById(id)
        );
    }

    @Override
    public List<PointHistory> history(long id) {
        //
        return withRead(()->
                pointHistoryTable.selectAllByUserId(id)
        );
    }

    @Override
    public UserPoint charge(long id, long amount) {
        //
        return withWrite(()->{
            if(amount < 0) {
                throw new IllegalArgumentException("포인트는 양수여야합니다.");
            }
            UserPoint userPoint = userPointTable.selectById(id);
            long point = userPoint.point();
            point += amount;
            UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, point);
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return updatedUserPoint;
        });
    }

    @Override
    public UserPoint use(long id, long amount) {
        //
        return withWrite(()->{
            if(amount < 0) {
                throw new IllegalArgumentException("포인트는 양수여야합니다.");
            }
            UserPoint userPoint = userPointTable.selectById(id);
            long point = userPoint.point();
            if(point < amount) {
                throw new IllegalArgumentException("포인트가 부족합니다.");
            }

            point -= amount;
            UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, point);
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return updatedUserPoint;
        });
    }
}
