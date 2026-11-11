package kr.banklearning.exercise;

/** 기본 WAR와 분리된 Phase 2 이름 검증 실습이다. */
public final class CustomerNameValidator {
    private CustomerNameValidator() {}

    public static String normalize(String name) {
        // TODO: 앞뒤 공백을 제거하고 1~80자 계약을 검증하세요.
        if(name == null) {
            throw new IllegalArgumentException("고객 이름은 1~80자여야 합니다.");
        }

        String nString = name.strip();
        if(nString.isEmpty() || nString.length() > 80) {
            throw new IllegalArgumentException("고객 이름은 1~80자여야 합니다.");
        }

        return nString;
    }
}
