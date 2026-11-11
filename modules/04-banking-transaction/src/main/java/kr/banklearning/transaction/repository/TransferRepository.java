package kr.banklearning.transaction.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;

public interface TransferRepository {
    boolean reserve(TransferCommand command, LocalDate businessDate);

    Optional<TransferReceipt> findRequest(UUID requestId, boolean forUpdate);

    List<Account> lockAccounts(UUID sourceId, UUID destinationId);

    List<Account> accounts();

    BigDecimal dailyUsed(UUID accountId, LocalDate date);

    void setBalance(UUID accountId, BigDecimal balance);

    void setDailyUsed(UUID accountId, LocalDate date, BigDecimal amount);

    void appendLedger(LedgerEntry entry);

    List<LedgerEntry> ledger(UUID requestId);

    void complete(UUID requestId);

    void markCancellationRequested(UUID requestId);

    boolean ready();
}
