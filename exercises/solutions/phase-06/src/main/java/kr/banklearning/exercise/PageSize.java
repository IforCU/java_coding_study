package kr.banklearning.exercise;

public final class PageSize {
    private PageSize() {}

    public static int resolve(Integer supplied) {
        if (supplied == null) return 50;
        if (supplied < 1 || supplied > 200) {
            throw new IllegalArgumentException("조회 건수 범위를 확인하세요.");
        }
        return supplied;
    }
}
