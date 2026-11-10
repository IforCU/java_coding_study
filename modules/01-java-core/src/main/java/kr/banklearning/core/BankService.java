package kr.banklearning.core;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kr.banklearning.core.domain.Account;
import kr.banklearning.core.domain.Customer;
import kr.banklearning.core.dto.AccountView;
import kr.banklearning.core.dto.CustomerView;
import kr.banklearning.core.dto.TransactionView;
import kr.banklearning.core.exception.NotFoundException;
import kr.banklearning.core.repository.BankRepository;
import kr.banklearning.core.repository.InMemoryBankRepository;

public final class BankService {
    private final BankRepository repository;
    private final Clock clock;

    public BankService() {
        this(new InMemoryBankRepository(), Clock.systemUTC());
    }

    public BankService(BankRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CustomerView createCustomer(String name) {
        Customer customer = new Customer(UUID.randomUUID(), name);
        repository.addCustomer(customer);
        return new CustomerView(customer.id(), customer.name());
    }

    public AccountView openAccount(UUID customerId) {
        requireCustomer(customerId);
        Account account = Account.open(UUID.randomUUID(), customerId);
        repository.addAccount(account);
        return view(account);
    }

    public AccountView deposit(UUID accountId, BigDecimal amount) {
        return view(repository.updateAccount(accountId,
                account -> account.deposit(amount, UUID.randomUUID(), clock.instant())));
    }

    public AccountView withdraw(UUID accountId, BigDecimal amount) {
        return view(repository.updateAccount(accountId,
                account -> account.withdraw(amount, UUID.randomUUID(), clock.instant())));
    }

    public AccountView getAccount(UUID accountId) {
        return view(requireAccount(accountId));
    }

    public List<AccountView> accountsOf(UUID customerId) {
        requireCustomer(customerId);
        return repository.findAccountsByCustomer(customerId).stream().map(BankService::view).toList();
    }

    public List<TransactionView> history(UUID accountId) {
        return requireAccount(accountId).transactions().stream()
                .map(transaction -> new TransactionView(transaction.id(), transaction.accountId(), transaction.type(),
                        transaction.amount(), transaction.balanceAfter(), transaction.occurredAt()))
                .toList();
    }

    private void requireCustomer(UUID id) {
        repository.findCustomer(id).orElseThrow(() -> new NotFoundException("고객"));
    }

    private Account requireAccount(UUID id) {
        return repository.findAccount(id).orElseThrow(() -> new NotFoundException("계좌"));
    }

    private static AccountView view(Account account) {
        return new AccountView(account.id(), account.customerId(), account.balance());
    }
}
