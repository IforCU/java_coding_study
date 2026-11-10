package kr.banklearning.transaction.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;
import kr.banklearning.transaction.service.BankProblem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcTransferRepository implements TransferRepository {
    private final JdbcTemplate jdbc;
    public JdbcTransferRepository(DataSource dataSource) { jdbc = new JdbcTemplate(dataSource); jdbc.setQueryTimeout(5); }

    @Override public boolean reserve(TransferCommand command, LocalDate businessDate) {
        try {
            return jdbc.update("INSERT INTO phase4.transfer_request (request_id, source_id, destination_id, amount, status, business_date, created_at) "
                        + "VALUES (?, ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP) ON CONFLICT (request_id) DO NOTHING",
                    command.requestId(), command.sourceId(), command.destinationId(), command.amount(), businessDate) == 1;
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                    throw new BankProblem(404, "ACCOUNT_NOT_FOUND", "이체 계좌를 찾을 수 없습니다.");
                }
            }
            throw exception;
        }
    }

    @Override public Optional<TransferReceipt> findRequest(UUID requestId, boolean forUpdate) {
        String sql = "SELECT request_id, source_id, destination_id, amount, status, business_date FROM phase4.transfer_request WHERE request_id = ?";
        return jdbc.query(sql + (forUpdate ? " FOR UPDATE" : ""), JdbcTransferRepository::receipt, requestId).stream().findFirst();
    }

    @Override public List<Account> lockAccounts(UUID sourceId, UUID destinationId) {
        // FK 검증의 KEY SHARE와 호환되면서 잔액 갱신을 직렬화한다. UUID 키는 갱신하지 않는다.
        return jdbc.query("SELECT id, customer_id, balance, daily_limit, version FROM phase4.account "
                + "WHERE id IN (?, ?) ORDER BY id FOR NO KEY UPDATE", JdbcTransferRepository::account, sourceId, destinationId);
    }

    @Override public List<Account> accounts() {
        return List.copyOf(jdbc.query("SELECT id, customer_id, balance, daily_limit, version FROM phase4.account ORDER BY id", JdbcTransferRepository::account));
    }

    @Override public BigDecimal dailyUsed(UUID accountId, LocalDate date) {
        return jdbc.query("SELECT used FROM phase4.daily_usage WHERE account_id = ? AND business_date = ?",
                (result, row) -> result.getBigDecimal("used"), accountId, date).stream().findFirst().orElse(BigDecimal.ZERO);
    }

    @Override public void setBalance(UUID accountId, BigDecimal balance) {
        exactlyOne(jdbc.update("UPDATE phase4.account SET balance = ?, version = version + 1 WHERE id = ?", balance, accountId));
    }

    @Override public void setDailyUsed(UUID accountId, LocalDate date, BigDecimal amount) {
        exactlyOne(jdbc.update("INSERT INTO phase4.daily_usage (account_id, business_date, used) VALUES (?, ?, ?) "
                + "ON CONFLICT (account_id, business_date) DO UPDATE SET used = EXCLUDED.used", accountId, date, amount));
    }

    @Override public void appendLedger(LedgerEntry entry) {
        exactlyOne(jdbc.update("INSERT INTO phase4.ledger (id, request_id, account_id, type, amount, balance_after, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                entry.id(), entry.requestId(), entry.accountId(), entry.type(), entry.amount(), entry.balanceAfter(), Timestamp.from(entry.occurredAt())));
    }

    @Override public List<LedgerEntry> ledger(UUID requestId) {
        return List.copyOf(jdbc.query("SELECT id, request_id, account_id, type, amount, balance_after, occurred_at FROM phase4.ledger WHERE request_id = ? ORDER BY occurred_at, id",
                (result, row) -> new LedgerEntry(result.getObject("id", UUID.class), result.getObject("request_id", UUID.class),
                        result.getObject("account_id", UUID.class), result.getString("type"), result.getBigDecimal("amount"),
                        result.getBigDecimal("balance_after"), result.getTimestamp("occurred_at").toInstant()), requestId));
    }

    @Override public void complete(UUID requestId) {
        exactlyOne(jdbc.update("UPDATE phase4.transfer_request SET status = 'COMPLETED' WHERE request_id = ? AND status = 'PENDING'", requestId));
    }

    @Override public void markCancellationRequested(UUID requestId) {
        exactlyOne(jdbc.update("UPDATE phase4.transfer_request SET status = 'CANCEL_REQUESTED' WHERE request_id = ? AND status = 'COMPLETED'", requestId));
    }

    @Override public boolean ready() {
        try {
            return Boolean.TRUE.equals(jdbc.queryForObject("SELECT (SELECT count(*) FROM phase4.customer) >= 0 AND (SELECT count(*) FROM phase4.account) >= 0 "
                    + "AND (SELECT count(*) FROM phase4.transfer_request) >= 0 AND (SELECT count(*) FROM phase4.ledger) >= 0 AND (SELECT count(*) FROM phase4.daily_usage) >= 0", Boolean.class));
        } catch (org.springframework.dao.DataAccessException exception) { return false; }
    }

    private static Account account(ResultSet result, int row) throws SQLException {
        return new Account(result.getObject("id", UUID.class), result.getObject("customer_id", UUID.class),
                result.getBigDecimal("balance"), result.getBigDecimal("daily_limit"), result.getLong("version"));
    }
    private static TransferReceipt receipt(ResultSet result, int row) throws SQLException {
        return new TransferReceipt(result.getObject("request_id", UUID.class), result.getObject("source_id", UUID.class),
                result.getObject("destination_id", UUID.class), result.getBigDecimal("amount"), result.getString("status"),
                result.getObject("business_date", LocalDate.class));
    }
    private static void exactlyOne(int count) { if (count != 1) throw new IllegalStateException("거래 상태를 변경하지 못했습니다."); }
}
