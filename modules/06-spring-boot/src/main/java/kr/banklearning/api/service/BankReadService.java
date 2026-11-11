package kr.banklearning.api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.LimitView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;
import kr.banklearning.api.repository.BankReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BankReadService {
    public static final UUID LEARNER_CUSTOMER =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final BankReadRepository repository;
    private final Clock clock;

    public BankReadService(BankReadRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CustomerView customer(UUID id, String username, boolean admin) {
        owner(id, username, admin);
        return repository.customer(id).orElseThrow(() -> new BankProblem(404, "NOT_FOUND"));
    }

    public AccountView account(UUID id, String username, boolean admin) {
        AccountView account =
                repository.account(id).orElseThrow(() -> new BankProblem(404, "NOT_FOUND"));
        owner(account.customerId(), username, admin);
        return account;
    }

    public List<TransactionView> transactions(
            UUID id, String from, String to, int limit, String username, boolean admin) {
        if (limit < 1 || limit > 200) throw new BankProblem(400, "INVALID_REQUEST");
        account(id, username, admin);
        LocalDate first = date(from), last = date(to);
        if (first != null && last != null && first.isAfter(last))
            throw new BankProblem(400, "INVALID_REQUEST");
        try {
            return List.copyOf(
                    repository.transactions(
                            id,
                            first == null ? null : first.atStartOfDay(ZONE).toOffsetDateTime(),
                            last == null
                                    ? null
                                    : last.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime(),
                            limit));
        } catch (DateTimeException exception) {
            throw new BankProblem(400, "INVALID_REQUEST");
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LimitView limit(UUID id, String username, boolean admin) {
        AccountView account = account(id, username, admin);
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        BigDecimal used = repository.dailyUsed(id, today);
        return new LimitView(
                id,
                today,
                account.dailyLimit(),
                used,
                account.dailyLimit().subtract(used).max(BigDecimal.ZERO));
    }

    public StatisticsView statistics(String from, String to, boolean admin) {
        if (!admin) throw new BankProblem(403, "FORBIDDEN");
        LocalDate first = date(from), last = date(to);
        if (first == null && last == null) first = last = LocalDate.now(clock.withZone(ZONE));
        else if (first == null) first = last;
        else if (last == null) last = first;
        if (first.isAfter(last) || ChronoUnit.DAYS.between(first, last) >= 366)
            throw new BankProblem(400, "INVALID_REQUEST");
        return repository.statistics(first, last);
    }

    private static void owner(UUID customerId, String username, boolean admin) {
        if (!admin && !("learner".equals(username) && LEARNER_CUSTOMER.equals(customerId)))
            throw new BankProblem(403, "FORBIDDEN");
    }

    private static LocalDate date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate parsed = LocalDate.parse(value);
            if (parsed.getYear() < 1 || parsed.getYear() > 9999)
                throw new DateTimeException("range");
            return parsed;
        } catch (DateTimeException exception) {
            throw new BankProblem(400, "INVALID_REQUEST");
        }
    }
}
