# PointServiceImpl — 동시성 처리 개요

## 핵심 아이디어
서비스 전역에 ReentrantReadWriteLock을 두어 읽기와 쓰기 작업을 분리 제어한다.

동시 접근 시 Race Condition을 방지하기 위해,
조회(point, history)는 읽기 락(read lock),
충전(charge), 사용(use)은 쓰기 락(write lock) 으로 감싼다.

## 코드
``` java

private final ReadWriteLock rw = new ReentrantReadWriteLock();
private <T> T withRead(Supplier<T> action) {
    rw.readLock().lock();
    try { return action.get(); }
    finally { rw.readLock().unlock(); }
}

private <T> T withWrite(Supplier<T> action) {
    rw.writeLock().lock();
    try { return action.get(); }
    finally { rw.writeLock().unlock(); }
}
```


- withRead(): 읽기 락을 획득해 동시에 여러 스레드의 읽기 허용
- withWrite(): 쓰기 락을 획득해 동시에 한 스레드만 쓰기 가능

## 개선 방향
현재 문제점은 user1, user2가 동시에 접근하려고할 때 lock이 걸리게 된다. 유저마다 고유의 임계구역이 있기 때문에 다른 유저라면 굳이 락을 걸 필요는 없다.
개선한다면 Concurrent HashMap과 같은 자료구조를 사용하여 Storage 레이어에서 동시성을 해결하는 것이 더 낫다고 생각한다.