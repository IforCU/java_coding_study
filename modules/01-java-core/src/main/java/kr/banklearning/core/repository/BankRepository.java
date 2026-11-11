package kr.banklearning.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import kr.banklearning.core.domain.Account;
import kr.banklearning.core.domain.Customer;

public interface BankRepository {
    void addCustomer(Customer customer);

    Optional<Customer> findCustomer(UUID id);

    void addAccount(Account account);

    Optional<Account> findAccount(UUID id);

    List<Account> findAccountsByCustomer(UUID customerId);

    /** 한 계좌의 잔액과 이력을 함께 교체한다. 연산 실패 시 기존 상태를 보존한다. */
    Account updateAccount(UUID id, UnaryOperator<Account> update);
}
