package kr.banklearning.core.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.banklearning.core.domain.TransactionType;

public record TransactionView(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant occurredAt) {}
