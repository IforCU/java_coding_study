# 개발 브랜치와 환경 확인

## 프로젝트와 현재 작업

이 프로젝트는 가상 고객과 계좌로 Java 은행 업무를 배우는 로컬 학습 환경이다. 요청이 웹 화면, Servlet/Controller, Service, SQL과 DB를 거치는 흐름을 추적하고 수정한다. 실제 금융 서비스나 운영 배포를 목표로 하지 않는다.

| Phase | 학습 내용 |
| --- | --- |
| 0 | Maven WAR, Tomcat/JSP, Docker Compose, PostgreSQL 연결과 데이터 유지 |
| 1 | Java 객체 설계, BigDecimal 입출금, 메모리 저장소 |
| 2 | Servlet/JSP/JDBC, 고객·계좌 등록과 조회 |
| 3 | Spring MVC/MyBatis, DI, SQL 매핑과 검색 |
| 4 | 이체, 트랜잭션, 동시성, 멱등성, 원장 |
| 5 | 인증·인가, 보안, 감사와 장애 분석 |
| 6 | Spring Boot 읽기 전용 REST API와 테스트 |

`modules`는 실행 가능한 기준 구현, `lessons`는 교재, `exercises/starter`는 직접 푸는 과제다. 정답은 별도 디렉터리에 있으며 기본 빌드에 포함되지 않는다. Phase 번호가 붙은 브랜치를 만들어도 다른 Phase 코드가 사라지지는 않는다. 현재 저장소에는 이미 0~6 기준 코드가 있으므로 Phase 0부터 읽고 실습한다.

## 브랜치 운영

개인 학습에는 `main`과 Phase별 작업 브랜치면 충분하다. 별도 `develop`은 필요하지 않다. `main`은 확인된 기준을 보존하고, 현재 `phase-00`에서 Phase 0 문서와 실습을 진행한다. 공통 코드 수정은 필요하면 `fix/구체적인-문제`로 분리한다. 이는 이 저장소에서 제안하는 운영 방식이다.

작업 시작 시 다음 읽기 전용 명령으로 위치와 변경을 확인한다.

```powershell
git status --short --branch
git branch -vv
git log -5 --oneline --decorate
git diff --stat
```

2026-11-11 점검 시작 시 `phase-00`, `main`, 로컬의 `origin/main`은 `a773109`로 같았다. `origin/main`은 마지막으로 저장된 원격 참조이므로 원격 서버의 최신 상태를 보장하지 않는다. 이번 동기화는 로컬 문서 정합성 작업이며 fetch/pull/push는 수행하지 않았다.

작업 완료 후 변경 파일을 직접 선택해 커밋한다. 다음은 예시이며 이번 점검에서 실행하지 않았다.

```powershell
git diff --check
git diff
git add exercises/starter/phase-00/read-metadata.sql
git commit -m "study: phase 0 SQL exercise"
```

위 `git add` 경로는 실제 수정한 파일 경로로 바꾼다. 로컬 완료 검증이 통과하면 다음 순서로 병합하고 다음 학습 브랜치를 만들 수 있다. Docker/DB가 대기라면 전체 Phase 완료로 기록하지 않는다. 브랜치를 먼저 전환하더라도 대기 항목은 계속 추적한다.

```powershell
git switch main
git merge --ff-only phase-00
git switch -c phase-01
```

`--ff-only`가 실패하면 분기 이력을 확인한다. 강제 reset으로 맞추지 않는다. `.env`, 도구, 캐시, 로그는 커밋하지 않으며 Git push와 외부 배포는 하지 않는다.

## 코드 작업 환경 확인

Maven Wrapper 추가 후에는 JDK만 준비하면 `mvn` 전역 설치 없이 실행할 수 있습니다. Windows에서는 `.\mvnw.cmd -v`, Linux/macOS에서는 `sh ./mvnw -v`로 확인합니다. 기존 로컬 검증 스크립트도 Wrapper를 우선 사용하므로 아래 `.tools` Maven 점검은 이전 방식의 대체 수단입니다. 최초 실행 시 네트워크가 필요하고 배포본·의존성·임시 파일은 프로젝트 `.cache`에 보관합니다.

IDE에서 저장소 루트를 열고 Maven 프로젝트로 가져온다. 로컬 컴파일·디버깅 시 프로젝트 SDK와 Maven 실행 JDK를 17로 맞추고 파일 인코딩은 UTF-8로 설정한다. 호스트 Java/Maven이 없어도 Docker 빌드는 가능하지만, IDE의 로컬 컴파일에는 JDK가 필요하다. 전역 설치나 시스템 설정 변경 대신 프로젝트 내부 도구를 사용한다.

```powershell
Get-Command java,mvn,docker -ErrorAction SilentlyContinue
Test-Path .tools/apache-maven-3.10.0/bin/mvn.cmd
Test-Path .env
```

`.env`가 없을 때만 생성한다. 기존 파일의 자격 증명을 출력하거나 덮어쓰지 않는다.

```powershell
if (-not (Test-Path -LiteralPath .env)) { Copy-Item .env.example .env }
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/doctor.ps1
```

doctor는 엔진 연결과 Compose 설정 검사이며 앱 빌드·DB 검증을 대신하지 않는다. 2026-11-11에는 Docker CLI가 있었지만 엔진 pipe 접근이 거절됐다. 이 경우 Docker 검증은 대기로 기록하고 가능한 코드·교재 작업을 진행한다.

JDK 17과 프로젝트 내부 Maven이 준비되면 Phase 0 또는 전체 기본 모듈을 검증한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Module 00-bootstrap
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1
```

의존성 캐시가 이미 준비된 경우에만 `-Offline`을 추가한다. 캐시와 임시 파일은 스크립트가 프로젝트 `.cache`에 둔다. 로컬 검증 통과는 실제 PostgreSQL 연결 성공을 의미하지 않는다.

Docker 검증을 재개할 수 있는 환경에서는 다음 순서로 Phase 0을 확인한다. 아래 명령은 컨테이너를 시작하고 DB migration/seed를 실행하므로 환경 점검 명령과 구분한다.

```powershell
docker compose --profile phase0 config --quiet
docker compose --profile phase0 up --build -d --wait
docker compose --profile phase0 ps -a
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/verify.ps1
```

포트는 현재 `.env`를 따른다. 웹은 `127.0.0.1`에만 공개한다. `migrate`의 `Exited (0)`는 정상이다. 재시작·영속성까지의 절차는 [Phase 0 검증](../lessons/phase-00/verification.md)을 따르고 실행 날짜·결과·미실행 원인을 [PROGRESS.md](../PROGRESS.md)에 기록한다. 실습 SQL은 [과제](../lessons/phase-00/exercise.md)를 읽은 뒤 작성한다.
