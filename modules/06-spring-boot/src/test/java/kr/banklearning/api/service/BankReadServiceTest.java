package kr.banklearning.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;
import kr.banklearning.api.repository.BankReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankReadServiceTest {
    @Mock BankReadRepository repository;
    private BankReadService service;
    private UUID accountId;
    private AccountView account;

    @BeforeEach
    void setUp() {
        service =
                new BankReadService(
                        repository,
                        Clock.fixed(Instant.parse("2026-10-25T15:00:00Z"), ZoneOffset.UTC));
        accountId = UUID.randomUUID();
        account =
                new AccountView(
                        accountId,
                        BankReadService.LEARNER_CUSTOMER,
                        new BigDecimal("5000"),
                        new BigDecimal("1000"),
                        3);
    }

    @Test
    void learnerCannotReadAnotherCustomerBeforeSqlRuns() {
        assertEquals(
                "FORBIDDEN",
                assertThrows(
                                BankProblem.class,
                                () -> service.customer(UUID.randomUUID(), "learner", false))
                        .code());
        assertEquals(
                "FORBIDDEN",
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.customer(
                                                BankReadService.LEARNER_CUSTOMER,
                                                "intruder",
                                                false))
                        .code());
        verifyNoInteractions(repository);
    }

    @Test
    void ownCustomerAndAdminCustomerUseRepository() {
        var customer = new CustomerView(BankReadService.LEARNER_CUSTOMER, "학습고객01", "ACTIVE");
        when(repository.customer(customer.id())).thenReturn(Optional.of(customer));
        assertEquals(customer, service.customer(customer.id(), "learner", false));
        assertEquals(customer, service.customer(customer.id(), "admin", true));
        verify(repository, times(2)).customer(customer.id());
    }

    @Test
    void foreignAccountPreventsLedgerAndLimitReads() {
        when(repository.account(accountId))
                .thenReturn(
                        Optional.of(
                                new AccountView(
                                        accountId,
                                        UUID.randomUUID(),
                                        BigDecimal.TEN,
                                        BigDecimal.TEN,
                                        0)));
        assertEquals(
                403,
                assertThrows(
                                BankProblem.class,
                                () ->
                                        service.transactions(
                                                accountId, null, null, 50, "learner", false))
                        .status());
        assertEquals(
                403,
                assertThrows(BankProblem.class, () -> service.limit(accountId, "learner", false))
                        .status());
        verify(repository, never()).transactions(any(), any(), any(), anyInt());
        verify(repository, never()).dailyUsed(any(), any());
    }

    @Test
    void missingAccountIs404() {
        when(repository.account(accountId)).thenReturn(Optional.empty());
        assertEquals(
                404,
                assertThrows(BankProblem.class, () -> service.account(accountId, "admin", true))
                        .status());
    }

    @Test
    void ledgerDatesAreConvertedToSeoulHalfOpenBoundsAndLimitIsPassed() {
        when(repository.account(accountId)).thenReturn(Optional.of(account));
        OffsetDateTime from = OffsetDateTime.parse("2026-10-01T00:00:00+09:00"),
                to = OffsetDateTime.parse("2026-10-26T00:00:00+09:00");
        when(repository.transactions(accountId, from, to, 17)).thenReturn(new ArrayList<>());
        List<TransactionView> result =
                service.transactions(accountId, "2026-10-01", "2026-10-25", 17, "learner", false);
        verify(repository).transactions(accountId, from, to, 17);
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    @Test
    void invalidLimitsNeverReachRepository() {
        assertThrows(
                BankProblem.class,
                () -> service.transactions(accountId, null, null, 0, "learner", false));
        assertThrows(
                BankProblem.class,
                () -> service.transactions(accountId, null, null, 201, "learner", false));
        verifyNoInteractions(repository);
    }

    @Test
    void invalidAndReversedDatesNeverReachLedgerSql() {
        when(repository.account(accountId)).thenReturn(Optional.of(account));
        assertThrows(
                BankProblem.class,
                () -> service.transactions(accountId, "2026-02-30", null, 50, "learner", false));
        assertThrows(
                BankProblem.class,
                () ->
                        service.transactions(
                                accountId, "2026-10-26", "2026-10-25", 50, "learner", false));
        verify(repository, never()).transactions(any(), any(), any(), anyInt());
    }

    @Test
    void dailyLimitUsesBusinessDateAndNeverReturnsNegativeRemaining() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        when(repository.account(accountId)).thenReturn(Optional.of(account));
        when(repository.dailyUsed(accountId, date))
                .thenReturn(new BigDecimal("300"), new BigDecimal("1100"));
        var first = service.limit(accountId, "learner", false);
        assertEquals(date, first.businessDate());
        assertEquals(new BigDecimal("700"), first.remaining());
        assertEquals(BigDecimal.ZERO, service.limit(accountId, "learner", false).remaining());
    }

    @Test
    void statisticsRequireAdminBeforeAnySql() {
        assertEquals(
                403,
                assertThrows(BankProblem.class, () -> service.statistics(null, null, false))
                        .status());
        verifyNoInteractions(repository);
    }

    @Test
    void statisticsDefaultDatesAndMaximum366DayRange() {
        LocalDate today = LocalDate.of(2026, 10, 26);
        when(repository.statistics(today, today))
                .thenReturn(new StatisticsView(today, today, 0, BigDecimal.ZERO));
        assertEquals(today, service.statistics(null, null, true).from());
        service.statistics("2026-01-01", "2027-01-01", true);
        verify(repository).statistics(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
        assertThrows(BankProblem.class, () -> service.statistics("2026-01-01", "2027-01-02", true));
        service.statistics(null, "2026-10-01", true);
        verify(repository).statistics(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1));
    }

    @Test
    void repositoryFailuresPropagateToCommonHttpBoundary() {
        when(repository.account(accountId))
                .thenThrow(
                        new org.springframework.dao.DataAccessResourceFailureException(
                                "secret database detail"));
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> service.account(accountId, "learner", false));
    }
}
