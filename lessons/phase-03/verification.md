# Phase 3 — 실행과 검증

화면 과제의 렌더링·완성 계약은 [JSTL 검증](jstl/verification.md)을 따릅니다. 아래 본편 테스트와 분리해 기록합니다. Wrapper에는 JDK 17이 필요하며 Maven 전역 설치는 필요하지 않습니다.

프로젝트 루트에서 실행합니다. 아래는 재현 절차와 기대 결과입니다. 로컬 테스트와 Docker·실제 DB·Tomcat JSP 검증의 최신 결과는 `PROGRESS.md`를 확인하며 실행하지 않은 항목은 `NOT RUN`으로 기록합니다.

## 로컬 빌드와 단위 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -pl modules/03-spring-mvc -am verify
```

Linux/macOS:

```sh
sh ./mvnw -pl modules/03-spring-mvc -am verify
```

기대 결과: 정상 모듈의 설정·서비스·Mapper·HTTP 단위 검증이 통과하고 `modules/03-spring-mvc/target/spring-mvc.war`가 생성됩니다. 단위 검증의 실제 범위와 테스트 수는 진행 기록을 확인합니다. 대역으로 생성한 서비스는 실제 DB commit/rollback을 검증하지 않습니다.

## Docker 검증 절차

```sh
docker compose --profile phase3 up --build -d
docker compose --profile phase3 ps -a
```

기본 주소는 `http://127.0.0.1:8083`입니다. Tomcat 11의 독립 WAR와 `phase3` 스키마를 사용합니다. Phase 2의 포트·스키마·WAR를 바꾸지 않습니다.

브라우저 검증 순서:

1. `/health`에서 HTTP 200과 준비 상태 JSON을 확인합니다.
2. `/customers`에서 가상 고객 등록과 POST→303→GET을 확인합니다.
3. ACTIVE 고객에게 계좌를 개설하고 잔액 0원을 확인합니다.
4. 고객을 SUSPENDED로 바꾸고 새 계좌 등록이 409로 거절되며 계좌 수가 늘지 않는지 확인합니다.
5. 다시 ACTIVE로 바꾸고 새 계좌 등록이 성공하는지 확인합니다.
6. 기존 거래 계좌의 `/history`에서 계좌만·시작일만·종료일만·양쪽 날짜를 각각 조회합니다.
7. 시작일이 종료일보다 늦은 경우와 날짜 형식 오류가 400으로 거절되는지 확인합니다.

날짜의 기대 구간은 Asia/Seoul 기준 시작일 0시 이상, 종료일 다음 날 0시 미만입니다. 같은 날 조회와 종료일 경계의 거래가 정확히 포함되는지 실제 DB 데이터로 확인합니다. CSRF 누락·불일치는 403이며 저장소 내부 오류와 자격 증명은 응답에 포함되지 않아야 합니다.

## 선택 Mapper 실습

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase3 -pl exercises/starter/phase-03 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase3 -pl exercises/starter/phase-03 -am verify
```

초기 starter는 XML 파싱에 성공하되 필수 필터·바인딩 계약에서 실패하는 것이 기대됩니다. 작성 후 네 가지 검색 조합, 명시적 null, 값 바인딩과 문자열 치환 금지 검증이 통과해야 합니다.

이 검증은 실제 PostgreSQL을 사용하지 않습니다. SQL 문자열 생성과 JDBC 바인딩 성공을 실제 조회 결과·형 변환·인덱스 사용 성공으로 집계하지 않습니다. 정답 비교가 필요하면 별도 solutions 안내를 확인합니다.

## 종료와 결과 기록

```sh
docker compose --profile phase3 down
```

볼륨은 유지합니다. 로컬 빌드·단위 테스트, 실제 DB 결과 매핑·기간 경계·상태 제한·트랜잭션, Tomcat/JSP 실행을 나누어 기록합니다. 실행하지 못한 항목은 `NOT RUN`으로 남깁니다.
