# Phase 6 실습 — 최대 반환 건수 정책

JSP/JSTL 화면 과제는 [별도 실습](jstl/exercise.md)을 진행합니다. 아래 본편 Java/SQL 과제와 독립적이며 [실행 안내](jstl/lesson.md)에서 시작합니다.

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

수정 파일은 [`PageSize.java`](../../exercises/starter/phase-06/src/main/java/kr/banklearning/exercise/PageSize.java)입니다. 정상 Boot API와 분리된 실습 모듈이며 기본 실행에 포함되지 않습니다.

## TODO 계약

`resolve(Integer supplied)`는 다음 정책을 구현합니다.

- null이면 기본 반환 건수 50을 반환합니다.
- 1~200이면 입력값 그대로 반환합니다.
- 0, 음수, 200 초과는 `IllegalArgumentException`으로 거절합니다.
- 예외 메시지에는 전달된 숫자를 그대로 포함하지 않습니다.
- 이전 호출에 따라 다음 호출의 기본값이 바뀌지 않아야 합니다.

| 입력 | 기대 결과 |
| --- | --- |
| null | 50 |
| 1 | 1 |
| 200 | 200 |
| 0, -1, 201 | 예외 |

유효하지 않은 값을 자동으로 1이나 200으로 바꾸지 않습니다. null과 0의 의미를 구분하고 Integer 범위 끝도 검증합니다. 이 정책은 조회 건수 제한이며 cursor·offset 기반 페이지 탐색 구현은 요구하지 않습니다.

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase6 -pl exercises/starter/phase-06 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase6 -pl exercises/starter/phase-06 -am verify
```

미완성 TODO는 실패가 기대됩니다. 구현 후 기본값·경계·범위 오류·입력 비노출·반복 요청 테스트를 통과시키세요. 정답과 해설은 solutions에만 둡니다.

## API 읽기 과제

1. `/api/accounts/{id}`가 principal에서 허용 고객 범위를 얻는 경로를 추적합니다.
2. 날짜 입력의 오류가 JSON 오류 envelope로 변환되는 곳을 찾습니다.
3. SQL 결과의 금액·시각이 DTO와 JSON에 전달되는 타입을 확인합니다.
4. GET 요청이 기존 거래·원장·일일 한도 상태를 바꾸지 않는지 검증할 항목을 적습니다.
5. 기본 조회 건수 제한과 DB 실행 계획·기간 제한의 책임을 구분합니다.
6. Mockito 검증과 실제 PostgreSQL 권한·결과 매핑 검증을 나누어 설계합니다.

API 필드와 HTTP 계약은 [api-contract.md](../../docs/architecture/api-contract.md), 힌트는 [hints.md](hints.md)를 확인합니다.
