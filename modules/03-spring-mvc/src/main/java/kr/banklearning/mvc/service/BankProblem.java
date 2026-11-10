package kr.banklearning.mvc.service;

public final class BankProblem extends RuntimeException {
    private final int status;
    public BankProblem(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
}
