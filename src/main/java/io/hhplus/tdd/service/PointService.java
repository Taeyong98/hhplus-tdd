package io.hhplus.tdd.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;

import java.util.List;

public interface PointService {
    public UserPoint point(long id);
    public List<PointHistory> history(long id);
    public UserPoint charge(long id, long amount);
    public UserPoint use(long id, long amount);
}
