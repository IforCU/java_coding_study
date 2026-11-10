package kr.banklearning.transaction.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Account(UUID id, UUID customerId, BigDecimal balance, BigDecimal dailyLimit, long version) {
    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public long getVersion() { return version; }
}
