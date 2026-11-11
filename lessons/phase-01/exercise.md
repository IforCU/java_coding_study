# Phase 1 실습 — 출금 가능 정책

정상 예제를 먼저 실행하고 계좌와 금액이 흐르는 경로를 읽습니다. 이번 직접 구현 과제는 상태를 변경하지 않고 출금 가능 여부만 판단하는 작은 정책입니다.

## 수정할 파일

[`WithdrawalPolicy.java`](../../exercises/starter/phase-01/src/main/java/kr/banklearning/exercise/WithdrawalPolicy.java)의 `canWithdraw(BigDecimal balance, BigDecimal amount)` TODO를 구현합니다. 계약 테스트는 같은 실습 모듈의 `src/test/java`에 있습니다.

정상 모듈인 `modules/01-java-core`를 수정하지 않습니다. 정답은 별도 solutions 디렉터리에 있고 이 문제에는 구현을 포함하지 않습니다.

## 정책 계약

메서드는 다음 조건이 모두 만족될 때만 `true`를 반환해야 합니다.

- 잔액과 출금액이 모두 null이 아닙니다.
- 잔액은 0 이상의 원 단위 정수입니다.
- 출금액은 0보다 큰 원 단위 정수입니다.
- 잔액은 출금액 이상입니다. 잔액 전부를 출금하는 경우도 허용합니다.

조건을 만족하지 않으면 예외 대신 `false`를 반환합니다. 입력을 바꾸거나 출금액을 차감하지 않습니다. 같은 입력으로 여러 번 호출해도 같은 결과를 반환해야 합니다. `BigDecimal`을 `double`, `float`, `long`으로 바꾸어 정밀도를 잃지 않도록 합니다.

| 잔액 | 출금액 | 기대 결과 | 확인하는 규칙 |
| --- | --- | --- | --- |
| `1000` | `400` | `true` | 일반 출금 |
| `1000` | `1000.00` | `true` | 수치가 같고 표현 scale만 다른 경우 |
| `1000` | `1001` | `false` | 잔액 부족 |
| `1000` | `0` | `false` | 0원 출금 금지 |
| `1000` | `100.1` | `false` | 원 미만 금액 거절 |
| `1000.1` | `100` | `false` | 잔액도 원 단위여야 함 |
| null | `1` | `false` | 잘못된 입력 |

표기상 소수점이 있다는 이유만으로 거절하지 않습니다. `100.00`은 원 단위 정수 값이고 `1E+3`도 1000입니다. 반올림해 정책을 통과시키면 안 됩니다.

## 실행과 검증

프로젝트 루트에서 명시적으로 실습 프로필을 선택합니다.

Windows PowerShell (JDK 준비 후 Maven 별도 설치 없이 실행):

```powershell
.\mvnw.cmd -Pexercise-phase1 -pl exercises/starter/phase-01 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase1 -pl exercises/starter/phase-01 -am verify
```

아직 TODO를 구현하지 않은 상태는 테스트가 실패하는 것이 기대 결과입니다. 이 실패는 정상 실행 모듈의 장애와 구분합니다. 구현 후 계약 테스트를 통과시키고 실패 원인과 수정 이유를 자신의 말로 기록하세요. 테스트를 삭제하거나 기대값을 바꿔 통과시키지 않습니다.

추가 점검 문제:

1. `100`과 `100.00`의 수치 비교와 객체 동등성은 어떤 차이가 있나요?
2. 매우 큰 정수 잔액과 1원 차이의 출금액도 정확하게 비교하나요?
3. 이 메서드가 `true`를 반환하는 것과 실제 계좌 출금이 성공하는 것은 어떤 차이가 있나요?
4. 기본 빌드에 미완성 실습을 넣으면 정상 예제 학습에 어떤 영향을 주나요?

막히면 [hints.md](hints.md)를 순서대로 확인합니다. 실행 환경과 실제 검증 기록은 [verification.md](verification.md)를 확인합니다.
