package kr.banklearning.mvc.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import kr.banklearning.mvc.dto.BankViews.AccountView;
import kr.banklearning.mvc.dto.BankViews.CustomerView;
import kr.banklearning.mvc.dto.BankViews.TransactionView;
import kr.banklearning.mvc.mapper.BankMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private final BankMapper mapper;

    public BankService(BankMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CustomerView> customers() {
        return List.copyOf(mapper.customers());
    }

    @Transactional
    public CustomerView createCustomer(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 80) {
            throw new BankProblem(400, "고객 이름은 1~80자여야 합니다.");
        }
        CustomerView customer =
                new CustomerView(UUID.randomUUID().toString(), name.strip(), "ACTIVE");
        if (mapper.insertCustomer(customer) != 1) throw new IllegalStateException("고객 등록에 실패했습니다.");
        return customer;
    }

    @Transactional
    public void changeStatus(String customerId, String status) {
        CustomerView customer = requireCustomer(customerId);
        if (!"ACTIVE".equals(status) && !"SUSPENDED".equals(status)) {
            throw new BankProblem(400, "고객 상태는 ACTIVE 또는 SUSPENDED여야 합니다.");
        }
        if (mapper.changeStatus(customer.id(), status) != 1)
            throw new BankProblem(404, "고객을 찾을 수 없습니다.");
    }

    @Transactional(readOnly = true)
    public List<AccountView> accounts(String customerId) {
        if (customerId != null && !customerId.isBlank()) requireCustomer(customerId);
        return List.copyOf(
                mapper.accounts(
                        customerId == null || customerId.isBlank() ? null : id(customerId)));
    }

    @Transactional
    public AccountView createAccount(String customerId) {
        CustomerView customer = requireCustomer(customerId);
        if (!"ACTIVE".equals(customer.status()))
            throw new BankProblem(409, "정지된 고객은 계좌를 개설할 수 없습니다.");
        AccountView account =
                new AccountView(UUID.randomUUID().toString(), customer.id(), BigDecimal.ZERO);
        if (mapper.insertAccount(account) != 1) throw new IllegalStateException("계좌 등록에 실패했습니다.");
        return account;
    }

    @Transactional(readOnly = true)
    public AccountView account(String accountId) {
        AccountView account = mapper.account(id(accountId));
        if (account == null) throw new BankProblem(404, "계좌를 찾을 수 없습니다.");
        return account;
    }

    @Transactional(readOnly = true)
    public List<TransactionView> history(String accountId, String from, String to) {
        account(accountId);
        LocalDate first = date(from);
        LocalDate last = date(to);
        if (first != null && last != null && first.isAfter(last)) {
            throw new BankProblem(400, "조회 시작일은 종료일보다 늦을 수 없습니다.");
        }
        try {
            OffsetDateTime fromInclusive =
                    first == null ? null : first.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
            OffsetDateTime toExclusive =
                    last == null
                            ? null
                            : last.plusDays(1).atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
            return List.copyOf(mapper.history(id(accountId), fromInclusive, toExclusive));
        } catch (DateTimeException exception) {
            throw new BankProblem(400, "조회 날짜 범위를 확인하세요.");
        }
    }

    public boolean ready() {
        try {
            return mapper.ready();
        } catch (org.springframework.dao.DataAccessException
                | org.apache.ibatis.exceptions.PersistenceException exception) {
            return false;
        }
    }

    private CustomerView requireCustomer(String customerId) {
        CustomerView customer = mapper.customer(id(customerId));
        if (customer == null) throw new BankProblem(404, "고객을 찾을 수 없습니다.");
        return customer;
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException exception) {
            throw new BankProblem(400, "날짜는 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    static String id(String value) {
        if (value == null
                || !value.matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new BankProblem(400, "올바른 UUID 식별자를 입력하세요.");
        }
        return UUID.fromString(value).toString();
    }
}
