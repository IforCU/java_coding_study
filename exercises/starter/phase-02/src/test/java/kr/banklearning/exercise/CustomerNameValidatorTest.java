package kr.banklearning.exercise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerNameValidatorTest {
    @Test
    void normalizesLeadingAndTrailingWhitespace() {
        assertEquals("학습 고객", CustomerNameValidator.normalize(" \t학습 고객\r\n"));
    }

    @Test
    void handlesJavaWhitespaceBeyondAsciiSpace() {
        assertEquals("고객", CustomerNameValidator.normalize("\u2003고객\u2003"));
    }

    @Test
    void preservesInternalWhitespaceAndCase() {
        assertEquals("Kim  학습", CustomerNameValidator.normalize("  Kim  학습  "));
    }

    @Test
    void acceptsOneCharacter() {
        assertEquals("가", CustomerNameValidator.normalize(" 가 "));
    }

    @Test
    void acceptsExactlyEightyCharactersAfterNormalization() {
        String name = "가".repeat(80);
        assertEquals(name, CustomerNameValidator.normalize(" \t" + name + " \n"));
    }

    @Test
    void rejectsEightyOneCharactersAfterNormalization() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomerNameValidator.normalize(" " + "가".repeat(81) + " "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\r\n", "\u2003"})
    void rejectsMissingOrWhitespaceOnlyName(String name) {
        assertThrows(IllegalArgumentException.class, () -> CustomerNameValidator.normalize(name));
    }

    @Test
    void repeatedNormalizationIsStable() {
        String first = CustomerNameValidator.normalize("  학습고객02  ");
        assertEquals(first, CustomerNameValidator.normalize(first));
    }
}
