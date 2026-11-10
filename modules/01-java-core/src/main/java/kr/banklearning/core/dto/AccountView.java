package kr.banklearning.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountView(UUID id, UUID customerId, BigDecimal balance) {}
