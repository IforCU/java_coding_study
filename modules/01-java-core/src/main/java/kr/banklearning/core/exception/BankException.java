package kr.banklearning.core.exception;

/** 업무 실패와 프로그래밍 오류를 구분하기 위한 최상위 업무 예외. */
public abstract class BankException extends RuntimeException {
    protected BankException(String message) {
        super(message);
    }
}
