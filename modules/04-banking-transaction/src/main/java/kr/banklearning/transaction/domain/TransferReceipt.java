package kr.banklearning.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferReceipt(UUID requestId, UUID sourceId, UUID destinationId, BigDecimal amount,
                              String status, LocalDate businessDate) {
    public UUID getRequestId() { return requestId; }
    public UUID getSourceId() { return sourceId; }
    public UUID getDestinationId() { return destinationId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDate getBusinessDate() { return businessDate; }
}
