package kr.banklearning.mvc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import kr.banklearning.mvc.StubMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BankServiceTest {
    @Test
    void customerStatusPreventsNewAccountUntilReactivated() {
        StubMapper mapper = new StubMapper();
        BankService bank = new BankService(mapper);
        var customer = bank.createCustomer(" 학습고객01 ");
        var first = bank.createAccount(customer.id());
        bank.changeStatus(customer.id(), "SUSPENDED");
        BankProblem failure =
                assertThrows(BankProblem.class, () -> bank.createAccount(customer.id()));
        assertEquals(409, failure.status());
        assertEquals(1, mapper.accounts.size());
        assertEquals(BigDecimal.ZERO, first.balance());
        bank.changeStatus(customer.id(), "ACTIVE");
        assertNotEquals(first.id(), bank.createAccount(customer.id()).id());
        assertEquals("학습고객01", customer.name());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void rejectedNameNeverReachesMapper(String name) {
        StubMapper mapper = new StubMapper();
        assertEquals(
                400,
                assertThrows(BankProblem.class, () -> new BankService(mapper).createCustomer(name))
                        .status());
        assertTrue(mapper.customers.isEmpty());
    }

    @Test
    void invalidStatusAndUnknownCustomerDoNotModifyData() {
        StubMapper mapper = new StubMapper();
        BankService bank = new BankService(mapper);
        var customer = bank.createCustomer("학습고객01");
        assertEquals(
                400,
                assertThrows(BankProblem.class, () -> bank.changeStatus(customer.id(), "DELETED"))
                        .status());
        assertEquals("ACTIVE", mapper.customer(customer.id()).status());
        assertEquals(
                404,
                assertThrows(
                                BankProblem.class,
                                () -> bank.createAccount(UUID.randomUUID().toString()))
                        .status());
        assertTrue(mapper.accounts.isEmpty());
    }

    @Test
    void inclusiveBusinessDatesBecomeHalfOpenTimestampRange() {
        StubMapper mapper = new StubMapper();
        BankService bank = new BankService(mapper);
        String account = bank.createAccount(bank.createCustomer("학습고객01").id()).id();
        bank.history(account, "2026-10-01", "2026-10-25");
        assertEquals(OffsetDateTime.parse("2026-10-01T00:00:00+09:00"), mapper.from);
        assertEquals(OffsetDateTime.parse("2026-10-26T00:00:00+09:00"), mapper.to);
        bank.history(account, null, "");
        assertNull(mapper.from);
        assertNull(mapper.to);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "2026/10/25", "2026-10-01' OR 1=1 --"})
    void invalidDatesNeverReachHistorySql(String value) {
        StubMapper mapper = new StubMapper();
        BankService bank = new BankService(mapper);
        String account = bank.createAccount(bank.createCustomer("학습고객01").id()).id();
        assertEquals(
                400,
                assertThrows(BankProblem.class, () -> bank.history(account, value, null)).status());
        assertEquals(0, mapper.historyCalls);
    }

    @Test
    void reversedDatesAreRejectedBeforeHistoryQuery() {
        StubMapper mapper = new StubMapper();
        BankService bank = new BankService(mapper);
        String account = bank.createAccount(bank.createCustomer("학습고객01").id()).id();
        assertEquals(
                400,
                assertThrows(
                                BankProblem.class,
                                () -> bank.history(account, "2026-10-26", "2026-10-25"))
                        .status());
        assertEquals(0, mapper.historyCalls);
    }

    @Test
    void invalidUuidIsRejectedAndSnapshotsCannotMutate() {
        BankService bank = new BankService(new StubMapper());
        assertEquals(
                400,
                assertThrows(BankProblem.class, () -> bank.createAccount("1-1-1-1-1")).status());
        bank.createCustomer("학습고객01");
        var before = bank.customers();
        bank.createCustomer("학습고객02");
        assertEquals(1, before.size());
        assertThrows(UnsupportedOperationException.class, before::clear);
    }

    @Test
    void readinessFailureDoesNotEscapeAsInternalError() {
        StubMapper mapper = new StubMapper();
        mapper.fail = true;
        assertFalse(new BankService(mapper).ready());
    }
}
