package kr.banklearning.servlet.service;

public final class MissingException extends RuntimeException {
    public MissingException() {
        super("요청한 고객 또는 계좌를 찾을 수 없습니다.");
    }
}
