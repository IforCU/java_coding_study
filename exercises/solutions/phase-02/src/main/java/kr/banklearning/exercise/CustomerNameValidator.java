package kr.banklearning.exercise;

public final class CustomerNameValidator {
    private CustomerNameValidator() {}

    public static String normalize(String name) {
        if (name == null) {
            throw new IllegalArgumentException("고객 이름은 1~80자여야 합니다.");
        }
        String normalized = name.strip();
        if (normalized.isEmpty() || normalized.length() > 80) {
            throw new IllegalArgumentException("고객 이름은 1~80자여야 합니다.");
        }
        return normalized;
    }
}
