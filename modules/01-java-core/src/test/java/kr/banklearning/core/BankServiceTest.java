package kr.banklearning.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kr.banklearning.core.domain.TransactionType;
import kr.banklearning.core.dto.AccountView;
import kr.banklearning.core.dto.CustomerView;
import kr.banklearning.core.dto.TransactionView;
import kr.banklearning.core.exception.InsufficientFundsException;
import kr.banklearning.core.exception.InvalidAmountException;
import kr.banklearning.core.exception.InvalidCustomerException;
import kr.banklearning.core.exception.NotFoundException;
import kr.banklearning.core.repository.InMemoryBankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BankServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");
    private BankService bank;
    private CustomerView customer;
    private AccountView account;

    @BeforeEach
    void setUp() {
        bank = new BankService(new InMemoryBankRepository(), Clock.fixed(NOW, ZoneOffset.UTC));
        customer = bank.createCustomer("  학습고객01  ");
        account = bank.openAccount(customer.id());
    }

    @Test
    void customerOwnsMultipleDistinctAccountsWithoutSharingBalances() {
        AccountView second = bank.openAccount(customer.id());
        CustomerView other = bank.createCustomer("학습고객02");
        bank.openAccount(other.id());
        bank.deposit(account.id(), new BigDecimal("100"));

        assertEquals("학습고객01", customer.name());
        assertNotEquals(account.id(), second.id());
        assertEquals(2, bank.accountsOf(customer.id()).size());
        assertTrue(bank.accountsOf(customer.id()).stream().allMatch(item -> item.customerId().equals(customer.id())));
        assertEquals(BigDecimal.ZERO, bank.getAccount(second.id()).balance());
        assertTrue(bank.history(second.id()).isEmpty());
    }

    @Test
    void depositAndWithdrawKeepExactWonAndOrderedHistory() {
        bank.deposit(account.id(), new BigDecimal("100000.00"));
        bank.withdraw(account.id(), new BigDecimal("35000"));

        assertEquals(new BigDecimal("65000"), bank.getAccount(account.id()).balance());
        List<TransactionView> history = bank.history(account.id());
        assertEquals(List.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL),
                history.stream().map(TransactionView::type).toList());
        assertEquals(List.of(new BigDecimal("100000"), new BigDecimal("65000")),
                history.stream().map(TransactionView::balanceAfter).toList());
        assertEquals(new BigDecimal("35000"), history.get(1).amount());
        assertEquals(NOW, history.get(0).occurredAt());
        assertEquals(account.id(), history.get(0).accountId());
        assertNotEquals(history.get(0).id(), history.get(1).id());
    }

    @Test
    void largeBalancesDoNotOverflowOrLosePrecision() {
        BigDecimal huge = new BigDecimal("9999999999999999999999999999999999999999");
        bank.deposit(account.id(), huge);
        bank.deposit(account.id(), BigDecimal.ONE);
        bank.withdraw(account.id(), new BigDecimal("3"));
        assertEquals(huge.subtract(new BigDecimal("2")), bank.getAccount(account.id()).balance());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01", "1.01", "0.1"})
    void rejectedAmountsPreserveBalanceAndHistory(String input) {
        bank.deposit(account.id(), new BigDecimal("100"));
        BigDecimal amount = input == null ? null : new BigDecimal(input);
        List<TransactionView> before = bank.history(account.id());

        assertThrows(InvalidAmountException.class, () -> bank.deposit(account.id(), amount));
        assertThrows(InvalidAmountException.class, () -> bank.withdraw(account.id(), amount));
        assertEquals(new BigDecimal("100"), bank.getAccount(account.id()).balance());
        assertEquals(before, bank.history(account.id()));
    }

    @Test
    void insufficientFundsDoNotCreateTransactionAndExactBalanceCanBeWithdrawn() {
        bank.deposit(account.id(), new BigDecimal("100"));
        List<TransactionView> before = bank.history(account.id());
        assertThrows(InsufficientFundsException.class,
                () -> bank.withdraw(account.id(), new BigDecimal("101")));
        assertEquals(new BigDecimal("100"), bank.getAccount(account.id()).balance());
        assertEquals(before, bank.history(account.id()));

        bank.withdraw(account.id(), new BigDecimal("100"));
        assertEquals(BigDecimal.ZERO, bank.getAccount(account.id()).balance());
        assertEquals(2, bank.history(account.id()).size());
    }

    @Test
    void snapshotsCannotBeChangedOrReflectFutureMutations() {
        List<AccountView> accounts = bank.accountsOf(customer.id());
        List<TransactionView> history = bank.history(account.id());
        assertThrows(UnsupportedOperationException.class, () -> accounts.clear());
        assertThrows(UnsupportedOperationException.class, () -> history.clear());
        bank.deposit(account.id(), BigDecimal.TEN);
        bank.openAccount(customer.id());
        assertEquals(BigDecimal.ZERO, account.balance());
        assertEquals(BigDecimal.ZERO, accounts.get(0).balance());
        assertEquals(1, accounts.size());
        assertTrue(history.isEmpty());
    }

    @Test
    void missingCustomerAndAccountReturnTypedFailures() {
        UUID missing = UUID.randomUUID();
        assertThrows(NotFoundException.class, () -> bank.openAccount(missing));
        assertThrows(NotFoundException.class, () -> bank.accountsOf(missing));
        assertThrows(NotFoundException.class, () -> bank.getAccount(missing));
        assertThrows(NotFoundException.class, () -> bank.history(missing));
        assertThrows(NotFoundException.class, () -> bank.deposit(missing, BigDecimal.ONE));
        assertThrows(NotFoundException.class, () -> bank.withdraw(missing, BigDecimal.ONE));
        assertThrows(NotFoundException.class, () -> bank.openAccount(null));
        assertThrows(NotFoundException.class, () -> bank.getAccount(null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n"})
    void invalidCustomerNameIsRejected(String name) {
        assertThrows(InvalidCustomerException.class, () -> bank.createCustomer(name));
    }

    @Test
    void excessivelyLongNameIsRejected() {
        assertThrows(InvalidCustomerException.class, () -> bank.createCustomer("가".repeat(101)));
    }

    @Test
    void repositoryInstanceSeparatesIndependentBankRuns() {
        BankService independent = new BankService();
        assertThrows(NotFoundException.class, () -> independent.getAccount(account.id()));
    }

    @Test
    void simultaneousWithdrawalsHaveOneWinnerAndOneRecordedWithdrawal() throws Exception {
        bank.deposit(account.id(), new BigDecimal("100"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> withdraw = () -> {
            try {
                bank.withdraw(account.id(), new BigDecimal("80"));
                return true;
            } catch (InsufficientFundsException exception) {
                return false;
            }
        };
        try {
            List<Future<Boolean>> results = executor.invokeAll(List.of(withdraw, withdraw));
            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successes++;
            }
            assertEquals(1, successes);
            assertEquals(new BigDecimal("20"), bank.getAccount(account.id()).balance());
            assertEquals(2, bank.history(account.id()).size());
        } finally {
            executor.shutdownNow();
        }
    }
}
