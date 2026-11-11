package kr.banklearning.exercise;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** 정상 거래 모듈과 분리된 잠금 순서 실습이다. */
public final class LockOrder {
    private LockOrder() {}

    public static List<UUID> order(UUID first, UUID second) {
        // TODO: PostgreSQL UUID 순서와 일치하는 불변 목록을 반환하세요.
        if(first == null || second == null || first.compareTo(second) == 0) {
            throw new IllegalArgumentException("서로 다른 두 계좌가 필요합니다.");
        }

        Comparator<UUID> databaseOrder = Comparator.comparing(UUID::toString);
        return databaseOrder.compare(first, second) < 0 ? List.of(first, second) : List.of(second, first);
    }
}
