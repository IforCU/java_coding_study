# 이체 한 건의 트랜잭션 경계와 실패 복구

Phase 4는 하나의 PostgreSQL 안에서 두 계좌의 자금을 이동하는 로컬 학습 모델이다. 금액은 Java `BigDecimal`과 SQL `numeric(19,0)`으로 표현하고 정수 원 단위를 검증한다. 출금·입금·요청 상태·원장·일일 사용액을 한 트랜잭션으로 묶는다. 외부 금융기관 호출이나 실제 고객 데이터는 사용하지 않는다.

이 문서는 트랜잭션 계약과 재현할 시나리오를 설명한다. 실제 PostgreSQL 통합 테스트, HTTP 실행, DB 재시작 영속성 검증의 최신 결과는 [진행 기록](../../PROGRESS.md)에서 확인한다. 문서의 예상 결과와 실제 통과 결과를 구분하고 수행하지 않은 항목은 NOT RUN으로 기록한다.

## 성공과 실패가 함께 결정되는 저장 범위

```text
Spring 트랜잭션 프록시 → TransferService.transfer(command)
  │
  ├─ 입력 금액/ID 검증
  ├─ request_id 고유값으로 PENDING 요청 예약
  │    └─ 같은 요청이면 payload 비교 후 저장된 결과 재사용
  ├─ 두 계좌를 UUID 순서로 FOR NO KEY UPDATE 잠금
  ├─ 출금 잔액·일일 사용액/한도·입금 범위 검사
  ├─ 출금 잔액 갱신 → FailureHook.afterDebit()
  ├─ 입금 잔액 갱신
  ├─ WITHDRAWAL 원장 + DEPOSIT 원장
  ├─ 출금 계좌/업무일의 daily_usage 갱신
  └─ COMPLETED → commit → 호출자에게 결과 반환

중간 RuntimeException / DB 예외 → rollback
  └─ 두 잔액·version·요청·원장·일일 사용액을 모두 이전 상태로 복구
```

실제 호출은 [TransferService](../../modules/04-banking-transaction/src/main/java/kr/banklearning/transaction/service/TransferService.java)와 [JdbcTransferRepository](../../modules/04-banking-transaction/src/main/java/kr/banklearning/transaction/repository/JdbcTransferRepository.java)를 따라간다. repository는 Spring JdbcTemplate을 통해 같은 DataSource의 트랜잭션에 참여한다. 단순히 서비스에 annotation을 붙이고 직접 `new TransferService(...)`를 호출하면 프록시가 만들어지지 않는다. [Spring @Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

기본 rollback 대상인 RuntimeException과 Error를 이해하고 업무 예외를 삼키지 않는다. checked exception이 추가되면 필요한 rollback 규칙을 명시하고 실제 실패 테스트를 추가한다. `transfer` 안의 `get` 직접 호출은 바깥 쓰기 트랜잭션 안에서 실행된다. 내부 `get`의 readOnly annotation이 새로운 트랜잭션을 만드는 것은 아니다. [Spring 프록시와 rollback 기본값](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

## ACID를 관찰 가능한 값으로 연결하기

| 성질 | 이 프로젝트에서 확인할 값 |
| --- | --- |
| 원자성 Atomicity | 출금 직후 실패해도 출금 계좌만 줄어든 상태가 남지 않는다. 요청·원장·일일 사용액도 함께 rollback한다. |
| 일관성 Consistency | 잔액 비음수, 금액 양수, FK, 요청 키·원장 고유 제약을 유지한다. 성공 이체마다 같은 금액의 출금·입금 원장 두 건이 생긴다. |
| 격리성 Isolation | 같은 출금 계좌의 경쟁 요청이 잠금 뒤 최신 잔액과 사용액을 기준으로 판단한다. 동시 요청 테스트에서 음수 잔액이나 초과 사용액이 없어야 한다. |
| 지속성 Durability | commit한 결과는 정상 DB 복구 뒤에도 남아야 한다. 이 성질의 실제 재시작·볼륨 검증은 별도로 실행해야 한다. |

DB 제약은 고객/계좌 존재와 각 행의 허용 값을 지키지만 여러 원장의 합과 잔액의 일치를 자동으로 증명하지 않는다. 그 관계는 트랜잭션 흐름과 검증 쿼리로 확인한다. `PENDING`은 처리 중의 내부 상태다. 이 구현에서는 성공 전 예약과 변경을 같은 트랜잭션에 두므로 실패한 새 요청은 예약 행도 남기지 않는다. 실패 이력을 영속적으로 보관하는 별도 업무 감사 저장소는 현재 모델의 구현 범위가 아니다.

V004는 [전용 스키마](../../database/migrations/V004__transaction.sql)를 추가하고 R004는 [가상 opening balance](../../database/seed/R004__transaction.sql)를 넣는다. 최초 출금 계좌 1,000,000원과 입금 계좌 100,000원은 시작 잔액이며 완료된 이체 원장이 아니다. 이체 원장 합계를 0에서 시작한 현재 잔액으로 해석하면 안 된다. 수수료가 없는 이 모델의 이체 전후에는 두 계좌의 잔액 합계가 보존된다. 기존 seed 잔액·한도·version은 재실행으로 덮어쓰지 않는다.

## 요청 ID와 업무일이 중복 처리를 막는 방법

`request_id`는 호출자가 동일 업무의 재시도에 계속 사용하는 UUID다. 예약 INSERT는 PK와 `ON CONFLICT DO NOTHING`을 사용한다. 같은 ID를 동시에 넣으면 DB의 고유성 처리가 경쟁을 조정하고 후속 요청은 완료된 결과를 조회한다. 재시도 시 새 UUID를 만들면 별개의 이체로 취급된다. [PostgreSQL INSERT의 ON CONFLICT와 고유 인덱스 대기](https://www.postgresql.org/docs/17/sql-insert.html)

키가 같아도 출금 계좌·입금 계좌·금액이 다르면 `REQUEST_CONFLICT`로 거부한다. 동일 키를 서로 다른 요청에 재사용하면 안 된다. 원장의 `UNIQUE(request_id, account_id)`는 같은 거래가 같은 계좌에 중복 기록되는 것을 추가로 막는다. 이 제약만으로 두 계좌의 올바른 방향이나 금액을 보장하지는 않으므로 서비스와 테스트가 함께 확인한다.

`business_date`는 주입한 Clock을 Asia/Seoul 기준으로 해석해 결정하고 `daily_usage(account_id, business_date)`는 출금 누계를 저장한다. 같은 출금 계좌의 잠금을 잡은 상태에서 사용액을 읽고 증가시켜 경쟁 요청을 직렬화한다. 자정이 지난 뒤 같은 요청을 재시도해도 완료된 요청의 저장된 업무일과 결과를 반환해야 한다. 새 요청은 새 업무일을 사용한다.

응답 timeout은 서버 rollback의 증거가 아니다. 서버가 commit한 다음 응답만 유실될 수 있다. 이 경우 동일 키와 동일 payload로 결과를 조회하거나 재시도한다. 새로운 키로 다시 출금하면 이중 이체가 된다. 현재 IT는 commit 후 결과를 버린 호출자 상황을 모델링하며, 실제 HTTP 연결 차단 시험까지 수행하는 것은 아니다.

## 격리 수준과 잠금 선택

PostgreSQL의 기본 격리 수준은 READ COMMITTED다. 잠금 없는 SELECT는 문장 시작 시점의 커밋된 데이터를 읽고 같은 트랜잭션의 후속 SELECT는 다른 결과를 볼 수 있다. 따라서 잔액을 먼저 읽고 나중에 쓰는 것만으로 동시 출금을 막지 못한다. 더 높은 격리 수준을 선택해도 직렬화 실패·경쟁 처리와 재시도 정책을 이해해야 한다. [PostgreSQL 격리 수준](https://www.postgresql.org/docs/17/transaction-iso.html)

현재 repository는 두 계좌를 `ORDER BY id FOR NO KEY UPDATE`로 읽는다. 잔액만 갱신하고 UUID 키는 바꾸지 않으므로 이 잠금은 다른 잔액 갱신을 막으면서 FK 검사에서 사용하는 KEY SHARE와 호환된다. 요청 예약 INSERT가 계좌 FK를 먼저 확인한 뒤 계좌를 잠그는 순서에서 무조건 FOR UPDATE로 강화하면 공유 키 잠금의 승격 대기로 교착을 만들 수 있다. [PostgreSQL 행 잠금 호환표](https://www.postgresql.org/docs/17/explicit-locking.html#LOCKING-ROWS)

반대 방향 이체라도 계좌 잠금 순서를 항상 UUID 순서로 맞춘다. 이는 서로 A→B와 B→A를 다른 순서로 잡는 교착 가능성을 줄인다. 잠금 순서만으로 모든 교착이 없어지는 것은 아니므로 timeout·DB 오류를 실패로 처리하고 트랜잭션 전체를 rollback한다. 재시도를 도입할 때는 새 트랜잭션에서 같은 요청 ID를 사용하고 무제한 반복하지 않는다. [PostgreSQL 교착 처리](https://www.postgresql.org/docs/17/explicit-locking.html#LOCKING-DEADLOCKS)

`version` 컬럼은 현재 잔액 변경 때 증가하는 관찰용 카운터다. `UPDATE ... WHERE version = 이전값`과 영향받은 행 수 검사를 사용하지 않으므로 현재 코드를 낙관적 잠금 구현이라고 부르지 않는다. 현재 동시성 제어의 기준은 비관적 행 잠금이다. 낙관적 방식으로 바꾸려면 충돌 후 재조회·한도 재계산·재시도까지 함께 설계해야 한다.

## propagation이 원자성에 미치는 영향

REQUIRED는 이미 존재하는 호출자의 트랜잭션에 참여하거나 새 트랜잭션을 만든다. 참여한 내부 작업이 rollback-only로 표시되면 바깥 작업도 commit할 수 없다. 출금과 입금은 같은 물리 트랜잭션 안에 있어야 한다. [Spring REQUIRED](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)

REQUIRES_NEW는 별도의 물리 트랜잭션을 열어 독립적으로 commit/rollback한다. 입금을 이 방식으로 분리하면 바깥 출금이 실패해도 입금만 남을 수 있다. 독립 감사 기록처럼 별도 의미가 필요한 경우에만 의도적으로 검토하고, 추가 Connection 때문에 풀 고갈이 생길 수 있는 점도 확인한다. 서비스가 다른 스레드로 일을 넘기는 경우에도 기존 ThreadLocal 트랜잭션이 자동으로 따라가지 않는다. [Spring propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)

## 취소 요청과 외부 이체의 경계

현재 `requestCancellation`은 COMPLETED를 CANCEL_REQUESTED로 바꾸며 잔액·원장·일일 사용액은 변경하지 않는다. 같은 취소 요청을 반복하면 같은 상태를 반환한다. 화면이나 API에서 이 상태를 환급 완료로 표시하면 안 된다. 실제 역거래·일일 한도 복원·원거래 연결 정책은 별도의 업무 기능이다.

외부 기관에 전송한 이체는 우리 DB rollback만으로 외부 기관의 commit을 되돌릴 수 없다. 네트워크 timeout으로 성공 여부가 불명확하면 외부 거래 ID로 상태 조회와 대사를 수행해야 한다. 로컬 변경과 전송할 이벤트를 같은 DB 트랜잭션의 outbox에 기록하고 별도 전달 작업이 재시도하는 방식은 이중 쓰기의 누락을 줄인다. 전달은 중복될 수 있으므로 수신 측 멱등성과 식별자를 유지해야 한다. 현재 프로젝트는 outbox나 외부 송금을 구현하지 않는다. [AWS transactional outbox 설명](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)

이미 완료한 외부 작업을 취소해야 한다면 업무 규칙에 맞는 보상 거래와 진행 상태가 필요하다. 보상은 과거 DB 행을 덮어써 원래 모습으로 돌리는 것과 다르며 그 사이의 다른 거래를 보존해야 한다. 보상 자체도 실패하거나 재시도될 수 있으므로 원거래·보상거래의 연결, 멱등성, 대사와 수동 처리 경로를 설계한다. [Microsoft 보상 트랜잭션 패턴](https://learn.microsoft.com/en-us/azure/architecture/patterns/compensating-transaction)

## 실제 검증에서 확인할 일곱 상황

[TransferServiceIT](../../modules/04-banking-transaction/src/test/java/kr/banklearning/transaction/TransferServiceIT.java)는 JdbcTransferRepository와 실제 Spring TransactionInterceptor 프록시를 사용한다.

1. 출금 직후 예외: 두 잔액·version·요청 수·원장 수·일일 사용액/행 수가 모두 이전과 같다.
2. 같은 키의 동시 요청과 재실행: 성공 결과가 같고 원장 2건, 요청 1건, 출금 1회다.
3. 같은 계좌의 경쟁 출금: 한 번만 성공하고 잔액 비음수 및 일일 한도를 유지한다.
4. 출금 직후 실제 CHECK 제약 위반: DB 예외가 전파되고 모든 중간 변경이 rollback된다.
5. commit 후 응답 유실 모델: 같은 키 재시도 전후 잔액·version·원장 식별자·누계가 같다.
6. 일일 한도·다른 payload·취소 요청: 초과/충돌은 거부하고 취소 요청이 자금을 다시 이동하지 않는다.
7. 반대 방향 동시 이체: 동일 잠금 순서를 사용하며 두 거래의 자금 합계와 원장 쌍을 보존한다.

이 테스트는 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 필수로 요구한다. 각 테스트는 새로운 가상 UUID 고객·계좌를 commit해 준비하고 성공한 fixture/거래 행을 남긴다. 기존 seed를 조회 기준으로 삼거나 삭제·초기화하지 않는다. 로컬 테스트 DB에 결과가 누적되는 것은 의도된 동작이다. 실제 DB가 준비된 환경에서 opt-in 통합 프로필로 실행하고 최신 결과는 진행 기록에서 확인한다. 관찰 SQL은 [phase4.sql](../../database/queries/phase4.sql)에 있다.
