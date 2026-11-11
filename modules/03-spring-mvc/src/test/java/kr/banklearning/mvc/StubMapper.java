package kr.banklearning.mvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.banklearning.mvc.dto.BankViews.AccountView;
import kr.banklearning.mvc.dto.BankViews.CustomerView;
import kr.banklearning.mvc.dto.BankViews.TransactionView;
import kr.banklearning.mvc.mapper.BankMapper;

/** DB 통합 테스트와 구분되는 서비스/HTTP 경계 검증용 저장소 대역. */
public final class StubMapper implements BankMapper {
    public final Map<String, CustomerView> customers = new LinkedHashMap<>();
    public final Map<String, AccountView> accounts = new LinkedHashMap<>();
    public OffsetDateTime from, to;
    public int historyCalls;
    public boolean ready = true, fail;

    public List<CustomerView> customers() {
        if (fail)
            throw new org.springframework.dao.DataAccessResourceFailureException(
                    "jdbc:secret password");
        return new ArrayList<>(customers.values());
    }

    public CustomerView customer(String id) {
        return customers.get(id);
    }

    public int insertCustomer(CustomerView customer) {
        customers.put(customer.id(), customer);
        return 1;
    }

    public int changeStatus(String id, String status) {
        CustomerView customer = customers.get(id);
        if (customer == null) return 0;
        customers.put(id, new CustomerView(id, customer.name(), status));
        return 1;
    }

    public List<AccountView> accounts(String customerId) {
        return accounts.values().stream()
                .filter(account -> customerId == null || account.customerId().equals(customerId))
                .toList();
    }

    public AccountView account(String id) {
        return accounts.get(id);
    }

    public int insertAccount(AccountView account) {
        accounts.put(account.id(), account);
        return 1;
    }

    public List<TransactionView> history(
            String accountId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive) {
        historyCalls++;
        from = fromInclusive;
        to = toExclusive;
        return List.of();
    }

    public boolean ready() {
        if (fail) throw new org.springframework.dao.DataAccessResourceFailureException("secret");
        return ready;
    }
}
