# 기존 거래 모듈과 조회 API의 데이터 소유권

Phase 6은 Phase 4가 작성한 계좌·이체·원장 데이터를 조회하는 별도 Spring Boot 애플리케이션이다. 업무 데이터의 쓰기 규칙은 Phase 4가 소유하고 API는 정해진 SELECT 계약을 사용한다. Servlet/JSP, Spring MVC, 트랜잭션, 보안 학습 모듈은 각각 독립 실행 경로를 유지한다.

아래 내용은 데이터 소유권과 검증 계약이다. 실제 역할 생성·PostgreSQL 권한 거절·기존 기능 회귀·HTTP 실행의 최신 결과는 [진행 기록](../../PROGRESS.md)에서 확인한다. 실행하지 못한 항목은 NOT RUN으로 기록한다.

## 누가 무엇을 읽고 쓰는가

| 데이터 | 업무 쓰기 소유자 | Phase 6 조회 범위 |
| --- | --- | --- |
| `phase4.customer` | Phase 4 고객/거래 데이터 계약 | 고객 ID·이름·상태 |
| `phase4.account` | Phase 4 이체 트랜잭션 | 소유 고객·잔액·일일 한도·version |
| `phase4.transfer_request` | Phase 4 멱등 요청·완료·취소 요청 | 업무일별 완료 이체 통계 |
| `phase4.ledger` | Phase 4의 출금/입금 원장 쌍 | 계좌별 이력·기간·최대 반환 건수 |
| `phase4.daily_usage` | Phase 4의 출금 누계 | 업무일별 사용액과 잔여 한도 계산 |
| `phase2`, `phase3` | 각 독립 학습 모듈 | 조회하지 않음 |
| `phase5`, `phase5_lab` | 정상 보안 모듈 / 격리 장애 fixture | 조회하지 않음 |

migration과 명시적인 통합 테스트 fixture 준비는 업무 실행과 다른 관리 경로다. Phase 6에 고객 등록·입출금·이체·원장 수정 SQL은 없다. Phase 2에서 만든 고객이 Phase 6에 자동으로 나타나는 구조도 아니다. 서로 다른 스키마의 학습 데이터를 이름만 보고 같은 고객으로 합치지 않는다.

```text
Phase 4 WAR
  → TransferService의 Spring 트랜잭션
  → JdbcTransferRepository
  → phase4의 계좌 / 요청 / 원장 / 누계 commit
                  │
                  │ 커밋된 데이터 조회
                  ▼
Phase 6 Boot API
  → 인증·역할·소유 범위 검사
  → BankReadService @Transactional(readOnly = true)
  → JdbcBankReadRepository (SELECT만 사용)
  → bank_learning_api DB 역할
```

[조회 repository](../../modules/06-spring-boot/src/main/java/kr/banklearning/api/repository/JdbcBankReadRepository.java)는 기존 Phase 4 Java 서비스 클래스를 호출하거나 내부 Connection을 공유하지 않는다. 기존 앱의 세션도 공유하지 않는다. 웹 인증 계정, 거래 요청 ID, DB 접속 역할은 서로 다른 식별자다. HTTP 계약과 권한 범위는 [api-contract.md](api-contract.md)를 따른다.

## 제한된 DB 역할로 경계를 검증하기

[V006](../../database/migrations/V006__api_reader.sql)은 관리용 migrator가 실행하는 psql 스크립트다. 기존 역할이 없으면 `bank_learning_api`를 LOGIN / NOSUPERUSER / NOCREATEDB / NOCREATEROLE / NOINHERIT / NOREPLICATION / NOBYPASSRLS로 만든다. 현재 DB의 CONNECT, `phase4` 스키마의 USAGE, 위 다섯 테이블의 SELECT를 명시적으로 부여한다. 업무 테이블의 소유권이나 INSERT/UPDATE/DELETE 권한은 부여하지 않는다. [PostgreSQL CREATE ROLE](https://www.postgresql.org/docs/17/sql-createrole.html), [GRANT](https://www.postgresql.org/docs/17/sql-grant.html)

새 역할에는 `default_transaction_read_only=on`을 설정한다. 이 기본값과 Hikari의 `read-only=true`, Spring의 readOnly 선언은 쓰기 경계를 드러내지만 그것만으로 DB 권한을 대신하지 않는다. 개별 트랜잭션에서 READ WRITE로 바꾸더라도 업무 테이블의 UPDATE 권한이 없어서 실패해야 한다. [트랜잭션 접근 모드](https://www.postgresql.org/docs/17/sql-set-transaction.html), [역할별 기본 설정](https://www.postgresql.org/docs/17/sql-alterrole.html)

V006은 root SQL들과 같은 advisory lock `7100000`을 사용한다. 역할 생성은 `api_password` psql 변수의 안전한 리터럴 quoting과 `format`/`\gexec`로 처리한다. 이 파일에는 psql meta-command가 있으므로 JDBC migration SQL로 그대로 보내면 안 된다. migrator는 `API_DB_PASSWORD`를 `psql -v api_password=...`로 전달하고 `ON_ERROR_STOP=1`로 실패를 중단한다. SQL 전문이나 비밀번호를 출력하는 echo 옵션은 사용하지 않는다. [psql 변수·gexec](https://www.postgresql.org/docs/17/app-psql.html)

재실행 시 기존 역할의 비밀번호·속성·기본 설정은 덮어쓰지 않는다. 필요한 SELECT 권한은 다시 부여하지만 사용자가 추가한 다른 권한을 자동으로 회수하지 않는다. 따라서 기존 역할에 실험용 쓰기 권한을 줬다면 V006 재실행이 이를 자동 복구하지 않는다. 통합 검증에서 실제 유효 권한을 확인해 상태를 드러낸다. 비밀번호만 환경변수에서 바꾸면 이미 만들어진 DB 역할의 비밀번호도 바뀐다고 가정하지 않는다.

이 역할의 범위는 업무 테이블 접근 권한이다. PostgreSQL의 PUBLIC 기본 권한이나 별도 사용자 역할 실험까지 전체 클러스터 정책을 재설정하는 migration은 아니다. 기존 사용자 설정을 초기화하거나 광범위한 REVOKE를 실행하지 않는다.

## 관리용 접속과 실행용 접속을 구분하기

| 실행 위치 | 설정 의미 |
| --- | --- |
| DB/migrator | `POSTGRES_USER`, `POSTGRES_PASSWORD`로 schema·role·seed 구성 |
| Phase 4 | 기존 학습 DB의 업무 쓰기 접속 설정 유지 |
| Phase 6 컨테이너 | `API_DB_USER`, `API_DB_PASSWORD`를 앱의 `DB_USER`, `DB_PASSWORD`로 전달해 reader 역할 사용 |
| `BankReadRepositoryIT` | `DB_URL/DB_USER/DB_PASSWORD`는 자기 fixture를 만드는 owner, `API_DB_USER/API_DB_PASSWORD`는 실제 조회 repository의 reader |

기본 reader 이름은 `bank_learning_api`, 공개된 로컬 가상 비밀번호 기본값은 `learning-api-only-password`다. `API_DB_USER`를 바꾸면 V006이 임의 이름의 역할을 새로 만드는 것은 아니므로 해당 역할과 SELECT 계약을 별도로 준비해야 한다. 기존 볼륨의 reader 비밀번호가 다르면 실제 저장된 값과 실행 환경을 맞춘다. 관리자 계정을 Boot API에 주입해 조회가 되는지만 확인하면 권한 분리가 검증되지 않는다.

## 트랜잭션은 HTTP 경계를 넘어 이어지지 않는다

Phase 4에서 시작한 Spring 트랜잭션이 Phase 6의 별도 HTTP 요청으로 전달되는 구조가 아니다. 두 애플리케이션은 각자의 DataSource·Connection·트랜잭션을 사용한다. Phase 4의 commit 전에 조회 API를 호출하면 그 이체가 아직 보이지 않을 수 있다. commit 이후에도 서로 다른 API 요청의 결과가 같은 시점의 스냅샷이라고 가정하지 않는다.

PostgreSQL READ COMMITTED에서는 각 SELECT가 문장 시작 시점의 커밋된 데이터를 본다. readOnly 선언만으로 여러 문장의 스냅샷이 고정되지는 않는다. 현재 `BankReadService.limit`은 별도로 `@Transactional(readOnly=true, isolation=REPEATABLE_READ)`를 지정해 계좌 한도와 일일 사용액을 같은 트랜잭션 snapshot으로 읽는다. 이 보장은 Spring 프록시로 해당 메서드에 진입했을 때 적용되며 단위 테스트의 직접 객체 호출과 구분한다. [PostgreSQL 격리 수준](https://www.postgresql.org/docs/17/transaction-iso.html)

다른 조회 메서드는 class의 기본 readOnly/DB 기본 격리 수준을 유지한다. 단일 통계 SELECT의 count/sum과 별도 호출한 잔액·이력이 같은 순간을 나타낸다고 단정하지 않는다. 다른 복합 보고서에 고정 시점이 필요하면 계약에 기준 시점이나 적절한 snapshot 범위를 정하고 검증해야 한다. 현재 API에는 분산 트랜잭션·snapshot token·cursor 페이지가 없다. 이력은 최근순 제한 조회이며 호출 사이의 동시 삽입까지 고정하는 페이지 계약은 아니다.

조회가 업무 데이터를 갱신하지 않아도 같은 DB의 CPU·I/O·Connection을 사용한다. repository의 query timeout, Hikari 최대 연결 수, 반환 건수 제한을 유지하고 실제 부하에서 지연을 따로 확인한다. 장시간 조회가 기존 거래에 미치는 자원 영향까지 단위 테스트 통과만으로 보장하지 않는다.

## 날짜·한도·취소 요청의 의미

업무일과 날짜 입력의 기준은 Asia/Seoul이다. 이력의 날짜 범위는 시작일 자정 이상, 종료일 다음 날 자정 미만이다. DB의 `timestamptz`는 시점으로 해석하고 응답의 offset 표현이 달라도 같은 Instant인지 확인한다. 통계는 저장된 `business_date`의 양 끝을 포함한다.

한도 응답은 계좌의 `daily_limit`과 오늘 업무일의 `daily_usage.used`를 읽고 남은 값을 계산한다. 이 조회는 한도를 예약하지 않으며 다음 이체의 성공을 보장하는 승인이 아니다. 실제 이체 시 Phase 4가 잠금 안에서 다시 잔액과 한도를 검사한다.

통계는 `COMPLETED`, `CANCEL_REQUESTED`를 포함하고 `PENDING`은 제외한다. 취소 요청 접수는 자금의 역이체나 환급 완료를 뜻하지 않으므로 완료된 자금 이동과 일일 사용액이 남아 있다. 상태명만 보고 취소 요청 이체를 통계에서 빼거나 잔액을 반대로 수정하지 않는다. 자세한 업무 경계는 [Phase 4 문서](../banking-domain/transaction-boundaries.md)에 있다.

## 실제 PostgreSQL 검증의 범위

[BankReadRepositoryIT](../../modules/06-spring-boot/src/test/java/kr/banklearning/api/repository/BankReadRepositoryIT.java)는 다음 세 검증을 제공한다.

1. owner로 가상 UUID 고객·계좌·완료/취소 요청/대기 요청·원장·사용액 fixture를 commit한 뒤 reader 역할의 실제 repository로 조회한다. 고객/계좌, BigDecimal, timestamp, 정렬·기간·limit, 일일 사용액, 통계 포함 상태와 기존 잔액 보존을 확인한다. 통계는 이전 값 대비 차이를 비교하고 fixture 날짜를 일반 업무일과 떨어뜨려 반복 데이터의 간섭을 줄인다.
2. 실제 유효 SELECT 권한과 쓰기 권한 부재 및 역할 속성을 확인한다. READ WRITE 트랜잭션을 명시한 뒤 `UPDATE ... WHERE false`를 시도해 SQLState `42501`을 확인한다. 행이 바뀌지 않는 문장으로 DB ACL을 검증하고 마지막에 rollback한다.
3. 실제 Spring 트랜잭션 프록시로 `limit`을 호출한다. 첫 계좌 SELECT 후 별도 owner Connection에서 자기 fixture의 추가 이체·원장·사용액과 한도 변경을 commit해도 reader의 REPEATABLE READ 트랜잭션은 이전의 일관된 쌍을 반환하고, 종료 후 새 조회는 새 값을 읽는지 확인한다.

fixture는 기존 seed를 사용하거나 삭제하지 않는다. 테스트가 추가한 성공 행은 보존되며 생성한 대기 요청은 통계 제외 조건을 확인하기 위한 fixture다. 이 테스트는 repository·DB 권한 검증이며 실제 HTTP 인증, JSON 직렬화, 기존 WAR의 화면 회귀, 재시작 지속성 검증은 별도로 실행한다. 실제 DB 미준비 상태에서 작성·컴파일만 했다면 실행 결과를 PASS로 올리지 않는다.
