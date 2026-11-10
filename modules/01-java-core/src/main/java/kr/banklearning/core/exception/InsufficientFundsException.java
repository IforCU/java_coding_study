package kr.banklearning.core.exception;

public final class InsufficientFundsException extends BankException {
    public InsufficientFundsException() {
        super("잔액이 부족합니다.");
    }
}
