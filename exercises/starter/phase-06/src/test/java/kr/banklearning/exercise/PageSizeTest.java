package kr.banklearning.exercise;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageSizeTest {
    @ParameterizedTest
    @MethodSource("acceptedSizes")
    void resolvesMissingAndValidSizes(Integer supplied, int expected) {
        assertEquals(expected, PageSize.resolve(supplied));
    }

    static Stream<Arguments> acceptedSizes() {
        return Stream.of(Arguments.of(null, 50), Arguments.of(1, 1), Arguments.of(50, 50),
                Arguments.of(199, 199), Arguments.of(200, 200));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 201, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsOutOfRangeSizeWithoutEchoingInput(int supplied) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> PageSize.resolve(supplied));
        assertFalse(String.valueOf(exception.getMessage()).contains(Integer.toString(supplied)));
    }

    @Test
    void doesNotRetainStateBetweenRequests() {
        assertEquals(1, PageSize.resolve(1));
        assertEquals(50, PageSize.resolve(null));
        assertEquals(200, PageSize.resolve(200));
        assertEquals(50, PageSize.resolve(null));
    }
}
