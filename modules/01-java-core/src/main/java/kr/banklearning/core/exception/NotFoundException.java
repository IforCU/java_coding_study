package kr.banklearning.core.exception;

public final class NotFoundException extends BankException {
    public NotFoundException(String entity) {
        super(entity + "을(를) 찾을 수 없습니다.");
    }
}
