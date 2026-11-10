package kr.banklearning.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import kr.banklearning.transaction.domain.Account;
import kr.banklearning.transaction.domain.LedgerEntry;
import kr.banklearning.transaction.domain.TransferCommand;
import kr.banklearning.transaction.domain.TransferReceipt;
import kr.banklearning.transaction.repository.TransferRepository;
import org.springframework.transaction.annotation.Transactional;

public class TransferService {
    private static final BigDecimal MAX = new BigDecimal("9999999999999999999");
    private final TransferRepository repository;
    private final Clock clock;
    private final FailureHook failureHook;
    public TransferService(TransferRepository repository, Clock clock, FailureHook failureHook) {
        this.repository = repository; this.clock = clock; this.failureHook = failureHook;
    }

    @Transactional
    public TransferReceipt transfer(TransferCommand supplied) {
        TransferCommand command = validate(supplied);
        LocalDate date = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        if (!repository.reserve(command, date)) {
            TransferReceipt existing = get(command.requestId());
            if (!existing.sourceId().equals(command.sourceId()) || !existing.destinationId().equals(command.destinationId())
                    || existing.amount().compareTo(command.amount()) != 0) {
                throw new BankProblem(409, "REQUEST_CONFLICT", "같은 요청 식별자에 다른 거래를 사용할 수 없습니다.");
            }
            if ("PENDING".equals(existing.status())) throw new BankProblem(409, "REQUEST_PENDING", "처리 중인 요청입니다.");
            return existing;
        }
        List<Account> locked = repository.lockAccounts(command.sourceId(), command.destinationId());
        Account source = locked.stream().filter(account -> account.id().equals(command.sourceId())).findFirst()
                .orElseThrow(() -> new BankProblem(404, "ACCOUNT_NOT_FOUND", "출금 계좌를 찾을 수 없습니다."));
        Account destination = locked.stream().filter(account -> account.id().equals(command.destinationId())).findFirst()
                .orElseThrow(() -> new BankProblem(404, "ACCOUNT_NOT_FOUND", "입금 계좌를 찾을 수 없습니다."));
        if (source.balance().compareTo(command.amount()) < 0) throw new BankProblem(409, "INSUFFICIENT_FUNDS", "잔액이 부족합니다.");
        BigDecimal used = repository.dailyUsed(source.id(), date).add(command.amount());
        if (used.compareTo(source.dailyLimit()) > 0) throw new BankProblem(409, "DAILY_LIMIT", "일일 이체 한도를 초과했습니다.");
        BigDecimal debitBalance = source.balance().subtract(command.amount());
        BigDecimal creditBalance = destination.balance().add(command.amount());
        if (creditBalance.compareTo(MAX) > 0) throw new BankProblem(409, "BALANCE_LIMIT", "입금 계좌의 금액 범위를 초과했습니다.");
        repository.setBalance(source.id(), debitBalance);
        failureHook.afterDebit();
        repository.setBalance(destination.id(), creditBalance);
        repository.appendLedger(new LedgerEntry(UUID.randomUUID(), command.requestId(), source.id(), "WITHDRAWAL", command.amount(), debitBalance, clock.instant()));
        repository.appendLedger(new LedgerEntry(UUID.randomUUID(), command.requestId(), destination.id(), "DEPOSIT", command.amount(), creditBalance, clock.instant()));
        repository.setDailyUsed(source.id(), date, used);
        repository.complete(command.requestId());
        return get(command.requestId());
    }

    @Transactional(readOnly = true)
    public TransferReceipt get(UUID requestId) {
        if (requestId == null) throw new BankProblem(400, "INVALID_ID", "요청 식별자를 입력하세요.");
        return repository.findRequest(requestId, false).orElseThrow(() -> new BankProblem(404, "REQUEST_NOT_FOUND", "거래 요청을 찾을 수 없습니다."));
    }

    @Transactional
    public TransferReceipt requestCancellation(UUID requestId) {
        if (requestId == null) throw new BankProblem(400, "INVALID_ID", "요청 식별자를 입력하세요.");
        TransferReceipt receipt = repository.findRequest(requestId, true).orElseThrow(() -> new BankProblem(404, "REQUEST_NOT_FOUND", "거래 요청을 찾을 수 없습니다."));
        if ("CANCEL_REQUESTED".equals(receipt.status())) return receipt;
        if (!"COMPLETED".equals(receipt.status())) throw new BankProblem(409, "CANCEL_STATE", "완료된 거래만 취소를 요청할 수 있습니다.");
        repository.markCancellationRequested(requestId);
        return get(requestId);
    }

    @Transactional(readOnly = true) public List<Account> accounts() { return List.copyOf(repository.accounts()); }
    @Transactional(readOnly = true) public List<LedgerEntry> ledger(UUID requestId) { get(requestId); return List.copyOf(repository.ledger(requestId)); }
    public boolean ready() { return repository.ready(); }

    private static TransferCommand validate(TransferCommand supplied) {
        if (supplied == null || supplied.requestId() == null || supplied.sourceId() == null || supplied.destinationId() == null) {
            throw new BankProblem(400, "INVALID_ID", "거래와 계좌 식별자를 입력하세요.");
        }
        if (supplied.sourceId().equals(supplied.destinationId())) throw new BankProblem(400, "SAME_ACCOUNT", "서로 다른 계좌를 선택하세요.");
        if (supplied.amount() == null || supplied.amount().signum() <= 0 || supplied.amount().compareTo(MAX) > 0) {
            throw new BankProblem(400, "INVALID_AMOUNT", "금액은 1~19자리 양수 원 단위여야 합니다.");
        }
        BigDecimal amount;
        try { amount = supplied.amount().setScale(0, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException exception) { throw new BankProblem(400, "INVALID_AMOUNT", "금액은 정수 원 단위여야 합니다."); }
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX) > 0) {
            throw new BankProblem(400, "INVALID_AMOUNT", "금액은 1~19자리 양수 원 단위여야 합니다.");
        }
        return new TransferCommand(supplied.requestId(), supplied.sourceId(), supplied.destinationId(), amount);
    }
}
