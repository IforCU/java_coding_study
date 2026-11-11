package kr.banklearning.core.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kr.banklearning.core.exception.InsufficientFundsException;

/** 외부 setter 없이 업무 연산을 통해서만 다음 상태를 만든다. */
public final class Account {
    private final UUID id;
    private final UUID customerId;
    private final BigDecimal balance;
    private final List<Transaction> transactions;

    private Account(UUID id, UUID customerId, BigDecimal balance, List<Transaction> transactions) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.balance = balance;
        this.transactions = List.copyOf(transactions);
    }

    public static Account open(UUID id, UUID customerId) {
        return new Account(id, customerId, BigDecimal.ZERO, List.of());
    }

    public Account deposit(BigDecimal amount, UUID transactionId, Instant occurredAt) {
        BigDecimal normalized = Won.positive(amount);
        return record(
                TransactionType.DEPOSIT,
                normalized,
                balance.add(normalized),
                transactionId,
                occurredAt);
    }

    public Account withdraw(BigDecimal amount, UUID transactionId, Instant occurredAt) {
        BigDecimal normalized = Won.positive(amount);
        if (balance.compareTo(normalized) < 0) {
            throw new InsufficientFundsException();
        }
        return record(
                TransactionType.WITHDRAWAL,
                normalized,
                balance.subtract(normalized),
                transactionId,
                occurredAt);
    }

    private Account record(
            TransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            UUID transactionId,
            Instant occurredAt) {
        List<Transaction> next = new ArrayList<>(transactions);
        next.add(new Transaction(transactionId, id, type, amount, balanceAfter, occurredAt));
        return new Account(id, customerId, balanceAfter, next);
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public BigDecimal balance() {
        return balance;
    }

    public List<Transaction> transactions() {
        return transactions;
    }
}
