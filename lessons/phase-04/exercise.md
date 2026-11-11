# Phase 4 실습 — 계좌 잠금 순서

JSP/JSTL 화면 과제는 [별도 실습](jstl/exercise.md)을 진행합니다. 아래 본편 Java/SQL 과제와 독립적이며 [실행 안내](jstl/lesson.md)에서 시작합니다.

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

서로 반대 방향의 이체가 각각 자기 출금 계좌부터 잠그면 상대 계좌를 기다리며 교착할 수 있습니다. 이번 실습은 두 계좌를 항상 같은 순서로 돌려주는 작은 정책을 작성합니다. 실제 DB 잠금은 획득하지 않습니다.

## 수정할 파일

[`LockOrder.java`](../../exercises/starter/phase-04/src/main/java/kr/banklearning/exercise/LockOrder.java)의 `order(UUID first, UUID second)` TODO입니다. 정상 거래 모듈은 이 실습을 호출하지 않으며 기본 빌드에 포함하지 않습니다.

## 계약

- 서로 다른 두 UUID를 받아 정확히 두 UUID의 목록을 반환합니다.
- 순서는 PostgreSQL uuid의 오름차순과 같습니다. 16바이트를 앞에서부터 unsigned 값으로 비교하는 순서입니다.
- 입력 순서를 바꾸어도 같은 결과를 반환합니다.
- 입력 UUID의 값을 바꾸지 않습니다.
- 반환 목록을 추가·교체·삭제할 수 없어야 합니다.
- null이 있거나 두 UUID 값이 같으면 `IllegalArgumentException`을 던집니다.

`7fff...`와 `8000...` 경계와 상위 64비트가 같고 하위 64비트만 다른 경우도 검증합니다. Java의 자연 정렬이 DB 정렬과 항상 같은지 확인하지 않고 가정하지 않습니다.

## 선택 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase4 -pl exercises/starter/phase-04 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase4 -pl exercises/starter/phase-04 -am verify
```

초기 TODO는 실패해야 합니다. 구현 후 고정 사례, 부호 경계, 재현 가능한 256개 UUID 쌍과 반대 입력, null·중복, 목록 불변성 검증이 통과해야 합니다. 테스트의 비교 기준을 바꾸어 문제를 피해 가지 않습니다.

## 이체 소스 추적 과제

1. 출금 후 입금 전에 오류가 발생하면 rollback되어야 할 테이블을 모두 적습니다.
2. 동일 requestId와 같은 내용의 재요청, 다른 내용의 재요청을 구분해 호출 경로를 표시합니다.
3. 동시에 같은 계좌에서 출금하는 두 요청이 잔액과 일일 한도를 읽는 순서를 그립니다.
4. 성공 응답을 받지 못했을 때 새 requestId를 만드는 경우와 기존 ID를 재사용하는 경우를 비교합니다.
5. 취소 요청 접수 이후 잔액·원장·일일 사용액이 어떻게 유지되어야 하는지 설명합니다.

DB 장애 시나리오는 [verification.md](verification.md)를 확인합니다. 완성 정책과 해설은 solutions에만 두며 힌트는 [hints.md](hints.md)에 있습니다.
