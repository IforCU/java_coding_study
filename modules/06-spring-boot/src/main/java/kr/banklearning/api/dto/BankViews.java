package kr.banklearning.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class BankViews {
    private BankViews() {}
    public record CustomerView(UUID id, String name, String status) {}
    public record AccountView(UUID id, UUID customerId, BigDecimal balance, BigDecimal dailyLimit, long version) {}
    public record TransactionView(UUID id, UUID requestId, UUID accountId, String type, BigDecimal amount,
                                  BigDecimal balanceAfter, OffsetDateTime occurredAt) {}
    public record LimitView(UUID accountId, LocalDate businessDate, BigDecimal dailyLimit, BigDecimal used, BigDecimal remaining) {}
    public record StatisticsView(LocalDate from, LocalDate to, long transferCount, BigDecimal totalAmount) {}
}
