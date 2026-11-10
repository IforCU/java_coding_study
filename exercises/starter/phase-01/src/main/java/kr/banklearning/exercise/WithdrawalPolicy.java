package kr.banklearning.exercise;

import java.math.BigDecimal;

/** Phase 1 독립 실습. 정상 실행 모듈에서는 사용하지 않는다. */
public final class WithdrawalPolicy {
    private WithdrawalPolicy() {}

    /**
     * 원 단위 잔액과 출금액을 받아 출금 가능 여부를 반환한다.
     * 계약의 자세한 조건은 lessons/phase-01/exercise.md와 테스트를 참고한다.
     */
    public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
        // TODO: 입력을 변경하지 않는 원 단위 출금 가능 정책을 구현하세요.
        throw new UnsupportedOperationException("TODO: 출금 가능 정책 구현");
    }
}
