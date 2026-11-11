# Phase 3 정답과 해설

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

정답 모듈은 기본 WAR와 기본 Maven 빌드에서 제외됩니다. 과제를 시도한 뒤 명시적으로 선택합니다.

## 동적 SQL의 구성

계좌 조건은 항상 있어야 하므로 `if` 안에 넣지 않습니다. `accountId`는 UUID 문자열이므로 PostgreSQL의 uuid 열과 비교할 때 `CAST(#{accountId} AS uuid)`를 사용합니다. 문자열 자체는 SQL에 붙이지 않고 PreparedStatement의 값으로 바인딩합니다.

`from`과 `to`가 null이 아닐 때만 시각 조건을 추가합니다. 시작은 `>=`, 끝은 `<`이므로 구간은 `[from, to)`입니다. XML에서 `<`는 `&lt;`로 표현합니다. 같은 시각의 경계 거래가 인접 기간에 두 번 포함되지 않도록 끝을 제외합니다.

바인딩 순서는 계좌, 시작 시각, 끝 시각입니다. 각 날짜가 빠지면 해당 조건과 바인딩도 함께 빠집니다. `#{}`는 값을 바인딩하고 `${}`는 SQL 텍스트를 치환하므로 이 검색 값에 문자열 치환을 쓰지 않습니다.

## 계약 테스트의 범위

테스트는 XMLMapperBuilder로 XML을 읽어 BoundSql과 parameter mapping을 만들고, DefaultParameterHandler가 PreparedStatement 대역에 전달한 실제 순서와 값을 확인합니다. DB 연결은 열지 않습니다.

SQL처럼 보이는 문자열 테스트는 값이 SQL 구조로 삽입되지 않는지 확인합니다. 정상 웹 서비스는 UUID 형식을 먼저 검증하므로 이 문자열이 실제 DB 조회에서 유효한 UUID라는 뜻은 아닙니다. PostgreSQL 문법·형 변환·조회 결과 검증은 실제 DB 통합 테스트로 별도 수행해야 합니다.

## 선택 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -Psolution-phase3 -pl exercises/solutions/phase-03 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Psolution-phase3 -pl exercises/solutions/phase-03 -am verify
```

정답도 starter의 동일 계약 테스트를 사용합니다. 테스트 결과와 DB 통합 결과를 혼합하지 말고 실제 실행 기록은 루트 `PROGRESS.md`에서 확인합니다.
