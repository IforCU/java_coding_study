package kr.banklearning.api.service;

public final class BankProblem extends RuntimeException {
    private final int status;
    private final String code;

    public BankProblem(int status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }
}
