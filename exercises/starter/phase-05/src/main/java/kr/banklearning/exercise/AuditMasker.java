package kr.banklearning.exercise;

/** 정상 애플리케이션과 분리한 표시용 마스킹 실습이다. */
public final class AuditMasker {
    private AuditMasker() {}

    public static String maskAccount(String account) {
        // TODO: 허용한 구분자를 제거하고 검증한 뒤 마지막 네 자리만 표시하세요.
        if(account == null) throw invalid();
        String normalized = account.replace(" ", "").replace("-", "");
        if(!normalized.matches("[0-9]{8,20}")) throw invalid();
        return "*".repeat(normalized.length() - 4) + normalized.substring(normalized.length() - 4);
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("학습 계좌 형식을 확인하세요.");
    }
}
