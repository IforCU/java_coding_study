# 은행권 Java 레거시 개발 학습 프로젝트

Java 요청이 JSP·Servlet·Service·SQL을 거치는 과정을 직접 추적하고 수정하는 로컬 은행 업무 학습 환경입니다. 메모리 은행에서 Spring MVC/MyBatis, 이체 트랜잭션, 보안·장애 분석, 별도 Spring Boot 조회 API까지 다룹니다. 모든 고객과 인증정보는 학습용 가상 데이터입니다.

2026-11-11 로컬 점검에서는 Java/Maven 부재와 Docker 엔진 접근 거절로 재검증을 대기 중입니다. 아래 PASS는 2026-10-25의 기록이며 `artifacts` 로그는 현재 작업 폴더에 없습니다. 최신 상태는 [PROGRESS.md](PROGRESS.md), 브랜치 운영과 IDE·실행 환경 확인법은 [개발 가이드](docs/development-workflow.md)를 확인하세요.

Phase 0~6의 코드·교재·실습과 실행 검증을 완료했습니다. 기본 테스트 171개와 정답 계약 테스트 79개가 통과했습니다.

Docker 전체 이미지 빌드·웹 앱 6개 준비 상태·실제 PostgreSQL 통합 테스트 18개와 Phase 2~6 HTTP/JSP·폼 시나리오 5개를 통과했습니다. 마이그레이션/seed 재실행, DB·앱 재시작, 일반 종료(`down`) 후 재실행(`up`)의 데이터 보존도 확인했습니다.

검증 범위와 결과는 [PROGRESS.md](PROGRESS.md), 실제 실행 근거는 [전체 Docker 검증 로그](artifacts/docker-all-final.log), [웹 시나리오 결과](artifacts/docker-web-smoke.json), [종료·재실행 보존 결과](artifacts/docker-lifecycle.json)에서 확인합니다.

## 준비

Windows 11에서는 Docker Desktop의 Linux 엔진을 실행합니다. Linux/macOS는 Docker Engine과 Compose v2가 필요합니다. Java와 Maven은 빌드 컨테이너가 제공하므로 호스트 전역 설치는 필수가 아닙니다.

프로젝트 루트에서 기존 `.env`를 보존하고 예시를 복사합니다.

```powershell
if (-not (Test-Path -LiteralPath .env)) { Copy-Item .env.example .env }
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/doctor.ps1
```

```sh
[ -f .env ] || cp .env.example .env
sh scripts/doctor.sh
```

모든 공개 포트는 `127.0.0.1`에만 바인딩됩니다. 포트는 `.env`에서 바꿀 수 있습니다. PowerShell 실행 정책 옵션은 해당 프로세스에만 적용하며 PC 설정을 바꾸지 않습니다.

## 단계별 실행과 학습

각 웹 단계는 아래 프로필로 독립 실행합니다. `db`가 healthy가 되면 `migrate`가 버전 SQL·seed를 적용한 뒤 종료하고 앱을 시작합니다. `migrate`의 `Exited (0)`은 정상 상태입니다.

| Phase | 서비스 / 프로필 | 현재 접속주소·역할 | 교재 |
| --- | --- | --- | --- |
| 0 | `legacy` / `phase0` | [8080](http://127.0.0.1:8080/), Hello World·JDBC 준비 상태 | [시작](lessons/phase-00/lesson.md) |
| 1 | `java-core` / `phase1` | CLI, 고객·다중 계좌·입출금·이력 | [Java·금액](lessons/phase-01/lesson.md) |
| 2 | `servlet-jsp` / `phase2` | [8082](http://127.0.0.1:8082/customers), Servlet/JSP/JDBC·Form·Session | [웹 구조](lessons/phase-02/lesson.md) |
| 3 | `spring-mvc` / `phase3` | [8083](http://127.0.0.1:8083/customers), Spring MVC/MyBatis·상태·기간 검색 | [계층과 SQL](lessons/phase-03/lesson.md) |
| 4 | `banking-transaction` / `phase4` | [18084](http://127.0.0.1:18084/), 이체·멱등성·원장·한도·취소 요청 | [트랜잭션](lessons/phase-04/lesson.md) |
| 5 | `security-operations` / `phase5` | [8085](http://127.0.0.1:8085/login), 로그인·권한·감사·장애 분석 | [보안·운영](lessons/phase-05/lesson.md) |
| 6 | `spring-boot` / `phase6` | [8086](http://127.0.0.1:8086/health), 읽기 전용 REST API | [신규 API](lessons/phase-06/lesson.md) |

위 주소는 현재 `.env` 기준입니다. 이 PC의 기존 포트 점유를 피하려고 PostgreSQL은 `127.0.0.1:15432`(`POSTGRES_PORT=15432`), Phase 4는 `TRANSACTION_PORT=18084`로 실행합니다. `.env.example`의 기본값은 각각 5432와 8084이며, 나머지 웹 포트는 기본값을 사용합니다.

예를 들어 Phase 2를 시작하고 로그를 조회합니다.

```sh
docker compose --profile phase2 up --build -d --wait
docker compose --profile phase2 ps -a
docker compose --profile phase2 logs -f servlet-jsp
```

Phase 1은 종료되는 CLI 프로그램이므로 다음 명령으로 실행합니다.

```sh
docker compose --profile phase1 run --build --rm java-core
```

기본 웹 주소는 `http://127.0.0.1:포트/`입니다. Phase 6은 `/health` 또는 아래 API 경로를 사용합니다. Phase 0 준비 상태는 `/health/ready`, 나머지 웹 앱은 `/health`이며 DB가 준비되지 않으면 HTTP 503입니다.

## 빌드·테스트

전체 기본 모듈의 단위/MVC/보안 테스트와 패키징:

```sh
docker compose --profile test run --build --rm test
```

Docker가 없어도 프로젝트 내부 Maven 또는 PATH의 Maven과 JDK 17이 있으면 로컬 검증할 수 있습니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1
# 의존성을 이미 받은 경우
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Offline
```

```sh
sh scripts/test-local.sh
# 단일 모듈 예시
sh scripts/test-local.sh -pl modules/01-java-core -am
```

실제 PostgreSQL 통합 테스트는 별도 선택 실행입니다. 정상 테스트 데이터는 단계별 스키마와 자체 UUID로 분리합니다. 이체·장애 테스트가 생성한 fixture는 삭제하지 않고 보존합니다.

```sh
docker compose --profile integration run --build --rm integration-test
```

전체 웹 앱, CLI, HTTP/DB 준비 상태, 선택형 재시작 및 통합 테스트를 한 번에 검증합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/verify-all.ps1 -Restart -Integration
```

```sh
sh scripts/verify-all.sh --restart --integration
```

이 명령들은 컨테이너를 실행하고 유지합니다. `-Restart`/`--restart`는 고유 marker를 추가해 DB 재시작 후 보존을 확인합니다. 실제 실행하지 못한 검증을 PASS로 집계하지 않습니다.

## 학습자 실습

각 교재는 개념 → 기준 코드 흐름 → 과제 → 힌트 → 검증 순서입니다. `modules`는 정상 기준 구현, `exercises/starter`는 직접 작성할 TODO입니다. TODO와 정답은 기본 빌드·실행에 포함되지 않습니다.

Phase 1~6 실습 테스트는 아래 명령에서 번호와 경로를 바꿔 실행합니다. 수정 전에는 계약 테스트가 실패하는 것이 정상입니다.

```sh
docker compose --profile exercise run --rm exercise-test -Pexercise-phase1 -pl exercises/starter/phase-01 -am test
```

Phase 0 SQL 실습은 DB 실행 후 `scripts/verify-exercise.ps1` 또는 `sh scripts/verify-exercise.sh`로 검증합니다. 정답은 별도 디렉터리에 있으며 필요할 때만 확인합니다. 장애 재현 fixture는 테스트 소스에만 있고 정상 WAR에 포함하지 않습니다. [장애 실습 안내](docs/troubleshooting/maintenance-labs.md)의 PASS는 증상 재현인지 수정 검증인지 구분해서 읽으세요.

## 로그인과 API 예시

Phase 5와 6의 가상 계정은 `learner / learning-user-password`, `admin / learning-admin-password`입니다. `.env`의 `DEMO_USER_PASSWORD`, `DEMO_ADMIN_PASSWORD`로 변경할 수 있습니다. Phase 5는 로그인 폼·세션·CSRF, Phase 6은 상태 없는 HTTP Basic 조회 API입니다.

```sh
curl -u learner:learning-user-password http://127.0.0.1:8086/api/customers/40000000-0000-0000-0000-000000000001
curl -u admin:learning-admin-password http://127.0.0.1:8086/api/admin/statistics
```

고객·계좌·거래 이력·한도·통계 API의 경로와 JSON 계약은 [API 설명](docs/architecture/api-contract.md)에 있습니다. 거래 데이터의 쓰기 소유자는 Phase 4이고 API는 별도 `bank_learning_api` DB 계정으로 읽습니다. [소유권과 트랜잭션 경계](docs/architecture/legacy-api-ownership.md)를 참고하세요.

## 종료·데이터·마이그레이션

```sh
docker compose --profile '*' down
```

일반 `down`은 DB 볼륨을 유지합니다. 데이터 삭제 명령은 자동 스크립트에 없습니다. 다시 실행하면 기존 고객·잔액·실습 데이터를 사용합니다. DB 계정·암호와 API 읽기 계정의 암호는 최초 생성 시 적용되므로 기존 볼륨에서 `.env` 값만 바꾸면 자동으로 암호가 바뀌지 않습니다.

```sh
docker compose --profile phase0 run --rm migrate
```

마이그레이션·seed는 재실행해도 기존 값을 덮어쓰거나 데이터를 삭제하지 않습니다. 서비스마다 별도 Dockerfile이 있으며 Servlet 6/Tomcat 10.1과 Spring 7/Servlet 6.1/Tomcat 11을 분리했습니다. [기술 호환성](docs/architecture/spring-compatibility.md), [SQL 이식성](docs/architecture/sql-portability.md), [환경 문제 해결](docs/troubleshooting/environment.md), [전체 계획](PLAN.md)을 함께 참고할 수 있습니다.
