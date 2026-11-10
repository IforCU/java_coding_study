# 유지보수 장애 8종: 증상 재현과 검증 범위

이 실습은 정상 보안 앱에 장애를 심지 않는다. 의도적으로 잘못된 동작은 `src/test`의 private `Lab` fixture에만 있고 WAR에는 포함하지 않는다. 메모리 재현은 [MaintenanceLabTest](../../modules/05-security-operations/src/test/java/kr/banklearning/operations/labs/MaintenanceLabTest.java), 실제 PostgreSQL 재현은 [MaintenanceLabIT](../../modules/05-security-operations/src/test/java/kr/banklearning/operations/labs/MaintenanceLabIT.java)에 있다.

여기의 재현 테스트가 PASS라는 것은 **의도한 장애 증상을 관찰했다**는 뜻이다. 정상 구현이 취약하다거나 학습자가 장애를 수정했다는 뜻이 아니다. 문제·수정 TODO는 [사건별 과제](../../exercises/starter/phase-05/incidents.md)에 있고 선택해서 보는 해설은 [별도 정답](../../exercises/solutions/phase-05/incident-solutions.md)에 있다. 이 문서에는 수정 정답 코드를 싣지 않는다.

아래 명령은 격리 랩의 재현 절차다. 실제 DB가 준비되지 않아 IT를 실행하지 못했다면 NOT RUN으로 기록한다. 단위 재현 성공을 실제 DB·HTTP·Spring Security 검증 결과로 합치지 않는다. 최신 실행 결과는 [PROGRESS.md](../../PROGRESS.md)에서 확인한다.

## 실행 환경과 데이터 격리

| 구분 | 대상과 실행 방식 | 확인 가능한 것 |
| --- | --- | --- |
| 메모리 재현 | 기본 JUnit `MaintenanceLabTest`, 8개 테스트 | 잘못된 정책·상태 전이·컬럼 매핑의 증상, 풀 대여/쿼리 작업량의 단순 모델 |
| 실제 DB 재현 | opt-in `MaintenanceLabIT`, 4개 테스트 | PostgreSQL 조회 조건, Hikari 대여 timeout, 서버 statement timeout, JDBC autoCommit의 부분 저장 |
| 정상 보안 앱 검증 | 정상 모듈의 별도 보안 회귀 테스트 및 실제 HTTP 확인 | 인증·인가·CSRF·세션·XSS 출력 정책의 실제 구현 계약 |

실제 DB 재현은 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 필수로 요구하며 누락 시 실패한다. 루트 migration V005가 만든 **`phase5_lab.account(id, owner_id, balance)`**만 사용한다. 정상 앱의 `phase5.profile`·`phase5.audit_event`와 물리 스키마를 나눈다. 테스트는 새로운 UUID의 가상 계좌 행만 추가하고, 자신의 UUID 범위만 읽거나 변경한다. 기존 seed 삭제·데이터 초기화·DDL은 수행하지 않는다.

SQL 조건 실습은 현재 테스트의 세 계좌 UUID에 한정해 두 소유자를 만든다. 트랜잭션 경계 실습은 자신의 가상 계좌에 부분 실패 결과를 의도적으로 남긴다. 성공한 fixture는 보존되므로 반복 실행하면 lab 행이 늘어난다. slow query 설정은 `SET LOCAL`로 테스트 트랜잭션에만 적용하고 마지막에 rollback한다. 풀 실습은 테스트가 만든 풀과 모든 Connection을 종료한다.

## 증상·조사 힌트·학습자가 확인할 계약

| 사건 | 관찰할 증상 | 재현 fixture / 조사 힌트 | 수정 TODO와 회귀 검증 계약 |
| --- | --- | --- | --- |
| 1. 잘못된 SQL 조건 | 현재 소유자의 목록에 다른 소유자의 계좌가 섞인다. | 메모리 2행, 실제 DB 자기 fixture 3행. UUID 범위와 사용자 범위를 각각 추적한다. | 과제의 조회 계약을 만족시키고 다른 소유자의 UUID가 결과에 없는지 검증한다. 이름/금액이 우연히 같아도 통과하지 않아야 한다. |
| 2. 연결 풀 고갈 | 첫 대여가 남아 있는 동안 다음 대여가 실패한다. | 단위 테스트는 permit 모델, IT는 Hikari 최대 연결 1개. 활성/유휴 연결 수와 요청별 자원 수명을 조사한다. | 정상·예외 경로를 각각 검증하고 반복 요청 후 다음 대여가 가능한지 확인한다. 풀 크기를 키워 증상을 숨기는 결과는 제외한다. |
| 3. 느린 쿼리 | 작업량이 정한 예산을 넘거나 SQL 실행이 취소된다. | 단위 테스트는 1,000행 작업량 모델이며 실제 시간 측정이 아니다. IT는 `pg_sleep(0.5)`와 100ms statement timeout이다. | 입력 범위·SQL 계획·timeout의 책임을 구분해 과제를 작성한다. 빠른 정상 경로와 제한 초과 경로를 모두 검증한다. |
| 4. 잘못된 트랜잭션 범위 | 출금 후 실패했는데 출금만 저장되어 합계가 줄어든다. | 단위 모델과 실제 autoCommit JDBC 경로. 예외 발생 시점과 commit 범위를 비교한다. | 두 잔액과 요청·원장·누계를 함께 검증한다. 예외 응답만 확인하고 저장 상태를 생략하지 않는다. |
| 5. 권한 검사 누락 | 다른 고객의 계좌 ID를 지정하면 그 계좌가 반환된다. | 독립 메모리 정책 fixture다. 로그인 주체, URL의 객체 ID, 저장된 소유자를 따로 표시한다. | 자기 자원·타인 자원·관리자 권한을 나눠 과제 계약을 검증한다. 이 fixture는 실제 Spring Security HTTP 시험이 아니다. |
| 6. 중복 이체 | 같은 요청 ID를 두 번 호출하면 두 번 출금된다. | 메모리 상태 모델. 동일 키·동일 payload와 다른 payload의 구분을 조사한다. | 순차·동시 재시도, 응답 유실 뒤 재시도의 잔액·원장·요청 수를 확인한다. 실제 DB 기준 구현의 검증은 Phase 4 IT다. |
| 7. 만료된 세션 | 만료 시각을 지난 fixture에서도 주체가 반환된다. | 고정 Instant를 사용하는 인증 정책 모델이다. 실제 브라우저 세션이나 쿠키 만료 시험이 아니다. | 만료 전·정확한 경계·만료 후를 검증하고 이전 주체가 보호 자원에 접근하지 못하는 계약을 확인한다. |
| 8. 잘못된 매핑 | 잔액과 한도 값이 서로 바뀐 DTO가 만들어진다. | 서로 다른 값인 잔액 100·한도 500을 사용한다. SELECT alias·컬럼 이름·생성자/필드를 대조한다. | 같은 타입의 컬럼 두 개도 구분하는 기대값을 만들고, 서로 다른 금액과 NULL/빈 결과 계약을 확인한다. |

## 메서드별 재현 위치

아래 이름은 실제 테스트 메서드와 일치한다. 한 사건의 재현을 선택할 때 사용하는 목록이다.

| 사건 | `MaintenanceLabTest` 메서드 | `MaintenanceLabIT` 메서드 |
| --- | --- | --- |
| 잘못된 SQL 조건 | `wrongSqlConditionReturnsAnotherOwnersAccount` | `wrongSqlConditionLeaksAnotherFixtureOwnersAccount` |
| 연결 풀 고갈 | `exhaustedPoolFixtureRejectsTheNextBorrow` | `realPoolStarvationTimesOutUntilTheBorrowedConnectionIsReturned` |
| 느린 쿼리 | `missingQueryBudgetAllowsExcessiveWork` | `statementTimeoutCancelsTheSlowQuery` |
| 잘못된 트랜잭션 범위 | `autoCommitFixtureLeavesTheDebitAfterFailure` | `wrongAutoCommitScopePersistsOnlyTheDebit` |
| 권한 검사 누락 | `missingAuthorizationReturnsTheOtherCustomersAccount` | 해당 없음: 메모리 정책 모델 |
| 중복 이체 | `duplicateRequestFixtureDebitsTwice` | 해당 없음: 실제 DB 정상 기준은 Phase 4 `TransferServiceIT` |
| 만료된 세션 | `expiredSessionFixtureStillReturnsAPrincipal` | 해당 없음: 메모리 정책 모델 |
| 잘못된 매핑 | `swappedColumnMappingCorruptsTheView` | 해당 없음: 메모리 컬럼 모델 |

## 실행과 결과 읽기

아래 예는 프로젝트 내부 Maven이 준비된 Windows 환경에서 루트 디렉터리 기준으로 실행한다. 호스트 전역 설치나 시스템 설정 변경은 필요하지 않다. `.tools`가 없다면 루트 [README](../../README.md)의 빌드 경로를 먼저 따른다.

```powershell
$labRoot = (Get-Location).Path
$labMaven = Join-Path $labRoot '.tools/apache-maven-3.10.0/bin/mvn.cmd'
$labRepository = Join-Path $labRoot '.cache/maven-repository'
$env:MAVEN_OPTS = '-Duser.home=' + $labRoot + '\.cache\java-home -Djava.io.tmpdir=' + $labRoot + '\.cache\tmp'

# 메모리 모델 8개. 실제 DB를 사용하지 않는다.
& $labMaven "-Dmaven.repo.local=$labRepository" -pl modules/05-security-operations -am `
  '-Dtest=MaintenanceLabTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

# 메모리 재현 중 한 사건만 선택한다.
& $labMaven "-Dmaven.repo.local=$labRepository" -pl modules/05-security-operations -am `
  '-Dtest=MaintenanceLabTest#expiredSessionFixtureStillReturnsAPrincipal' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test

# V005까지 migration한 로컬 DB와 DB_URL/DB_USER/DB_PASSWORD가 준비됐을 때만 실행한다.
& $labMaven "-Dmaven.repo.local=$labRepository" -pl modules/05-security-operations -am `
  -Pintegration-tests '-Dit.test=MaintenanceLabIT' '-Dfailsafe.failIfNoSpecifiedTests=false' verify
```

기본 Surefire는 `*IT`를 실행하지 않는다. IT를 작성·컴파일한 결과와 네 가지 실제 DB 재현을 실행한 결과를 구분한다. 특정 실제 DB 사건만 실행하려면 `-Dit.test=MaintenanceLabIT#메서드명`을 사용한다. 환경변수나 migration이 없어서 실패한 결과를 장애 재현 PASS로 분류하지 않는다.

풀 IT는 첫 Connection을 먼저 확보한 뒤 후속 대여 timeout을 250ms로 설정한다. Hikari 설정의 단위와 최소값은 [공식 설정 설명](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)을 기준으로 한다. timeout까지 정확히 250ms가 걸렸다고 단정하는 wall-clock assertion은 사용하지 않는다.

slow query IT는 PostgreSQL의 [statement_timeout](https://www.postgresql.org/docs/17/runtime-config-client.html#GUC-STATEMENT-TIMEOUT)으로 [pg_sleep](https://www.postgresql.org/docs/17/functions-datetime.html#FUNCTIONS-DATETIME-DELAY)의 실행이 취소되는지 확인한다. SQLState `57014`는 query_canceled이며 일반 접속 실패와 구분한다. [공식 오류 코드](https://www.postgresql.org/docs/17/errcodes-appendix.html)

수정 과제는 정상 앱이나 재현 테스트의 증상 assertion을 지워서 통과시키지 않는다. [starter](../../exercises/starter/phase-05/incidents.md)의 TODO와 기대 계약을 대상으로 별도 검증을 작성하고, 진단 기록에는 재현 환경·관찰한 저장 상태·최소 변경·회귀 결과를 남긴다. 실패 원인을 설명할 때도 원본 개인정보·자격 증명·SQL 예외 전문을 HTTP 응답이나 로그에 넣지 않는다.
