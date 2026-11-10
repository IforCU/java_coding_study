# Phase 0 정답과 해설

이 디렉터리는 과제를 직접 시도한 뒤 비교할 때 엽니다. 정답 SQL과 이 해설은 기본 실행 화면이나 문제 문서에 포함하지 않으며, 기본 빌드와 마이그레이션에서도 사용하지 않습니다.

## SQL 해설

`read-metadata.sql`은 `bootstrap.app_metadata` 테이블에서 `environment`, `schema_version` 두 열을 순서대로 읽습니다. `WHERE`는 `local-learning` 환경의 행만 선택합니다. 초기 데이터의 환경 행이 유일하므로 한 행을 반환합니다.

`SELECT`에 값을 직접 적는 방식으로 기대 결과를 흉내 내지 않습니다. 실제 테이블 값을 읽어야 메타데이터 변경이나 초기화 누락을 발견할 수 있습니다. `SELECT *` 대신 필요한 열을 지정하면 결과 순서와 열 수가 명확해집니다. 문자열 조건은 작은따옴표로 표현하며 `bootstrap.app_metadata`의 점은 스키마와 테이블의 경계를 뜻합니다.

정답 SQL의 psql 비정렬 출력 기대값은 다음과 같습니다. 이 예시는 실행 결과 기록이 아닙니다.

```text
local-learning|1
```

## 선택 검증

DB와 Phase 0 환경을 실행한 후 프로젝트 루트에서 정답 파일을 명시해 검증합니다.

```powershell
.\scripts\verify-exercise.ps1 -SqlFile exercises/solutions/phase-00/read-metadata.sql
```

```sh
sh scripts/verify-exercise.sh --sql-file exercises/solutions/phase-00/read-metadata.sql
```

학습자가 작성한 쿼리와 비교할 때는 열 순서, 스키마 명시, 환경 조건, 실제 테이블 조회 여부를 확인합니다. 검증기는 결과 형식을 확인하므로 상수 반환 여부까지 코드 리뷰해야 합니다.
