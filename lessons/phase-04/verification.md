# Phase 4 — 이체 검증

화면 과제의 렌더링·완성 계약은 [JSTL 검증](jstl/verification.md)을 따릅니다. 아래 본편 테스트와 분리해 기록합니다. Wrapper에는 JDK 17이 필요하며 Maven 전역 설치는 필요하지 않습니다.

명령은 프로젝트 루트 기준입니다. 아래는 재현 절차와 기대 결과이며 실제 PASS 로그가 아닙니다. 로컬 테스트와 실제 PostgreSQL·Docker 통합 검증의 최신 결과는 `PROGRESS.md`를 확인합니다.

## 로컬 빌드와 단위 테스트

Windows PowerShell:

```powershell
.\mvnw.cmd -pl modules/04-banking-transaction -am verify
```

Linux/macOS:

```sh
sh ./mvnw -pl modules/04-banking-transaction -am verify
```

기대 결과: 정상 모듈이 빌드되고 `target/banking-transaction.war`가 생성됩니다. 서비스 대역 테스트는 입력, 업무 분기, 호출 순서와 결과를 확인할 수 있지만 실제 DB rollback·행 잠금·동시성을 증명하지 않습니다.

## Docker 실행 절차

```sh
docker compose --profile phase4 up --build -d
docker compose --profile phase4 ps -a
```

기본 주소는 `http://127.0.0.1:8084`입니다. `.env`의 `TRANSACTION_PORT`를 변경한 경우 해당 포트로 접속하며 현재 실행 주소는 [README](../../README.md)를 확인합니다. `banking-transaction` 서비스와 `phase4` 전용 스키마를 사용합니다.

브라우저에서 정상 흐름을 확인합니다.

1. `/health`에서 준비 상태를 확인합니다.
2. `/`에서 두 가상 계좌와 현재 잔액·한도를 확인합니다.
3. 새 requestId로 10,000원을 이체하고 303 후 영수증을 확인합니다.
4. 출금·입금 잔액 합계가 유지되고 원장 두 건, 완료 요청 한 건, 일일 사용액 증가가 같은 거래를 가리키는지 확인합니다.
5. 같은 requestId와 같은 내용으로 다시 요청해 추가 출금이 없는지 확인합니다.
6. 같은 requestId에 다른 금액·계좌를 제출해 409인지 확인합니다.
7. 영수증에서 취소 요청을 접수해 CANCEL_REQUESTED가 되고 잔액·원장·일일 사용액은 유지되는지 확인합니다.

초기 seed는 두 계좌에 1,000,000원과 100,000원의 opening balance를 제공합니다. 재실행 시 기존 실습 잔액을 덮어쓰지 않습니다. 초기 원장이나 완료 이체가 이미 있었다고 해석하지 않습니다.

## 실제 DB에서 재현할 다섯 장애

모든 시나리오는 격리된 테스트 데이터와 테스트용 FailureHook을 사용합니다. 기본 폼에 실패 실행 기능을 추가하지 않습니다. DB 실행 전에는 아래 항목을 `NOT RUN`으로 기록합니다.

| 시나리오 | 기대 검증 |
| --- | --- |
| 출금 갱신 직후 입금 전 오류 | 양쪽 잔액·요청 예약·원장·일일 사용액 모두 이전 상태 |
| 동일 요청의 중복·동시 제출 | 동일 결과 한 건, 원장 두 건, 출금·한도 사용 한 번 |
| 같은 출금 계좌의 동시 요청 | 잔액과 한도를 넘지 않으며 성공 건수와 원장·사용액 일치 |
| DB 작업 중 예외 | 일부 commit 없이 관련 변경 전체 rollback |
| commit 후 응답 유실 | 동일 ID로 결과 조회·재요청 시 추가 거래 없음 |

실제 DB 통합 테스트는 `TransferServiceIT`에 두고 Failsafe의 선택 프로필에서만 실행합니다.

```sh
docker compose --profile integration run --build --rm integration-test
```

호스트에서 이미 준비한 학습 DB를 사용하는 경우에는 `DB_URL`, `DB_USER`, `DB_PASSWORD`를 해당 학습 DB로 설정한 뒤 실행합니다. 실제 운영 연결 정보는 사용하지 않습니다.

Windows PowerShell:

```powershell
.\mvnw.cmd -pl modules/04-banking-transaction -am -Pintegration-tests verify
```

Linux/macOS:

```sh
sh ./mvnw -pl modules/04-banking-transaction -am -Pintegration-tests verify
```

기본 단위 테스트 명령은 `*IT.java`를 실행하지 않습니다. 통합 프로필을 선택하고도 DB 환경변수가 없으면 실패해야 하며 자동 생략을 PASS로 기록하지 않습니다. 위 두 실행 경로의 실제 DB 결과는 `PROGRESS.md`에 별도로 기록합니다.

응답 유실은 commit된 상태에서 클라이언트가 응답을 못 받은 상황으로 재현합니다. commit 전 예외를 주입한 rollback 시나리오와 구분합니다. 실제 테스트 실행 지침과 단일 DB·외부 기관 경계는 [트랜잭션 경계 문서](../../docs/banking-domain/transaction-boundaries.md)를 확인합니다.

추가로 반대 방향 이체의 잠금 순서, 일일 한도 경계, Asia/Seoul 자정 경계, 취소 재요청, 잔액·입금 금액 상한을 확인합니다. 테스트를 위해 사용자의 학습 데이터를 삭제하지 않습니다.

## 선택 잠금 정책 실습

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase4 -pl exercises/starter/phase-04 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase4 -pl exercises/starter/phase-04 -am verify
```

TODO 상태에서는 실패해야 합니다. 완성 후 같은 UUID 쌍의 입력 방향과 부호 경계, 불변성·잘못된 입력 테스트가 통과해야 합니다. 정답 비교는 solutions 안내에서 선택합니다.

## 종료와 결과 기록

```sh
docker compose --profile phase4 down
```

볼륨을 유지합니다. 로컬 빌드·단위 테스트, 실제 DB 장애 다섯 종류, 동시성·잠금·영속성, HTTP·JSP를 따로 기록합니다. 실제 DB에서 실행하지 않은 검증은 PASS로 집계하지 않습니다.
