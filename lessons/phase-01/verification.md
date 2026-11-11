# Phase 1 — 실행과 검증

아래 명령은 프로젝트 루트 기준의 재현 절차입니다. 성공 기대값과 실제 실행 기록은 다릅니다. 로컬 코드·테스트와 Docker 실행의 최신 결과는 루트 `PROGRESS.md`에서 확인합니다.

## 정상 모듈 빌드와 테스트

JDK 17을 준비하고 Maven Wrapper로 실행합니다. Maven 전역 설치는 필요하지 않습니다. 최초 실행 시 인터넷 연결이 필요하며 배포본과 의존성은 프로젝트 `.cache`에 저장됩니다.

Windows PowerShell:

```powershell
.\mvnw.cmd -pl modules/01-java-core -am verify
```

Linux/macOS:

```sh
sh ./mvnw -pl modules/01-java-core -am verify
```

기대 결과: 고객과 계좌 소유 관계, 입출금, 금액 검증, 잔액 부족, 실패 후 상태 보존, 거래 기록, 조회 스냅샷에 대한 테스트가 성공합니다. 결과 보고서는 `modules/01-java-core/target/surefire-reports`에 생성됩니다.

## 정상 예제 실행

빌드 후 로컬에서 실행합니다.

```sh
java -cp modules/01-java-core/target/classes kr.banklearning.core.BankDemo
```

기대 출력의 주요 값은 다음과 같습니다. 아래는 실제 실행 로그가 아닌 예시입니다.

```text
학습 고객: 학습고객01
첫 번째 계좌 잔액: 65000원
두 번째 계좌 잔액: 5000원
첫 번째 계좌 거래 내역:
DEPOSIT 100000원 / 잔액 100000원
WITHDRAWAL 35000원 / 잔액 65000원
```

Docker 엔진이 준비되면 독립 프로필로 같은 예제를 실행합니다.

```sh
docker compose --profile phase1 run --build --rm java-core
```

이 단계는 DB를 사용하지 않습니다. Docker 명령을 실행하지 못했다면 로컬 Java 실행 성공으로 Docker 실행을 PASS 처리하지 않습니다.

## 미완성 실습 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase1 -pl exercises/starter/phase-01 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase1 -pl exercises/starter/phase-01 -am verify
```

TODO 상태에서는 `UnsupportedOperationException`으로 실패해야 합니다. 구현 뒤에는 일반 출금, 잔액과 동일한 금액, scale 차이, 0·음수·소수·null, 큰 금액의 정밀도, 입력과 반복 호출 보존에 대한 계약 테스트가 모두 통과해야 합니다.

기본 Wrapper `verify`의 성공과 실습 TODO의 완료는 별개입니다. 실습은 선택한 프로필에서 검증합니다. 정답 비교가 필요할 때만 별도 solutions 디렉터리의 안내를 확인합니다.

## 기록 기준

- 빌드와 Java 테스트는 실행 명령·종료 코드·테스트 수를 기록합니다.
- 예제 실행은 잔액뿐 아니라 거래 유형·금액·거래 후 잔액도 확인합니다.
- 미완성 starter의 예상 실패와 정상 모듈의 테스트 실패를 구분합니다.
- 로컬 메모리 테스트는 DB 트랜잭션이나 실제 Docker 환경 검증으로 집계하지 않습니다.
- 실행하지 못한 항목은 `NOT RUN`과 원인을 남깁니다.
