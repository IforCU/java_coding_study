# Phase 4 — 이체의 원자성과 재요청 처리

정상 모듈은 `modules/04-banking-transaction`입니다. 두 계좌 사이의 이체를 하나의 DB 트랜잭션으로 처리하며, 잔액·요청 상태·원장·일일 사용액을 함께 관리합니다. 실제 은행 연동 없이 로컬의 가상 원화 계좌를 사용합니다.

## 이체가 보존할 것

출금액만 줄고 입금되지 않거나, 잔액은 맞아도 원장이 빠지면 성공한 이체가 아닙니다. 성공 시 출금·입금 원장 두 건과 완료 요청 한 건이 남고 일일 사용액이 증가해야 합니다. 실패하면 이 변화가 모두 취소되어야 합니다.

| ACID | 이 모듈에서 관찰할 의미 |
| --- | --- |
| Atomicity | 출금·입금·원장·한도·요청 상태가 함께 commit 또는 rollback |
| Consistency | 음수 잔액, 잘못된 금액, 같은 계좌 이체 등 업무·DB 규칙 유지 |
| Isolation | 동시 요청이 같은 잔액을 따로 소비하지 않도록 제어 |
| Durability | commit된 결과를 DB 영속 저장에서 다시 조회 |

Annotation과 코드만으로 이 성질이 검증된 것은 아닙니다. 실제 DB 장애·동시성 테스트를 실행해야 합니다.

## 트랜잭션과 전파

`TransferService.transfer`는 `@Transactional` 경계이며 JdbcTemplate이 같은 DataSource 연결을 사용합니다. 기본 전파 `REQUIRED`는 이미 있는 트랜잭션에 참여하거나 새 트랜잭션을 시작합니다. RuntimeException이 경계 밖으로 전파되면 기본 rollback 대상입니다. 향후 checked exception을 추가하면 rollback 규칙도 다시 확인해야 합니다.

`REQUIRES_NEW`는 별도 물리 트랜잭션을 사용합니다. 이 단일 DB 이체의 원장을 독립 commit하면 계좌 rollback과 어긋날 수 있으므로 하나의 업무 경계를 유지합니다. [Spring 트랜잭션 전파](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)

## 격리 수준과 잠금

로컬 PostgreSQL 기본 격리 수준은 READ COMMITTED입니다. 일반 조회는 명령 시작 시점의 커밋 데이터를 보며 같은 트랜잭션 안에서도 다음 조회 결과가 달라질 수 있습니다. REPEATABLE READ는 더 넓은 스냅샷을 유지하고 SERIALIZABLE은 직렬 실행에 맞지 않는 충돌을 실패시켜 재시도를 요구할 수 있습니다. PostgreSQL의 READ UNCOMMITTED는 READ COMMITTED처럼 동작합니다. [PostgreSQL 격리 수준](https://www.postgresql.org/docs/17/transaction-iso.html)

현재 구현은 두 계좌를 UUID 순서로 조회하고 `FOR NO KEY UPDATE`로 잠급니다. 잔액을 확인한 뒤 다른 요청이 같은 계좌를 동시에 갱신하는 것을 제어하는 비관적 잠금입니다. 키를 바꾸지 않는 갱신이므로 요청 예약의 외래 키 검사에서 사용하는 KEY SHARE와 양립할 수 있는 모드를 사용합니다. 모든 관련 코드에서 잠금 순서를 일관되게 정해야 교착 위험을 줄일 수 있습니다. [PostgreSQL 행 잠금과 deadlock](https://www.postgresql.org/docs/17/explicit-locking.html)

낙관적 잠금은 조회한 version과 갱신 시 version을 비교해 충돌을 발견하는 방식입니다. 현재 계좌의 version은 증가하지만 `WHERE version = 기대값` 방식으로 충돌을 검사하는 낙관적 잠금 구현은 아닙니다. 선택한 제어 방식과 단순한 버전 기록을 구분합니다.

## 멱등성과 응답 유실

requestId는 같은 이체를 식별하는 UUID입니다. 동일 ID와 동일 계좌·금액을 다시 제출하면 이미 처리한 결과를 반환하고 다시 출금하지 않습니다. 같은 ID에 다른 내용을 넣으면 409로 거절합니다. 성공 응답이 유실되어도 기존 ID로 결과를 조회하거나 동일 내용을 재요청해야 합니다. 새 ID를 만들면 별도의 거래가 됩니다.

요청 예약도 이체와 같은 트랜잭션에 있습니다. 실패 시 예약까지 rollback되므로 실패를 영구 저장한 상태로 취급하지 않습니다. 완료한 요청의 상태는 `COMPLETED`이며, 취소 요청은 `CANCEL_REQUESTED`로 접수합니다. 취소 요청 접수는 환불이나 역이체를 실행하지 않습니다.

일일 한도는 출금 계좌와 Asia/Seoul 업무 날짜별로 계산합니다. 클라이언트가 전달한 날짜로 한도를 바꾸지 않습니다. 외부 기관 거래와 재시도·대사·보상 처리는 [트랜잭션 경계 문서](../../docs/banking-domain/transaction-boundaries.md)를 참고합니다.

[architecture.md](architecture.md)에서 실제 호출 경로를, [exercise.md](exercise.md)에서 잠금 순서 정책 과제를 확인합니다.
