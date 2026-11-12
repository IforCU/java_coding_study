package kr.banklearning.api.service;

public final class BankProblem extends RuntimeException {
    private final BankErrorCode errorCode;

    public BankProblem(BankErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
    }

    public BankErrorCode getErrorCode() {
        return errorCode;
    }

    public int status() {
        return errorCode.getStatus();
    }

    public String code() {
        return errorCode.getCode();
    }
}
