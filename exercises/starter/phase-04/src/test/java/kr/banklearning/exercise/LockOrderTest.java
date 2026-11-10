package kr.banklearning.exercise;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LockOrderTest {
    @Test
    void returnsSameOrderWhenTransferDirectionChanges() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        assertEquals(List.of(first, second), LockOrder.order(first, second));
        assertEquals(List.of(first, second), LockOrder.order(second, first));
    }

    @Test
    void respectsUnsignedMostSignificantBoundary() {
        UUID lower = UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff");
        UUID higher = UUID.fromString("80000000-0000-0000-0000-000000000000");
        assertEquals(List.of(lower, higher), LockOrder.order(higher, lower));
    }

    @Test
    void respectsUnsignedLeastSignificantBoundaryWhenPrefixMatches() {
        UUID lower = UUID.fromString("12345678-1234-1234-7fff-ffffffffffff");
        UUID higher = UUID.fromString("12345678-1234-1234-8000-000000000000");
        assertEquals(List.of(lower, higher), LockOrder.order(higher, lower));
    }

    @Test
    void matchesUnsignedByteOrderForReproduciblePairsAndTheirReversals() {
        Random random = new Random(20261025L);
        for (int index = 0; index < 256; index++) {
            UUID first = new UUID(random.nextLong(), random.nextLong());
            UUID second = new UUID(random.nextLong(), random.nextLong());
            List<UUID> expected = Arrays.compareUnsigned(bytes(first), bytes(second)) < 0
                    ? List.of(first, second) : List.of(second, first);
            assertEquals(expected, LockOrder.order(first, second), "pair " + index);
            assertEquals(expected, LockOrder.order(second, first), "reversed pair " + index);
        }
    }

    @Test
    void returnsImmutableListAndDoesNotAlterInputValues() {
        UUID first = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("40000000-0000-0000-0000-000000000002");
        List<UUID> result = LockOrder.order(second, first);
        assertThrows(UnsupportedOperationException.class, () -> result.add(UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, second));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
        assertEquals("40000000-0000-0000-0000-000000000001", first.toString());
        assertEquals("40000000-0000-0000-0000-000000000002", second.toString());
        assertEquals(List.of(first, second), result);
    }

    @Test
    void rejectsEqualValuesEvenWhenTheyAreDifferentInstances() {
        UUID first = UUID.fromString("40000000-0000-0000-0000-000000000001");
        UUID equal = UUID.fromString(first.toString());
        assertThrows(IllegalArgumentException.class, () -> LockOrder.order(first, equal));
    }

    @Test
    void rejectsNullAtEitherPosition() {
        UUID id = UUID.fromString("40000000-0000-0000-0000-000000000001");
        assertThrows(IllegalArgumentException.class, () -> LockOrder.order(null, id));
        assertThrows(IllegalArgumentException.class, () -> LockOrder.order(id, null));
        assertThrows(IllegalArgumentException.class, () -> LockOrder.order(null, null));
    }

    private static byte[] bytes(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits()).array();
    }
}
