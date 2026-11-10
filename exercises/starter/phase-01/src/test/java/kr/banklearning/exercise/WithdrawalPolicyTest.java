package kr.banklearning.exercise;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawalPolicyTest {
    @ParameterizedTest(name = "balance={0}, amount={1} -> {2}")
    @MethodSource("withdrawalCases")
    void satisfiesWithdrawalContract(String balance, String amount, boolean expected) {
        assertEquals(expected, WithdrawalPolicy.canWithdraw(decimal(balance), decimal(amount)));
    }

    static Stream<Arguments> withdrawalCases() {
        return Stream.of(
                Arguments.of("1000", "400", true),
                Arguments.of("1000", "1000", true),
                Arguments.of("1000", "1001", false),
                Arguments.of("0", "1", false),
                Arguments.of("1000", "0", false),
                Arguments.of("1000", "-1", false),
                Arguments.of("-1", "1", false),
                Arguments.of("-1", "-2", false),
                Arguments.of("1000.00", "1000.000", true),
                Arguments.of("1000.00", "100.00", true),
                Arguments.of("1E+3", "1E+2", true),
                Arguments.of("1000", "100.1", false),
                Arguments.of("1000.1", "100", false),
                Arguments.of("1", "0.0000000000000000001", false),
                Arguments.of(null, "1", false),
                Arguments.of("1", null, false),
                Arguments.of(null, null, false));
    }

    @Test
    void preservesPrecisionAboveDoubleIntegerRange() {
        BigDecimal balance = new BigDecimal("9007199254740992");
        BigDecimal tooMuch = new BigDecimal("9007199254740993");
        assertFalse(WithdrawalPolicy.canWithdraw(balance, tooMuch));
        assertTrue(WithdrawalPolicy.canWithdraw(tooMuch, balance));
    }

    @Test
    void comparesVeryLargeIntegerAmountsWithoutLongOverflow() {
        BigDecimal balance = new BigDecimal("100000000000000000000000000000000000000");
        BigDecimal amount = new BigDecimal("99999999999999999999999999999999999999");
        assertTrue(WithdrawalPolicy.canWithdraw(balance, amount));
        assertFalse(WithdrawalPolicy.canWithdraw(amount, balance));
    }

    @Test
    void leavesInputsAndTheirScaleUnchanged() {
        BigDecimal balance = new BigDecimal("1000.00");
        BigDecimal amount = new BigDecimal("100.000");
        assertTrue(WithdrawalPolicy.canWithdraw(balance, amount));
        assertEquals(new BigDecimal("1000.00"), balance);
        assertEquals(new BigDecimal("100.000"), amount);
        assertEquals(2, balance.scale());
        assertEquals(3, amount.scale());
    }

    @Test
    void repeatedChecksDoNotConsumeBalanceOrKeepTransactionState() {
        BigDecimal balance = new BigDecimal("100");
        BigDecimal amount = new BigDecimal("75");
        assertTrue(WithdrawalPolicy.canWithdraw(balance, amount));
        assertFalse(WithdrawalPolicy.canWithdraw(balance, new BigDecimal("101")));
        assertTrue(WithdrawalPolicy.canWithdraw(balance, amount));
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
