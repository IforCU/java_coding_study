package kr.banklearning.operations.labs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import org.junit.jupiter.api.Test;

/**
 * 장애 증상을 재현하는 격리된 메모리 모델이다. PASS는 증상 관찰 성공을 뜻한다.
 * private Lab fixture만 사용하며 정상 앱·실제 DB·HTTP 보안의 회귀 검증을 대신하지 않는다.
 */
class MaintenanceLabTest {
    @Test
    void wrongSqlConditionReturnsAnotherOwnersAccount() {
        UUID owner = UUID.randomUUID();
        LabAccount mine = new LabAccount(UUID.randomUUID(), owner, money("100"));
        LabAccount other = new LabAccount(UUID.randomUUID(), UUID.randomUUID(), money("900"));

        List<LabAccount> observed = LabSqlQuery.withoutOwnerCondition(List.of(mine, other), owner);

        assertEquals(2, observed.size());
        assertTrue(
                observed.stream()
                        .anyMatch(
                                account ->
                                        account.id().equals(other.id())
                                                && !account.ownerId().equals(owner)));
    }

    @Test
    void exhaustedPoolFixtureRejectsTheNextBorrow() {
        LabPool pool = new LabPool(1);

        LabLeak.borrowWithoutReturning(pool);

        assertEquals(0, pool.available());
        assertThrows(IllegalStateException.class, pool::borrow);
    }

    @Test
    void missingQueryBudgetAllowsExcessiveWork() {
        int configuredBudget = 100;
        List<Integer> fixtureRows = java.util.stream.IntStream.range(0, 1000).boxed().toList();

        int visitedRows = LabQueryCost.scanWithoutBudget(fixtureRows, configuredBudget);

        assertEquals(1000, visitedRows);
        assertTrue(visitedRows > configuredBudget);
    }

    @Test
    void autoCommitFixtureLeavesTheDebitAfterFailure() {
        LabBank bank = new LabBank(money("1000"), money("100"));

        assertThrows(
                IllegalStateException.class, () -> bank.transferAsSeparateCommits(money("100")));

        assertEquals(money("900"), bank.source);
        assertEquals(money("100"), bank.destination);
        assertEquals(money("1000"), bank.source.add(bank.destination));
        assertNotEquals(money("1100"), bank.source.add(bank.destination));
    }

    @Test
    void missingAuthorizationReturnsTheOtherCustomersAccount() {
        UUID principal = UUID.randomUUID();
        LabAccount other = new LabAccount(UUID.randomUUID(), UUID.randomUUID(), money("900"));

        LabAccount exposed =
                LabAuthorization.findWithoutOwnershipCheck(
                        principal, other.id(), Map.of(other.id(), other));

        assertEquals(other.id(), exposed.id());
        assertNotEquals(principal, exposed.ownerId());
    }

    @Test
    void duplicateRequestFixtureDebitsTwice() {
        LabBank bank = new LabBank(money("1000"), money("100"));
        UUID sameRequest = UUID.randomUUID();

        bank.transferWithoutRequestRegistry(sameRequest, money("100"));
        bank.transferWithoutRequestRegistry(sameRequest, money("100"));

        assertEquals(money("800"), bank.source);
        assertEquals(money("300"), bank.destination);
        assertEquals(2, bank.transferCount);
        assertNotEquals(money("900"), bank.source);
    }

    @Test
    void expiredSessionFixtureStillReturnsAPrincipal() {
        Instant now = Instant.parse("2026-10-25T03:00:00Z");
        LabSession expired = new LabSession(UUID.randomUUID(), now.minusSeconds(1));

        UUID observed = LabAuthentication.principalWithoutExpiryCheck(expired, now);

        assertTrue(expired.expiresAt().isBefore(now));
        assertEquals(expired.principal(), observed);
    }

    @Test
    void swappedColumnMappingCorruptsTheView() {
        Map<String, BigDecimal> row = Map.of("balance", money("100"), "daily_limit", money("500"));

        LabAccountView mapped = LabMapping.swappedColumns(row);

        assertEquals(money("500"), mapped.balance());
        assertEquals(money("100"), mapped.dailyLimit());
        assertNotEquals(money("100"), mapped.balance());
        assertNotEquals(money("500"), mapped.dailyLimit());
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    private record LabAccount(UUID id, UUID ownerId, BigDecimal balance) {}

    private record LabSession(UUID principal, Instant expiresAt) {}

    private record LabAccountView(BigDecimal balance, BigDecimal dailyLimit) {}

    private static final class LabSqlQuery {
        private static List<LabAccount> withoutOwnerCondition(
                List<LabAccount> rows, UUID requestedOwner) {
            return rows.stream().filter(row -> row.balance().signum() >= 0).toList();
        }
    }

    private static final class LabPool {
        private final Semaphore permits;

        private LabPool(int size) {
            permits = new Semaphore(size);
        }

        private LabLease borrow() {
            if (!permits.tryAcquire())
                throw new IllegalStateException("Lab pool has no available lease.");
            return new LabLease(permits);
        }

        private int available() {
            return permits.availablePermits();
        }
    }

    private static final class LabLease implements AutoCloseable {
        private final Semaphore permits;
        private boolean returned;

        private LabLease(Semaphore permits) {
            this.permits = permits;
        }

        @Override
        public void close() {
            if (!returned) {
                returned = true;
                permits.release();
            }
        }
    }

    private static final class LabLeak {
        private static void borrowWithoutReturning(LabPool pool) {
            pool.borrow();
        }
    }

    private static final class LabQueryCost {
        private static int scanWithoutBudget(List<Integer> rows, int configuredBudget) {
            int visited = 0;
            for (Integer ignored : rows) visited++;
            return visited;
        }
    }

    private static final class LabBank {
        private BigDecimal source;
        private BigDecimal destination;
        private int transferCount;

        private LabBank(BigDecimal source, BigDecimal destination) {
            this.source = source;
            this.destination = destination;
        }

        private void transferAsSeparateCommits(BigDecimal amount) {
            source = source.subtract(amount);
            throw new IllegalStateException("Lab failure after the first independent commit.");
        }

        private void transferWithoutRequestRegistry(UUID requestId, BigDecimal amount) {
            source = source.subtract(amount);
            destination = destination.add(amount);
            transferCount++;
        }
    }

    private static final class LabAuthorization {
        private static LabAccount findWithoutOwnershipCheck(
                UUID principal, UUID accountId, Map<UUID, LabAccount> accounts) {
            return accounts.get(accountId);
        }
    }

    private static final class LabAuthentication {
        private static UUID principalWithoutExpiryCheck(LabSession session, Instant now) {
            return session == null ? null : session.principal();
        }
    }

    private static final class LabMapping {
        private static LabAccountView swappedColumns(Map<String, BigDecimal> row) {
            return new LabAccountView(row.get("daily_limit"), row.get("balance"));
        }
    }
}
