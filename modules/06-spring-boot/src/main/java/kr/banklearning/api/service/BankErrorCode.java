package kr.banklearning.api.service;

public enum BankErrorCode {
    INVALID_REQUEST(400, "INVALID_REQUEST"),
    NOT_FOUND(404, "NOT_FOUND"),
    FORBIDDEN(403, "FORBIDDEN"),
    METHOD_NOT_ALLOWED(405, "INVALID_REQUEST"),
    STORE_UNAVAILABLE(503, "STORE_UNAVAILABLE"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR");

    private final int status;
    private final String code;

    private BankErrorCode(int status, String code){
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }    
}
