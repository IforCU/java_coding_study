package kr.banklearning.transaction.service;

public final class BankProblem extends RuntimeException {
    private final int status;
    private final String code;
    public BankProblem(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    public int status() { return status; }
    public String code() { return code; }
}
