# Phase 3 실습 — 선택 기간 검색 Mapper

JSP/JSTL 화면 과제는 [별도 실습](jstl/exercise.md)을 진행합니다. 아래 본편 Java/SQL 과제와 독립적이며 [실행 안내](jstl/lesson.md)에서 시작합니다.

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

수정할 파일은 [`HistoryMapper.xml`](../../exercises/starter/phase-03/src/main/resources/exercise/HistoryMapper.xml)입니다. 정상 Spring MVC WAR와 분리된 실습 리소스입니다. 초기 XML은 읽을 수 있지만 필터가 빠져 있어 계약 테스트가 실패해야 합니다.

## 입력과 결과 계약

`kr.banklearning.exercise.HistoryMapper.search(Map<String, Object>)`는 `phase3.transaction_history`의 ID 목록을 `List<Map<String, Object>>`로 반환합니다. 이번 실습에서는 반환 결과를 실제 DB에서 읽기 전에 SQL 생성과 바인딩을 검증합니다.

| 입력 키 | 타입 | 조건 |
| --- | --- | --- |
| `accountId` | UUID 형태의 String | 필수, account_id 조건에 항상 사용 |
| `from` | OffsetDateTime | 선택, occurred_at 시작 포함 |
| `to` | OffsetDateTime | 선택, occurred_at 끝 제외 |

다음 조건을 지키도록 TODO를 작성하세요.

- 계좌 조건은 날짜 조건의 유무와 관계없이 존재합니다.
- 날짜가 빠졌거나 null이면 해당 날짜 조건과 바인딩을 생략합니다.
- 시작은 포함하고 종료 시각은 제외합니다.
- 바인딩 순서는 `accountId`, 제공된 `from`, 제공된 `to`입니다.
- 값은 `#{}`로 바인딩하고 `${}` 문자열 치환을 사용하지 않습니다.
- namespace, `search` ID와 결과 타입 계약을 유지합니다.
- UUID 문자열과 PostgreSQL uuid 열의 비교 타입을 맞춥니다.

이 Mapper의 `to`는 이미 끝 제외 시각입니다. 화면의 종료일을 다음 날 0시로 바꾸는 책임은 정상 서비스 계층에 있으므로 Mapper에서 날짜를 다시 더하지 않습니다.

## 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase3 -pl exercises/starter/phase-03 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase3 -pl exercises/starter/phase-03 -am verify
```

계약 테스트는 계좌만, 시작만, 종료만, 양쪽 날짜가 있는 경우를 비교합니다. BoundSql의 조건·물음표·parameter mapping과 PreparedStatement 대역에 전달된 인덱스·값을 확인합니다. SQL처럼 보이는 입력도 SQL 구조로 삽입되지 않아야 합니다.

테스트는 DB 연결을 열지 않으며 SQL 실행 결과나 실제 인덱스 사용을 증명하지 않습니다. 입력 UUID의 유효성 검증은 정상 서비스에서 별도로 수행합니다. 검증을 통과한 뒤 실제 DB 환경이 준비되면 조회 결과와 경계 시각을 추가 확인합니다.

## 소스 추적 과제

1. XML의 Bean이 서비스 생성자로 어떻게 주입되는지 연결합니다.
2. 고객을 SUSPENDED로 바꾼 뒤 계좌 개설이 거절되는 경로를 표시합니다.
3. 화면의 날짜 두 개가 Mapper의 시작 포함·끝 제외 시각으로 바뀌는 곳을 찾습니다.
4. `resultMap`의 열 이름·타입·생성자 인자 순서가 어긋나면 어떤 검증이 필요한지 설명합니다.

완성 XML과 해설은 solutions에만 둡니다. 힌트는 [hints.md](hints.md)를 확인합니다.
