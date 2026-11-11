package kr.banklearning.api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Phase 4 데이터 소유권을 침범하지 않는 SELECT 전용 어댑터. */
@Repository
public class JdbcBankReadRepository implements BankReadRepository {
    private final JdbcTemplate jdbc;

    public JdbcBankReadRepository(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(3);
    }

    @Override
    public Optional<CustomerView> customer(UUID id) {
        return jdbc
                .query(
                        "SELECT id, name, status FROM phase4.customer WHERE id = ?",
                        (result, row) ->
                                new CustomerView(
                                        result.getObject("id", UUID.class),
                                        result.getString("name"),
                                        result.getString("status")),
                        id)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AccountView> account(UUID id) {
        return jdbc
                .query(
                        "SELECT id, customer_id, balance, daily_limit, version FROM phase4.account WHERE id = ?",
                        (result, row) ->
                                new AccountView(
                                        result.getObject("id", UUID.class),
                                        result.getObject("customer_id", UUID.class),
                                        result.getBigDecimal("balance"),
                                        result.getBigDecimal("daily_limit"),
                                        result.getLong("version")),
                        id)
                .stream()
                .findFirst();
    }

    @Override
    public List<TransactionView> transactions(
            UUID accountId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive, int limit) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT id, request_id, account_id, type, amount, balance_after, occurred_at FROM phase4.ledger WHERE account_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(accountId);
        if (fromInclusive != null) {
            sql.append(" AND occurred_at >= ?");
            parameters.add(fromInclusive);
        }
        if (toExclusive != null) {
            sql.append(" AND occurred_at < ?");
            parameters.add(toExclusive);
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ?");
        parameters.add(limit);
        return List.copyOf(
                jdbc.query(
                        sql.toString(),
                        (result, row) ->
                                new TransactionView(
                                        result.getObject("id", UUID.class),
                                        result.getObject("request_id", UUID.class),
                                        result.getObject("account_id", UUID.class),
                                        result.getString("type"),
                                        result.getBigDecimal("amount"),
                                        result.getBigDecimal("balance_after"),
                                        result.getObject("occurred_at", OffsetDateTime.class)),
                        parameters.toArray()));
    }

    @Override
    public BigDecimal dailyUsed(UUID accountId, LocalDate date) {
        return jdbc
                .query(
                        "SELECT used FROM phase4.daily_usage WHERE account_id = ? AND business_date = ?",
                        (result, row) -> result.getBigDecimal("used"),
                        accountId,
                        date)
                .stream()
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public StatisticsView statistics(LocalDate from, LocalDate to) {
        return jdbc.queryForObject(
                "SELECT count(*) AS transfer_count, COALESCE(sum(amount), 0) AS total_amount FROM phase4.transfer_request "
                        + "WHERE status IN ('COMPLETED', 'CANCEL_REQUESTED') AND business_date >= ? AND business_date <= ?",
                (result, row) ->
                        new StatisticsView(
                                from,
                                to,
                                result.getLong("transfer_count"),
                                result.getBigDecimal("total_amount")),
                from,
                to);
    }

    @Override
    public boolean ready() {
        try {
            return Boolean.TRUE.equals(
                    jdbc.queryForObject(
                            "SELECT (SELECT count(*) FROM phase4.customer) >= 0 AND (SELECT count(*) FROM phase4.account) >= 0 "
                                    + "AND (SELECT count(*) FROM phase4.ledger) >= 0 AND (SELECT count(*) FROM phase4.daily_usage) >= 0 AND (SELECT count(*) FROM phase4.transfer_request) >= 0",
                            Boolean.class));
        } catch (org.springframework.dao.DataAccessException exception) {
            return false;
        }
    }
}
