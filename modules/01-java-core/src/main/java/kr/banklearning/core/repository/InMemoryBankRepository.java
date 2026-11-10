package kr.banklearning.core.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;
import kr.banklearning.core.domain.Account;
import kr.banklearning.core.domain.Customer;
import kr.banklearning.core.exception.NotFoundException;

public final class InMemoryBankRepository implements BankRepository {
    private final ConcurrentMap<UUID, Customer> customers = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void addCustomer(Customer customer) {
        if (customers.putIfAbsent(customer.id(), customer) != null) {
            throw new IllegalStateException("고객 식별자가 중복되었습니다.");
        }
    }

    @Override
    public Optional<Customer> findCustomer(UUID id) {
        return id == null ? Optional.empty() : Optional.ofNullable(customers.get(id));
    }

    @Override
    public void addAccount(Account account) {
        if (!customers.containsKey(account.customerId())) {
            throw new NotFoundException("고객");
        }
        if (accounts.putIfAbsent(account.id(), account) != null) {
            throw new IllegalStateException("계좌 식별자가 중복되었습니다.");
        }
    }

    @Override
    public Optional<Account> findAccount(UUID id) {
        return id == null ? Optional.empty() : Optional.ofNullable(accounts.get(id));
    }

    @Override
    public List<Account> findAccountsByCustomer(UUID customerId) {
        return accounts.values().stream()
                .filter(account -> account.customerId().equals(customerId))
                .sorted(Comparator.comparing(account -> account.id().toString()))
                .toList();
    }

    @Override
    public Account updateAccount(UUID id, UnaryOperator<Account> update) {
        if (id == null) {
            throw new NotFoundException("계좌");
        }
        return accounts.compute(id, (key, current) -> {
            if (current == null) {
                throw new NotFoundException("계좌");
            }
            Account next = update.apply(current);
            if (next == null || !next.id().equals(current.id())
                    || !next.customerId().equals(current.customerId())) {
                throw new IllegalArgumentException("계좌 식별자와 소유자는 변경할 수 없습니다.");
            }
            return next;
        });
    }
}
