package kr.banklearning.transaction.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(
        UUID requestId, UUID sourceId, UUID destinationId, BigDecimal amount) {}
