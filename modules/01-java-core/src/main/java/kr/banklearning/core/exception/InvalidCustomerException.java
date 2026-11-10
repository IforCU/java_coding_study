package kr.banklearning.core.exception;

public final class InvalidCustomerException extends BankException {
    public InvalidCustomerException() {
        super("고객 이름은 1~100자여야 합니다.");
    }
}
