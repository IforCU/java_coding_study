package kr.banklearning.api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.api.dto.BankViews.AccountView;
import kr.banklearning.api.dto.BankViews.CustomerView;
import kr.banklearning.api.dto.BankViews.StatisticsView;
import kr.banklearning.api.dto.BankViews.TransactionView;

public interface BankReadRepository {
    Optional<CustomerView> customer(UUID id);

    Optional<AccountView> account(UUID id);

    List<TransactionView> transactions(
            UUID accountId, OffsetDateTime fromInclusive, OffsetDateTime toExclusive, int limit);

    BigDecimal dailyUsed(UUID accountId, LocalDate date);

    StatisticsView statistics(LocalDate from, LocalDate to);

    boolean ready();
}
