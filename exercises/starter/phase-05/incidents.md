# Phase 5 장애 분석 과제 여덟 가지

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

의도적인 오류는 `modules/05-security-operations/src/test/java/kr/banklearning/operations/labs/MaintenanceLabTest.java`와 `MaintenanceLabIT.java`의 격리 fixture에만 있습니다. 정상 WAR와 기본 화면에서 이 오류를 켜지 않습니다.

아래 랩의 PASS는 **의도한 장애 증상을 관찰했다**는 뜻입니다. 수정 완료가 아닙니다. 원본 재현 검증을 유지하고, 학습자의 수정안과 새로운 회귀 검증은 starter 아래 별도 학습 파일이나 분석 기록으로 작성합니다. 원인과 교정 해설은 이 문서에 포함하지 않습니다.

기본 재현 명령:

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -pl modules/05-security-operations -am -Dtest=MaintenanceLabTest -Dsurefire.failIfNoSpecifiedTests=false test
```

개별 재현은 `-Dtest=MaintenanceLabTest#메서드명`으로 선택합니다. 실제 PostgreSQL IT 실행 환경과 데이터 범위는 [maintenance-labs.md](../../../docs/troubleshooting/maintenance-labs.md)를 확인합니다. DB 실행 전 IT는 `NOT RUN`입니다.

## 1. 잘못된 SQL 조건

- 증상: 특정 소유자의 계좌 조회에 다른 소유자의 fixture 계좌가 포함됩니다.
- 재현: `wrongSqlConditionReturnsAnotherOwnersAccount`를 실행합니다. 실제 DB에서는 `wrongSqlConditionLeaksAnotherFixtureOwnersAccount`를 선택합니다.
- 분석 소스: `MaintenanceLabTest`, `MaintenanceLabIT`의 해당 메서드와 조회 fixture입니다.
- 힌트: 요청 소유자와 반환 행 소유자를 비교하고, 입력 조건이 결과 범위를 어떻게 정하는지 표시합니다.
- 과제: 허용 결과 집합을 먼저 정의하고 변경할 SQL 경계와 회귀 사례를 제안합니다.
- 검증: 다른 소유자 fixture가 포함되는 기존 증상을 보존해 기록하고, 수정안에는 해당 소유자만 반환되는 검증을 추가합니다.

## 2. 연결 풀 고갈

- 증상: 연결을 빌린 요청 뒤에 다음 요청이 더 이상 연결을 얻지 못합니다.
- 재현: `exhaustedPoolFixtureRejectsTheNextBorrow`를 실행합니다. 실제 DB의 `realPoolStarvationTimesOutUntilTheBorrowedConnectionIsReturned`는 풀 크기 1에서 빌린 연결을 유지한 뒤 다음 대기와 회복을 관찰합니다.
- 분석 소스: 두 랩의 연결 대여·종료 구간입니다.
- 힌트: 정상, 예외, 조기 반환 경로에서 자원 수명이 같은지 확인합니다.
- 과제: 연결 소유자와 반환 책임을 표시하고 반복 호출·실패 후 회복 검증을 설계합니다.
- 검증: 제한된 대기 후 timeout과 연결 반환 뒤 재대여 성공을 구분합니다. 테스트를 통과시키기 위해 풀 크기만 늘리지 않습니다.

## 3. 느린 쿼리

- 증상: 검색 요청이 필요 이상으로 오래 일하거나 응답을 지연합니다.
- 재현: `missingQueryBudgetAllowsExcessiveWork`를 실행합니다. 실제 DB에서는 `statementTimeoutCancelsTheSlowQuery`가 격리 세션에서 짧은 statement timeout과 `pg_sleep` 취소를 관찰합니다.
- 분석 소스: 랩의 작업 예산·timeout 설정 구간과 실제 DB 실행 구간입니다.
- 힌트: 조회 조건, 반환량, 실행 계획, 대기 시간 중 무엇을 측정하는지 나눕니다.
- 과제: 조사할 지표와 제한 정책, timeout 이후 오류 변환·자원 정리를 제안합니다.
- 검증: DB 취소 SQLSTATE `57014`와 정상 요청의 후속 실행을 확인합니다. sleep 시나리오만으로 실제 업무 쿼리의 실행 계획이 검증되었다고 판단하지 않습니다.

## 4. 잘못된 트랜잭션 범위

- 증상: 출금 뒤 오류가 발생했는데 출금만 남아 있습니다.
- 재현: `autoCommitFixtureLeavesTheDebitAfterFailure`를 실행합니다. 실제 DB의 `wrongAutoCommitScopePersistsOnlyTheDebit`를 함께 관찰합니다.
- 분석 소스: 연결의 auto-commit, 출금·입금 실행과 예외 경계입니다.
- 힌트: 업무 메서드 경계와 실제 commit 경계를 별도로 그립니다.
- 과제: 함께 보존되어야 할 상태와 rollback 경계를 정의하고 신규 실패 회귀 검증을 설계합니다.
- 검증: 잘못된 경계의 부분 잔액 변화와 수정안의 전체 상태 보존을 각각 기록합니다. 실제 DB 검증 전 대역 결과를 rollback PASS로 부르지 않습니다.

## 5. 권한 검증 누락

- 증상: 사용자 한 명이 다른 소유자의 계좌 정보를 얻습니다.
- 재현: `missingAuthorizationReturnsTheOtherCustomersAccount`를 실행합니다.
- 분석 소스: 랩의 principal, 대상 소유자와 접근 판단 구간입니다.
- 힌트: 로그인 여부와 특정 대상에 대한 권한을 각각 질문합니다.
- 과제: 허용·거절 정책과 서버에서 검사할 경계를 정의합니다.
- 검증: 타 소유자 요청은 거절되고 본인 요청은 성공하는 신규 사례를 추가합니다. 화면에서 버튼을 숨긴 결과만 확인하지 않습니다.

## 6. 중복 거래

- 증상: 같은 요청을 두 번 처리하여 잔액이 두 번 감소합니다.
- 재현: `duplicateRequestFixtureDebitsTwice`를 실행합니다.
- 분석 소스: 랩의 요청 식별자와 출금 처리 경계입니다.
- 힌트: HTTP 횟수와 업무 요청 ID의 관계를 확인합니다.
- 과제: 같은 ID·같은 내용과 같은 ID·다른 내용을 구분하는 계약과 검증을 작성합니다.
- 검증: 한 번의 출금·한 번의 한도 사용·정확한 원장 건수가 유지되는지 함께 확인합니다. 실제 DB 중복·동시성 검증은 Phase 4 IT와 연결합니다.

## 7. 세션 만료

- 증상: 만료 시각이 지난 fixture 세션에서 사용자 정보가 계속 반환됩니다.
- 재현: `expiredSessionFixtureStillReturnsAPrincipal`를 실행합니다.
- 분석 소스: 랩의 고정 시각, 만료 조건과 principal 복원 경로입니다.
- 힌트: 현재 시각이 만료 시각과 같거나 늦은 두 경계를 비교합니다.
- 과제: 만료 뒤 재인증 계약과 상태 제거 정책, 경계 시간 테스트를 제안합니다.
- 검증: 만료 전 성공, 만료 시점·이후 거절을 확인합니다. 실제 로그인 세션 검증에서는 HTTP Basic 재인증 헤더를 빼고 관찰합니다.

## 8. 잘못된 데이터 매핑

- 증상: 조회는 성공하지만 화면의 고객·계좌·잔액 필드가 잘못 연결됩니다.
- 재현: `swappedColumnMappingCorruptsTheView`를 실행합니다.
- 분석 소스: 랩의 원본 행과 변환된 view 생성 구간입니다.
- 힌트: 비슷한 타입의 필드를 이름·열 위치·생성자 순서로 나눕니다.
- 과제: 데이터 사전과 명시적 매핑 기준을 작성하고 각 필드의 기대값을 고정한 회귀 검증을 설계합니다.
- 검증: 서로 다른 sentinel 값을 써서 자리 교환을 발견하고 금액 정밀도·null 처리도 검토합니다.
