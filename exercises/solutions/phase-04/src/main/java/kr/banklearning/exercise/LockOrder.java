package kr.banklearning.exercise;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class LockOrder {
    private LockOrder() {}

    public static List<UUID> order(UUID first, UUID second) {
        if (first == null || second == null || first.equals(second)) {
            throw new IllegalArgumentException("서로 다른 두 계좌가 필요합니다.");
        }
        Comparator<UUID> databaseOrder = Comparator.comparing(UUID::toString);
        return databaseOrder.compare(first, second) < 0 ? List.of(first, second) : List.of(second, first);
    }
}
