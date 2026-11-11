package kr.banklearning.servlet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BankViews {
    private BankViews() {}

    // JavaBean getters는 JSP EL에서 사용한다. 업무 연산은 DTO에 넣지 않는다.
    public record CustomerView(UUID id, String name, String status) {
        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
    }

    public record AccountView(UUID id, UUID customerId, BigDecimal balance) {
        public UUID getId() {
            return id;
        }

        public UUID getCustomerId() {
            return customerId;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }

    public record TransactionView(
            UUID id,
            UUID accountId,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            Instant occurredAt) {
        public UUID getId() {
            return id;
        }

        public UUID getAccountId() {
            return accountId;
        }

        public String getType() {
            return type;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getBalanceAfter() {
            return balanceAfter;
        }

        public Instant getOccurredAt() {
            return occurredAt;
        }
    }
}
