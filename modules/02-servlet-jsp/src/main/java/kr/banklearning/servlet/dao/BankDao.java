package kr.banklearning.servlet.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;

public interface BankDao {
    List<CustomerView> customers();
    Optional<CustomerView> customer(UUID id);
    void insertCustomer(CustomerView customer);
    List<AccountView> accounts(UUID customerId);
    Optional<AccountView> account(UUID id);
    void insertAccount(AccountView account);
    List<TransactionView> history(UUID accountId);
    boolean ready();
}
