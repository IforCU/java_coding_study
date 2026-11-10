package kr.banklearning.transaction.service;

/** 운영 기준 구현에서는 아무 작업도 하지 않으며 통합 테스트가 출금 직후 실패를 주입한다. */
@FunctionalInterface
public interface FailureHook {
    void afterDebit();
}
