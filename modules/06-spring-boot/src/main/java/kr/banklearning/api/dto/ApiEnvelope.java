package kr.banklearning.api.dto;

public record ApiEnvelope<T>(boolean success, T data, ApiError error, String requestId) {
    public record ApiError(String code, String message) {}

    public static <T> ApiEnvelope<T> ok(T data, String trace) {
        return new ApiEnvelope<>(true, data, null, trace);
    }

    public static ApiEnvelope<Void> failure(String code, String message, String trace) {
        return new ApiEnvelope<>(false, null, new ApiError(code, message), trace);
    }
}
