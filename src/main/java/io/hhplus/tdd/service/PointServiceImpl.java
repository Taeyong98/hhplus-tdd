package io.hhplus.tdd.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService{
    //
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint point(long id) {
        //
        return userPointTable.selectById(id);
    }

    @Override
    public List<PointHistory> history(long id) {
        //
        return pointHistoryTable.selectAllByUserId(id);
    }

    @Override
    public UserPoint charge(long id, long amount) {
        //
        if(amount < 0) {
            throw new IllegalArgumentException("포인트는 양수여야합니다.");
        }
        UserPoint userPoint = userPointTable.selectById(id);
        long point = userPoint.point();
        point += amount;
        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(id, point);
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return updatedUserPoint;
    }

    @Override
    public UserPoint use(long id, long amount) {
        //
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
    }
}
