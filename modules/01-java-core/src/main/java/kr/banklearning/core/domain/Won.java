package kr.banklearning.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import kr.banklearning.core.exception.InvalidAmountException;

/** 반올림으로 금액을 바꾸지 않고 원 단위의 정확한 값을 보존한다. */
public final class Won {
    private Won() {}

    public static BigDecimal positive(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException();
        }
        final BigDecimal normalized;
        try {
            normalized = amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidAmountException();
        }
        if (normalized.signum() <= 0) {
            throw new InvalidAmountException();
        }
        return normalized;
    }
}
