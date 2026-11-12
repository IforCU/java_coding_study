package kr.banklearning.exercise;

/** 정상 API의 조회 건수 정책과 분리된 실습이다. */
public final class PageSize {
    private PageSize() {}

    public static int resolve(Integer supplied) {
        if(supplied == null) return 50;
        if(supplied <= 0 || supplied > 200) throw new IllegalArgumentException("조회 건수 범위를 확인하세요.");
        return supplied;
    }
}
