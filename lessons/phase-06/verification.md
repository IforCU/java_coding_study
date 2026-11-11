# Phase 6 — 조회 API 검증

화면 과제의 렌더링·완성 계약은 [JSTL 검증](jstl/verification.md)을 따릅니다. 아래 본편 테스트와 분리해 기록합니다. Wrapper에는 JDK 17이 필요하며 Maven 전역 설치는 필요하지 않습니다.

프로젝트 루트에서 실행합니다. 아래는 재현 절차와 기대 결과이며 실제 PASS 로그가 아닙니다. 로컬 빌드·테스트와 Docker·DB 검증의 최신 결과는 `PROGRESS.md`에서 확인합니다.

## 로컬 빌드

Windows PowerShell:

```powershell
.\mvnw.cmd -pl modules/06-spring-boot -am verify
```

Linux/macOS:

```sh
sh ./mvnw -pl modules/06-spring-boot -am verify
```

기대 결과: 정상 API JAR가 생성되고 JUnit·Mockito·HTTP 계약 테스트가 통과합니다. 테스트 대역의 성공으로 PostgreSQL 권한과 조회 결과가 검증되었다고 판단하지 않습니다.

## Docker 실행 절차

```sh
docker compose --profile phase6 up --build -d
docker compose --profile phase6 ps -a
```

기본 주소는 `http://127.0.0.1:8086`이며 서비스 이름은 `spring-boot`입니다. 실행 JAR는 `spring-boot-api.jar`입니다. API DB 연결은 조회 전용 역할을 사용합니다.

Windows PowerShell에서는 `curl.exe`, Linux/macOS에서는 같은 인자의 `curl`을 사용합니다. 아래 계정은 공개된 로컬 가상 자격 증명입니다.

```powershell
curl.exe -i http://127.0.0.1:8086/health
curl.exe -i --user learner:learning-user-password --header "X-Request-ID: demo-phase6-01" http://127.0.0.1:8086/api/customers/40000000-0000-0000-0000-000000000001
curl.exe -i --user learner:learning-user-password http://127.0.0.1:8086/api/accounts/40000000-0000-0000-0000-000000000101
curl.exe -i --user learner:learning-user-password "http://127.0.0.1:8086/api/accounts/40000000-0000-0000-0000-000000000101/transactions?from=2026-10-01&to=2026-10-31&limit=50"
curl.exe -i --user learner:learning-user-password http://127.0.0.1:8086/api/accounts/40000000-0000-0000-0000-000000000101/limit
curl.exe -i --user admin:learning-admin-password "http://127.0.0.1:8086/api/admin/statistics?from=2026-10-01&to=2026-10-31"
```

기대 결과: 읽을 수 있는 자원은 200과 표준 envelope로 반환되며 requestId가 이어집니다. 거래가 없으면 목록은 비어 있을 수 있습니다. seed의 opening balance를 이체 원장으로 세지 않습니다.

## 오류와 권한

| 사례 | 기대 결과 |
| --- | --- |
| 자격 증명 없는 API 요청 | 401, 안전한 오류 envelope |
| USER의 관리자 통계 요청 | 403 |
| USER의 타 소유자 고객·계좌 | 403 |
| 허용 범위의 존재하지 않는 리소스 | 404 |
| 잘못된 UUID·날짜·조회 건수 | 400 |
| 통계 날짜 범위가 366일 초과 | 400 |
| DB 사용 불가 | 503, 내부 SQL·자격 증명 비노출 |

기존 자원이 없는 경우와 권한 밖의 요청은 사용하는 역할에 따라 검증합니다. 오류 코드와 requestId도 HTTP 상태와 함께 확인합니다. 구체 필드는 [API 계약](../../docs/architecture/api-contract.md)을 따릅니다.

## 실제 DB 통합 검증

```sh
docker compose --profile integration run --build --rm integration-test
```

실제 DB IT는 UUID·금액·시각 매핑, 기간 경계, 조회 역할의 쓰기 거절과 기존 업무 데이터 보존을 확인합니다. 학습 DB 환경변수를 준비한 호스트에서는 `.\mvnw.cmd -pl modules/06-spring-boot -am -Pintegration-tests verify`로 선택합니다. IT의 `DB_URL`, `DB_USER`, `DB_PASSWORD`는 자기 fixture를 준비하는 소유자 연결이고 `API_DB_USER`, `API_DB_PASSWORD`는 조회 역할 연결입니다. 정상 API 실행의 DB 연결은 조회 역할만 사용합니다. 소유 범위와 HTTP 요청 분기는 별도 서비스·HTTP 테스트와 함께 확인합니다.

로컬 코드 테스트와 실제 권한·DB·내장 서버 검증을 나누고 DB가 없으면 `NOT RUN`을 남깁니다. 데이터 삭제나 볼륨 초기화로 검증하지 않습니다.

## 선택 건수 정책 실습

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase6 -pl exercises/starter/phase-06 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase6 -pl exercises/starter/phase-06 -am verify
```

TODO 상태에서는 실패해야 합니다. 완성 후 기본값·범위·반복 호출 계약이 통과해야 합니다. 기본 빌드와 API JAR는 이 TODO와 정답을 포함하지 않습니다.

## 종료

```sh
docker compose --profile phase6 down
```

볼륨을 유지합니다. 문서의 명령으로 실제 재현하지 못한 항목은 완료로 표시하지 않습니다.
