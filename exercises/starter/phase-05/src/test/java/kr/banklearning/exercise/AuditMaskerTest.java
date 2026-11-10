package kr.banklearning.exercise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditMaskerTest {
    @Test
    void removesAsciiSpaceAndHyphenBeforeMasking() {
        assertEquals("********9012", AuditMasker.maskAccount(" 1234-5678 9012 "));
    }

    @Test
    void acceptsEightDigitsAndPreservesLeadingZeros() {
        assertEquals("****0001", AuditMasker.maskAccount("00000001"));
    }

    @Test
    void acceptsExactlyTwentyDigits() {
        assertEquals("****************7890", AuditMasker.maskAccount("12345678901234567890"));
    }

    @Test
    void exposesOnlyLastFourDigitsWithNormalizedLength() {
        String input = "1234-5678-9012-3456";
        String normalized = "1234567890123456";
        String masked = AuditMasker.maskAccount(input);
        assertEquals(normalized.length(), masked.length());
        assertTrue(masked.matches("\\*{12}[0-9]{4}"));
        assertEquals("3456", masked.substring(masked.length() - 4));
        assertFalse(masked.contains(normalized));
        assertFalse(masked.contains(input));
        assertEquals("1234-5678-9012-3456", input);
    }

    @Test
    void returnsStableResultForRepeatedInput() {
        String input = "1234 5678";
        assertEquals(AuditMasker.maskAccount(input), AuditMasker.maskAccount(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " - - ", "1234567", "123456789012345678901", "1234abcd", "****5678",
            "１２３４５６７８", "١٢٣٤٥٦٧٨", "1234\t5678", "1234\n5678", "1234\u20035678",
            "1234\u20105678", "1234\u200b5678", "12345678' OR 1=1 --", "<script>12345678</script>"
    })
    void rejectsNullWrongLengthAndNonAsciiDigits(String input) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> AuditMasker.maskAccount(input));
        if (input != null && !input.isBlank()) {
            assertFalse(String.valueOf(exception.getMessage()).contains(input), "오류에도 원문을 노출하지 않습니다.");
        }
    }
}
