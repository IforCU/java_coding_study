package kr.banklearning.operations.util;

public final class PrivacyMasker {
    private PrivacyMasker() {}

    public static String email(String value) {
        if (value == null || value.isBlank()) return "";
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1) return "***";
        return value.substring(0, 1) + "***@" + value.substring(at + 1);
    }

    public static String account(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }

    public static String username(String value) {
        return value == null || value.isBlank() ? "anonymous" : value.substring(0, 1) + "***";
    }
}
