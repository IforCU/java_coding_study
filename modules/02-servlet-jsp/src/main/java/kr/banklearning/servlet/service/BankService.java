package kr.banklearning.servlet.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import kr.banklearning.servlet.dao.BankDao;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;

public final class BankService {
    private final BankDao dao;

    public BankService(BankDao dao) {
        this.dao = dao;
    }

    public List<CustomerView> customers() {
        return List.copyOf(dao.customers());
    }

    public CustomerView createCustomer(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 80) {
            throw new InputException("고객 이름은 1~80자여야 합니다.");
        }
        CustomerView customer = new CustomerView(UUID.randomUUID(), name.strip(), "ACTIVE");
        dao.insertCustomer(customer);
        return customer;
    }

    public CustomerView customer(String id) {
        return dao.customer(id(id)).orElseThrow(MissingException::new);
    }

    public List<AccountView> accounts(String customerId) {
        if (customerId == null || customerId.isBlank()) return List.copyOf(dao.accounts(null));
        CustomerView customer = customer(customerId);
        return List.copyOf(dao.accounts(customer.id()));
    }

    public AccountView createAccount(String customerId) {
        CustomerView customer = customer(customerId);
        AccountView account = new AccountView(UUID.randomUUID(), customer.id(), BigDecimal.ZERO);
        dao.insertAccount(account);
        return account;
    }

    public AccountView account(String accountId) {
        return dao.account(id(accountId)).orElseThrow(MissingException::new);
    }

    public List<TransactionView> history(String accountId) {
        AccountView account = account(accountId);
        return List.copyOf(dao.history(account.id()));
    }

    public boolean ready() {
        return dao.ready();
    }

    public static UUID id(String value) {
        if (value == null
                || !value.matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new InputException("올바른 UUID 식별자를 입력하세요.");
        }
        return UUID.fromString(value);
    }
}
