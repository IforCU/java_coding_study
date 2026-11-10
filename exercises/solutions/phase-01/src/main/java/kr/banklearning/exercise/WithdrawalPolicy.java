package kr.banklearning.exercise;

import java.math.BigDecimal;

public final class WithdrawalPolicy {
    private WithdrawalPolicy() {}

    public static boolean canWithdraw(BigDecimal balance, BigDecimal amount) {
        if (balance == null || amount == null || balance.signum() < 0 || amount.signum() <= 0) {
            return false;
        }
        if (balance.stripTrailingZeros().scale() > 0 || amount.stripTrailingZeros().scale() > 0) {
            return false;
        }
        return balance.compareTo(amount) >= 0;
    }
}
