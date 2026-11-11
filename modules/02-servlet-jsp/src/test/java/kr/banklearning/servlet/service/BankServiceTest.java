package kr.banklearning.servlet.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.servlet.dao.BankDao;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BankServiceTest {
    @Test
    void customerAndMultipleAccountsRemainSeparated() {
        MemoryDao dao = new MemoryDao();
        BankService bank = new BankService(dao);
        CustomerView first = bank.createCustomer("  학습고객01  ");
        CustomerView second = bank.createCustomer("학습고객02");
        AccountView account = bank.createAccount(first.id().toString());
        bank.createAccount(first.id().toString());
        bank.createAccount(second.id().toString());
        assertEquals("학습고객01", first.name());
        assertEquals("ACTIVE", first.status());
        assertEquals(BigDecimal.ZERO, account.balance());
        assertEquals(2, bank.accounts(first.id().toString()).size());
        assertEquals(3, bank.accounts(null).size());
        assertEquals(first.id(), bank.account(account.id().toString()).customerId());
        assertTrue(bank.history(account.id().toString()).isEmpty());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void invalidNamesNeverReachDao(String name) {
        MemoryDao dao = new MemoryDao();
        assertThrows(InputException.class, () -> new BankService(dao).createCustomer(name));
        assertTrue(dao.customerRows.isEmpty());
    }

    @Test
    void maximumNameLengthMatchesDatabase() {
        BankService bank = new BankService(new MemoryDao());
        assertEquals(80, bank.createCustomer("가".repeat(80)).name().length());
        assertThrows(InputException.class, () -> bank.createCustomer("가".repeat(81)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1-1-1-1-1", "not-uuid", "' OR 1=1 --"})
    void uuidRequiresFullCanonicalShape(String value) {
        assertThrows(InputException.class, () -> BankService.id(value));
    }

    @Test
    void missingOwnerPreventsAccountInsertAndMissingAccountPreventsHistoryRead() {
        MemoryDao dao = new MemoryDao();
        BankService bank = new BankService(dao);
        String missing = UUID.randomUUID().toString();
        assertThrows(MissingException.class, () -> bank.createAccount(missing));
        assertTrue(dao.accountRows.isEmpty());
        assertThrows(MissingException.class, () -> bank.history(missing));
        assertEquals(0, dao.historyCalls);
        assertThrows(MissingException.class, () -> bank.accounts(missing));
    }

    @Test
    void resultCollectionsAreImmutableSnapshots() {
        BankService bank = new BankService(new MemoryDao());
        bank.createCustomer("학습고객01");
        List<CustomerView> before = bank.customers();
        bank.createCustomer("학습고객02");
        assertEquals(1, before.size());
        assertThrows(UnsupportedOperationException.class, before::clear);
    }

    private static final class MemoryDao implements BankDao {
        final Map<UUID, CustomerView> customerRows = new LinkedHashMap<>();
        final Map<UUID, AccountView> accountRows = new LinkedHashMap<>();
        int historyCalls;

        public List<CustomerView> customers() {
            return new ArrayList<>(customerRows.values());
        }

        public Optional<CustomerView> customer(UUID id) {
            return Optional.ofNullable(customerRows.get(id));
        }

        public void insertCustomer(CustomerView customer) {
            customerRows.put(customer.id(), customer);
        }

        public List<AccountView> accounts(UUID id) {
            return accountRows.values().stream()
                    .filter(account -> id == null || id.equals(account.customerId()))
                    .toList();
        }

        public Optional<AccountView> account(UUID id) {
            return Optional.ofNullable(accountRows.get(id));
        }

        public void insertAccount(AccountView account) {
            accountRows.put(account.id(), account);
        }

        public List<TransactionView> history(UUID id) {
            historyCalls++;
            return List.of();
        }

        public boolean ready() {
            return true;
        }
    }
}
