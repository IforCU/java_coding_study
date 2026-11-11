package kr.banklearning.core.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Transaction(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant occurredAt) {
    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(balanceAfter, "balanceAfter");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
