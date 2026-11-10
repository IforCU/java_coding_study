package kr.banklearning.servlet.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.servlet.dto.BankViews.AccountView;
import kr.banklearning.servlet.dto.BankViews.CustomerView;
import kr.banklearning.servlet.dto.BankViews.TransactionView;

public final class JdbcBankDao implements BankDao {
    private final DataSource dataSource;

    public JdbcBankDao(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public List<CustomerView> customers() {
        return query("SELECT id, name, status FROM phase2.customer ORDER BY name, id", statement -> {},
                JdbcBankDao::mapCustomer);
    }

    @Override
    public Optional<CustomerView> customer(UUID id) {
        return query("SELECT id, name, status FROM phase2.customer WHERE id = ?",
                statement -> statement.setObject(1, id), JdbcBankDao::mapCustomer).stream().findFirst();
    }

    @Override
    public void insertCustomer(CustomerView customer) {
        update("INSERT INTO phase2.customer (id, name, status) VALUES (?, ?, ?)", statement -> {
            statement.setObject(1, customer.id());
            statement.setString(2, customer.name());
            statement.setString(3, customer.status());
        });
    }

    @Override
    public List<AccountView> accounts(UUID customerId) {
        if (customerId == null) {
            return query("SELECT id, customer_id, balance FROM phase2.account ORDER BY id",
                    statement -> {}, JdbcBankDao::mapAccount);
        }
        return query("SELECT id, customer_id, balance FROM phase2.account WHERE customer_id = ? ORDER BY id",
                statement -> statement.setObject(1, customerId), JdbcBankDao::mapAccount);
    }

    @Override
    public Optional<AccountView> account(UUID id) {
        return query("SELECT id, customer_id, balance FROM phase2.account WHERE id = ?",
                statement -> statement.setObject(1, id), JdbcBankDao::mapAccount).stream().findFirst();
    }

    @Override
    public void insertAccount(AccountView account) {
        update("INSERT INTO phase2.account (id, customer_id, balance) VALUES (?, ?, ?)", statement -> {
            statement.setObject(1, account.id());
            statement.setObject(2, account.customerId());
            statement.setBigDecimal(3, account.balance());
        });
    }

    @Override
    public List<TransactionView> history(UUID accountId) {
        return query("SELECT id, account_id, type, amount, balance_after, occurred_at "
                        + "FROM phase2.transaction_history WHERE account_id = ? ORDER BY occurred_at, id",
                statement -> statement.setObject(1, accountId), result -> new TransactionView(
                        result.getObject("id", UUID.class), result.getObject("account_id", UUID.class),
                        result.getString("type"), result.getBigDecimal("amount"), result.getBigDecimal("balance_after"),
                        result.getObject("occurred_at", OffsetDateTime.class).toInstant()));
    }

    @Override
    public boolean ready() {
        try {
            return query("SELECT (SELECT count(*) FROM phase2.customer) AS customers, "
                            + "(SELECT count(*) FROM phase2.account) AS accounts, "
                            + "(SELECT count(*) FROM phase2.transaction_history) AS transactions",
                    statement -> {}, result -> result.getLong("customers") >= 0).stream().findFirst().orElse(false);
        } catch (DataAccessException exception) {
            return false;
        }
    }

    private <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(3);
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<T> values = new ArrayList<>();
                while (result.next()) values.add(mapper.map(result));
                return List.copyOf(values);
            }
        } catch (SQLException exception) {
            throw new DataAccessException();
        }
    }

    private void update(String sql, Binder binder) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(3);
            binder.bind(statement);
            if (statement.executeUpdate() != 1) throw new DataAccessException();
        } catch (SQLException exception) {
            throw new DataAccessException();
        }
    }

    private static CustomerView mapCustomer(ResultSet result) throws SQLException {
        return new CustomerView(result.getObject("id", UUID.class), result.getString("name"), result.getString("status"));
    }

    private static AccountView mapAccount(ResultSet result) throws SQLException {
        return new AccountView(result.getObject("id", UUID.class), result.getObject("customer_id", UUID.class),
                result.getBigDecimal("balance"));
    }

    @FunctionalInterface private interface Binder { void bind(PreparedStatement statement) throws SQLException; }
    @FunctionalInterface private interface RowMapper<T> { T map(ResultSet result) throws SQLException; }
}
