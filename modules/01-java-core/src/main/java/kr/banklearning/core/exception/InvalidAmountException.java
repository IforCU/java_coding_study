package kr.banklearning.core.exception;

public final class InvalidAmountException extends BankException {
    public InvalidAmountException() {
        super("금액은 0보다 큰 정수 원 단위여야 합니다.");
    }
}
