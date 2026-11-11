package kr.banklearning.transaction.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import kr.banklearning.transaction.MemoryTransferRepository;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TransferServiceTest {
    private MemoryTransferRepository repository;
    private TransferService service;
    private UUID source, destination;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 25);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        repository = new MemoryTransferRepository();
        source = UUID.randomUUID();
        destination = UUID.randomUUID();
        repository.accounts.put(
                source,
                new Account(
                        source,
                        UUID.randomUUID(),
                        new BigDecimal("5000"),
                        new BigDecimal("1000"),
                        0));
        repository.accounts.put(
                destination,
                new Account(
                        destination,
                        UUID.randomUUID(),
                        new BigDecimal("500"),
                        new BigDecimal("1000"),
                        0));
        service = new TransferService(repository, CLOCK, () -> {});
    }

    private TransferCommand command(String amount) {
        return new TransferCommand(UUID.randomUUID(), source, destination, new BigDecimal(amount));
    }

    @Test
    void transferCreatesExactlyTwoBalancedLedgerEntriesAndUsage() {
        TransferReceipt receipt = service.transfer(command("300.00"));
        assertEquals("COMPLETED", receipt.status());
        assertEquals(DATE, receipt.businessDate());
        assertEquals(new BigDecimal("4700"), repository.accounts.get(source).balance());
        assertEquals(new BigDecimal("800"), repository.accounts.get(destination).balance());
        assertEquals(new BigDecimal("300"), repository.dailyUsed(source, DATE));
        assertEquals(2, repository.entries.size());
        assertEquals(
                List.of("WITHDRAWAL", "DEPOSIT"),
                repository.entries.stream().map(entry -> entry.type()).toList());
        assertTrue(
                repository.entries.stream()
                        .allMatch(
                                entry ->
                                        entry.requestId().equals(receipt.requestId())
                                                && entry.amount().equals(new BigDecimal("300"))));
        assertEquals(new BigDecimal("4700"), repository.entries.get(0).balanceAfter());
        assertEquals(new BigDecimal("800"), repository.entries.get(1).balanceAfter());
        assertNotEquals(repository.entries.get(0).id(), repository.entries.get(1).id());
    }

    @Test
    void sameRequestCanBeReplayedAfterLostResponseWithoutAnotherDebit() {
        TransferCommand command = command("300");
        TransferReceipt first = service.transfer(command);
        TransferReceipt replay =
                service.transfer(
                        new TransferCommand(
                                command.requestId(),
                                source,
                                destination,
                                new BigDecimal("300.00")));
        assertEquals(first, replay);
        assertEquals(2, repository.entries.size());
        assertEquals(1, repository.accounts.get(source).version());
        assertEquals(new BigDecimal("300"), repository.dailyUsed(source, DATE));
    }

    @Test
    void changedPayloadWithSameKeyIsConflictWithoutNewLedger() {
        TransferCommand command = command("300");
        service.transfer(command);
        assertEquals(
                "REQUEST_CONFLICT",
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.transfer(
                                                new TransferCommand(
                                                        command.requestId(),
                                                        source,
                                                        destination,
                                                        new BigDecimal("200"))))
                        .code());
        assertEquals(2, repository.entries.size());
        assertEquals(new BigDecimal("4700"), repository.accounts.get(source).balance());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-1", "1.01", "10000000000000000000"})
    void invalidAmountsNeverReserveRequest(String input) {
        BigDecimal amount = input == null ? null : new BigDecimal(input);
        assertEquals(
                "INVALID_AMOUNT",
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.transfer(
                                                new TransferCommand(
                                                        UUID.randomUUID(),
                                                        source,
                                                        destination,
                                                        amount)))
                        .code());
        assertEquals(0, repository.reserveCalls);
        assertTrue(repository.entries.isEmpty());
    }

    @Test
    void invalidIdsAndSameAccountNeverReserve() {
        assertEquals(
                "SAME_ACCOUNT",
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.transfer(
                                                new TransferCommand(
                                                        UUID.randomUUID(),
                                                        source,
                                                        source,
                                                        BigDecimal.ONE)))
                        .code());
        assertEquals(
                "INVALID_ID",
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.transfer(
                                                new TransferCommand(
                                                        null, source, destination, BigDecimal.ONE)))
                        .code());
        assertEquals(0, repository.reserveCalls);
    }

    @Test
    void insufficientBalanceDoesNotReachBalanceOrLedgerUpdates() {
        assertEquals(
                "INSUFFICIENT_FUNDS",
                assertThrows(BankProblem.class, () -> service.transfer(command("5001"))).code());
        assertEquals(new BigDecimal("5000"), repository.accounts.get(source).balance());
        assertTrue(repository.entries.isEmpty());
        assertTrue(repository.usage.isEmpty());
        // PENDING 예약 rollback은 이 메모리 대역이 아니라 실제 DB 통합 테스트가 검증한다.
    }

    @Test
    void dailyLimitAllowsExactBoundaryAndRejectsOneMoreWon() {
        service.transfer(command("900"));
        service.transfer(command("100"));
        assertEquals(
                "DAILY_LIMIT",
                assertThrows(BankProblem.class, () -> service.transfer(command("1"))).code());
        assertEquals(new BigDecimal("1000"), repository.dailyUsed(source, DATE));
        assertEquals(new BigDecimal("4000"), repository.accounts.get(source).balance());
        assertEquals(4, repository.entries.size());
    }

    @Test
    void businessDateUsesSeoulMidnightAndSeparatesDailyUsage() {
        repository.setDailyUsed(source, DATE, new BigDecimal("1000"));
        TransferService afterMidnight =
                new TransferService(
                        repository,
                        Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC),
                        () -> {});
        TransferReceipt receipt = afterMidnight.transfer(command("100"));
        assertEquals(DATE.plusDays(1), receipt.businessDate());
        assertEquals(new BigDecimal("1000"), repository.dailyUsed(source, DATE));
        assertEquals(new BigDecimal("100"), repository.dailyUsed(source, DATE.plusDays(1)));
    }

    @Test
    void missingAccountAndDestinationOverflowStopBeforeDebit() {
        repository.accounts.remove(destination);
        assertEquals(
                "ACCOUNT_NOT_FOUND",
                assertThrows(BankProblem.class, () -> service.transfer(command("1"))).code());
        repository.accounts.put(
                destination,
                new Account(
                        destination,
                        UUID.randomUUID(),
                        new BigDecimal("9999999999999999999"),
                        BigDecimal.TEN,
                        0));
        assertEquals(
                "BALANCE_LIMIT",
                assertThrows(BankProblem.class, () -> service.transfer(command("1"))).code());
        assertEquals(0, repository.accounts.get(source).version());
        assertTrue(repository.entries.isEmpty());
    }

    @Test
    void cancellationLocksRequestAndDoesNotChangeBalancesLedgerOrDailyUsage() {
        TransferReceipt receipt = service.transfer(command("300"));
        var beforeAccounts = repository.accounts();
        var beforeLedger = List.copyOf(repository.entries);
        TransferReceipt cancellation = service.requestCancellation(receipt.requestId());
        assertEquals("CANCEL_REQUESTED", cancellation.status());
        assertTrue(repository.cancellationLocked);
        assertEquals(cancellation, service.requestCancellation(receipt.requestId()));
        assertEquals(
                cancellation,
                service.transfer(
                        new TransferCommand(
                                receipt.requestId(), source, destination, receipt.amount())));
        assertEquals(beforeAccounts, repository.accounts());
        assertEquals(beforeLedger, repository.entries);
        assertEquals(new BigDecimal("300"), repository.dailyUsed(source, DATE));
    }

    @Test
    void pendingRequestCannotBeCancelledOrExecutedAgain() {
        TransferCommand command = command("300");
        repository.reserve(command, DATE);
        assertEquals(
                "REQUEST_PENDING",
                assertThrows(BankProblem.class, () -> service.transfer(command)).code());
        assertEquals(
                "CANCEL_STATE",
                assertThrows(
                                BankProblem.class,
                                () -> service.requestCancellation(command.requestId()))
                        .code());
        assertTrue(repository.entries.isEmpty());
    }

    @Test
    void injectedFailurePropagatesToSpringTransactionBoundary() {
        TransferService failing =
                new TransferService(
                        repository,
                        CLOCK,
                        () -> {
                            throw new IllegalStateException("injected");
                        });
        assertThrows(IllegalStateException.class, () -> failing.transfer(command("100")));
        assertTrue(repository.entries.isEmpty());
        // rollback 여부는 직접 생성한 서비스로 증명할 수 없다. TransferServiceIT가 실제 프록시/DB로 검증한다.
    }

    @Test
    void queriesReturnImmutableSnapshotsAndMissingRequestIsTypedFailure() {
        var before = service.accounts();
        service.transfer(command("100"));
        assertThrows(UnsupportedOperationException.class, before::clear);
        assertEquals(
                new BigDecimal("5000"),
                before.stream()
                        .filter(account -> account.id().equals(source))
                        .findFirst()
                        .orElseThrow()
                        .balance());
        assertEquals(
                404,
                assertThrows(BankProblem.class, () -> service.get(UUID.randomUUID())).status());
    }
}
