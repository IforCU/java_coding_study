package kr.banklearning.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class BankViews {
    private BankViews() {}
    public record CustomerView(String id, String name, String status) {
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStatus() { return status; }
    }
    public record AccountView(String id, String customerId, BigDecimal balance) {
        public String getId() { return id; }
        public String getCustomerId() { return customerId; }
        public BigDecimal getBalance() { return balance; }
    }
    public record TransactionView(String id, String accountId, String type, BigDecimal amount,
                                  BigDecimal balanceAfter, OffsetDateTime occurredAt) {
        public String getId() { return id; }
        public String getAccountId() { return accountId; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public BigDecimal getBalanceAfter() { return balanceAfter; }
        public OffsetDateTime getOccurredAt() { return occurredAt; }
    }
}
