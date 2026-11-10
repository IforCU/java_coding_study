package kr.banklearning.exercise;

public final class AuditMasker {
    private AuditMasker() {}

    public static String maskAccount(String account) {
        if (account == null) throw invalid();
        String normalized = account.replace(" ", "").replace("-", "");
        if (!normalized.matches("[0-9]{8,20}")) throw invalid();
        return "*".repeat(normalized.length() - 4) + normalized.substring(normalized.length() - 4);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("학습 계좌 형식을 확인하세요.");
    }
}
