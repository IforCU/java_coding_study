package kr.banklearning.exercise;

import java.util.List;
import java.util.UUID;

/** 정상 거래 모듈과 분리된 잠금 순서 실습이다. */
public final class LockOrder {
    private LockOrder() {}

    public static List<UUID> order(UUID first, UUID second) {
        // TODO: PostgreSQL UUID 순서와 일치하는 불변 목록을 반환하세요.
        throw new UnsupportedOperationException("TODO: 잠금 순서 구현");
    }
}
