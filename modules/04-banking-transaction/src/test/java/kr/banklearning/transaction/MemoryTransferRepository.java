package kr.banklearning.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;
import kr.banklearning.transaction.repository.TransferRepository;

/** 업무 분기용 대역이며 DB 트랜잭션/락/rollback을 구현하거나 검증하지 않는다. */
public final class MemoryTransferRepository implements TransferRepository {
    public final Map<UUID, Account> accounts = new LinkedHashMap<>();
    public final Map<UUID, TransferReceipt> requests = new LinkedHashMap<>();
    public final Map<String, BigDecimal> usage = new LinkedHashMap<>();
    public final List<LedgerEntry> entries = new ArrayList<>();
    public int reserveCalls;
    public boolean cancellationLocked;
    public boolean ready = true;

    public boolean reserve(TransferCommand command, LocalDate date) {
        reserveCalls++;
        return requests.putIfAbsent(
                        command.requestId(),
                        new TransferReceipt(
                                command.requestId(),
                                command.sourceId(),
                                command.destinationId(),
                                command.amount(),
                                "PENDING",
                                date))
                == null;
    }

    public Optional<TransferReceipt> findRequest(UUID id, boolean forUpdate) {
        if (forUpdate) cancellationLocked = true;
        return Optional.ofNullable(requests.get(id));
    }

    public List<Account> lockAccounts(UUID source, UUID destination) {
        return accounts.values().stream()
                .filter(account -> account.id().equals(source) || account.id().equals(destination))
                .toList();
    }

    public List<Account> accounts() {
        return new ArrayList<>(accounts.values());
    }

    public BigDecimal dailyUsed(UUID id, LocalDate date) {
        return usage.getOrDefault(id + "/" + date, BigDecimal.ZERO);
    }

    public void setBalance(UUID id, BigDecimal balance) {
        Account account = accounts.get(id);
        accounts.put(
                id,
                new Account(
                        id,
                        account.customerId(),
                        balance,
                        account.dailyLimit(),
                        account.version() + 1));
    }

    public void setDailyUsed(UUID id, LocalDate date, BigDecimal amount) {
        usage.put(id + "/" + date, amount);
    }

    public void appendLedger(LedgerEntry entry) {
        entries.add(entry);
    }

    public List<LedgerEntry> ledger(UUID requestId) {
        return entries.stream().filter(entry -> entry.requestId().equals(requestId)).toList();
    }

    public void complete(UUID id) {
        status(id, "COMPLETED");
    }

    public void markCancellationRequested(UUID id) {
        status(id, "CANCEL_REQUESTED");
    }

    public boolean ready() {
        return ready;
    }

    private void status(UUID id, String status) {
        TransferReceipt receipt = requests.get(id);
        requests.put(
                id,
                new TransferReceipt(
                        id,
                        receipt.sourceId(),
                        receipt.destinationId(),
                        receipt.amount(),
                        status,
                        receipt.businessDate()));
    }
}
