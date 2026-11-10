package kr.banklearning.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(UUID id, UUID requestId, UUID accountId, String type, BigDecimal amount,
                          BigDecimal balanceAfter, Instant occurredAt) {
    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getOccurredAt() { return occurredAt; }
}
