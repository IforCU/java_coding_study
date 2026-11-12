# Project Progress

## 2026-11-12 Phase 06 JSP lab API fixture 가독성 정리

- `WorkbenchServlet`의 API 계좌 고정 데이터를 `API_ACCOUNTS` 상수로 분리하고 List·Map 구성은 별도 `api(available, empty)` 메서드로 옮겼다. `doGet()`에서는 다른 속성처럼 `setAttribute()` 한 줄로 전달한다. 기존 중첩 구조와 demo 동작은 유지했고 작성 중인 starter JSP는 수정하지 않았다.
- **PASS:** `.\mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml "-Pjstl-phase6,jstl-solution" verify`, HTTP/Jasper 계약 테스트 5개, failures/errors/skipped 0, WAR 빌드 성공.
- Docker/DB 통합 검증은 **NOT RUN / 대기**다. 이번 변경은 고정 fixture 구성 정리이며 신규 기능이나 학습 문제 변경은 없다.

## 2026-11-11 본편 Java 줄바꿈과 들여쓰기

- `phase-04`의 깨끗한 작업 폴더에서 시작했다. `modules`의 Java 113개를 읽고 Google Java Format 1.24.0의 AOSP 방식으로 4칸 들여쓰기를 적용했다. 기존 서식과 일치한 14개를 제외한 소스·테스트 99개가 변경됐다. import 정렬·삭제, 긴 문자열 재작성, Javadoc 재서식은 비활성화했다. 실습 starter/solutions는 수정하지 않았다.
- **PASS:** `.cache/java-format/check.cjs`로 113개 파일의 변경 전후 코드 토큰과 문자열 리터럴이 동일함을 확인했다. 원본과 도구는 프로젝트 `.cache/java-format`에 보관하며 전역 설치·시스템 설정 변경은 하지 않았다.
- **PASS:** `.\mvnw.cmd -o verify`, 기본 7모듈 171개 테스트 모두 통과, failures/errors/skipped 0. JDK 21에서 release 17로 빌드했다. 로그는 `.cache/java-format/verify.log`에 있다. `git diff --check`도 통과했다.
- 실제 JDK 17 실행 및 Docker/DB 통합은 **NOT RUN / 대기**다. 서식 변경이므로 신규 테스트와 교재 내용 변경은 추가하지 않았다. 사용자 요청에 따라 서식·이 기록·계획 변경만 커밋해 main에 cherry-pick하고 phase-04로 복귀한다. 학습 커밋은 main에 포함하지 않으며 Git push는 하지 않는다.

## 2026-11-11 JSP 줄바꿈과 들여쓰기

- 저장소의 JSP 21개(정상 화면 11개, starter 5개, 정답 5개)를 읽고 블록별 줄바꿈과 4칸 들여쓰기를 적용했다. URL/EL/JSTL 태그와 속성을 그대로 보존했으며 금액 뒤 '원', 인사 문구, textarea 등 의미가 있는 인라인 출력은 함께 유지했다. 외부 Tomcat의 JSP는 수정하지 않았다.
- 시작 시 `exercises/starter/phase-02/jstl/accounts.jsp`에 학습자가 작성 중인 변경이 있었다. 원본을 `.cache/jsp-format/user-accounts.jsp`와 전용 stash에 보관해 공통 서식 커밋에서 제외한다. main cherry-pick 후 phase-02에서 학습자 내용을 같은 서식으로 복원한다.
- 모든 JSP를 대상으로 변경 전후 비공백 문자 일치와 태그·속성 문자열의 정확한 일치를 검사했다. 검사 정보와 원본은 `.cache/jsp-format/manifest.json`, `baseline`에 보관했다. 검사 도구는 프로젝트 캐시에만 두며 전역 설치하지 않았다.
- **PASS:** 단계별 정답 `mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml`에 `jstl-solution` 및 해당 `jstl-phaseN` 프로필을 선택해 verify. Phase 2는 7개, Phase 3~6은 각각 5개로 총 27개 HTTP/Jasper 검사가 실패·오류·생략 없이 통과했다. 순번 검사의 두 정규식은 태그 사이 줄바꿈·공백을 허용하되 순번 1·2의 기대값은 유지했다.
- **PASS:** `.\mvnw.cmd -o verify`, 기존 기본 7모듈 171개 테스트 통과. 관련 로그는 `.cache/jsp-format/phase0N-solution.log`, `default-regression.log`에 보관했다. 테스트는 로컬 JDK 21에서 release 17 대상으로 수행했다.
- **PASS:** 각 단계 starter의 선택형 verify도 모두 성공했다. Phase 2 smoke 2개, Phase 3~6 smoke 각 1개를 실행했고 미완성 계약은 선택하지 않아 생략했다. 로그는 `.cache/jsp-format/phase0N-starter.log`에 있다.
- 실제 JDK 17, Docker/DB 통합과 본편 웹 화면의 브라우저 시각 검토는 **NOT RUN / 대기**다. 본편 JSP는 정적 보존 검사와 WAR 빌드로 검증했고 실제 Jasper 렌더링은 실습 JSP 10개를 대상으로 한다. Git push는 하지 않는다.

## 2026-11-11 Phase 2~6 Wrapper·화면 과제 갱신

- `phase-02`에서 작업을 시작했고 시작 시 작업 폴더는 깨끗했다. Phase 2~6 본편·실습·정답 문서의 전역 `mvn` 명령을 Windows `.\mvnw.cmd`와 POSIX `sh ./mvnw`로 교체했다. PowerShell의 `-D` 및 복수 `-P` 인자는 인용하고 기존 Docker 명령은 유지했다. 대상 문서에 전역 mvn 명령이 남지 않았음을 정적 검색으로 확인했다.
- 기존 Phase 2 계좌 JSP는 보존했다. Phase 3 거래 검색·한국 시각, Phase 4 이체 상태·취소 요청 안내, Phase 5 마스킹 감사·거절 표시, Phase 6 중첩 API 응답 표시를 별도 starter/정답과 한국어 교재 각 5종으로 추가했다. 기본 업무 모듈·DB·보안·Boot API는 변경하지 않았다.
- 기존 JSP lab에 `jstl-phase3`~`jstl-phase6` 선택 프로필과 `/workbench` fixture를 추가했다. JSP는 표시만 하고 금액은 BigDecimal을 사용한다. Phase 5의 거절 fixture는 Servlet에서도 감사 목록을 비우며, demo 파라미터가 실제 인가 기능이 아님을 교재에 명시했다. WAR 이름·전개 디렉터리에 단계와 starter/solution을 모두 포함해 격리했다.
- **PASS:** 각 단계 `mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml -Pjstl-phaseN verify` (N=3~6). starter 실제 Jasper/HTTP smoke 1개 통과, 완성 계약 4개는 의도적 skipped. **예상 실패 확인:** `"-Pjstl-phaseN,jstl-check" test`는 각 단계 5개 중 완성 계약 4개 실패, errors/skipped 0이었다. **PASS:** `"-Pjstl-phaseN,jstl-solution" verify`는 각 단계 5개, 합계 20개 통과, failures/errors/skipped 0. 로그는 `.cache/jstl-p03-*`~`jstl-p06-*.log`에 분리했다. 복수 프로필의 최초 무인용 PowerShell 호출은 파서 오류였고 인용 후 검증했다.
- **PASS:** 기존 Phase 2 starter `mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml verify` (smoke 2개 통과/계약 5개 skipped), `-Pjstl-solution verify` (7개 통과, failures/errors/skipped 0). 관련 로그 `.cache/jstl-p02-starter.log`, `jstl-p02-solution.log`.
- **PASS:** `.\mvnw.cmd -o verify`, 기본 7모듈 171개 통과. **PASS:** `solution-phase2`~`solution-phase6` 선택 Wrapper 빌드, 기존 Java/SQL 정답 계약 58개 통과. XML 집계와 `.cache/phase02-06-wrapper-regression.log`, `phase02-06-solutions.log`에 근거를 남겼다. 본편의 미완성 starter는 수정하지 않았다.
- **PASS:** Compose `jstl-lab` 설정 검사, Phase 2~6 Markdown 로컬 링크 검사, diff 공백 검사. Docker는 같은 루트 서비스의 `JSTL_PHASE` 빌드 인자로 단계만 선택하며 이미지에 정답을 복사하지 않는다. 기존 `.env`는 변경하지 않았다.
- 검증은 JDK 21에서 release 17 대상으로 수행했다. 실제 JDK 17, Linux/macOS 네이티브 실행, Docker 이미지 빌드·기동, 실제 DB·API 연동과 브라우저 시각 검토는 **NOT RUN / 대기**다. 이번 변경을 Docker/DB까지 전체 완료로 기록하지 않는다. 로컬 검증 뒤 사용자 지시에 따라 관련 변경을 커밋·main cherry-pick하고 phase-02로 복귀하며 Git push는 하지 않는다.

## Current Phase
**Phase 0~6 기준 구현·교재·실습 존재. 현재 Phase 1 학습 및 Maven Wrapper 도입. 로컬 기본 테스트 재검증 완료, 전체 Docker/DB 재검증은 대기.**

### 2026-11-11 로컬 동기화 (Asia/Seoul)

#### 은행 JSP/JSTL 화면 보조 실습

- 사용자가 지정한 외부 Tomcat 9의 `jstl-shop.jsp`를 읽어 반복·필터·조건·금액·빈 목록 실습 형식을 확인했다. 외부 파일과 서버는 수정하지 않았다. 현재 작업 브랜치 `phase-01`에서 기존 학습 파일을 보존했다.
- `exercises/jstl-bank-lab`에 DB 없는 조회용 Servlet과 선택형 WAR 모듈을 추가했다. `BigDecimal` 잔액과 마스킹된 가상 계좌 4개를 request attribute로 전달한다. starter는 `exercises/starter/phase-02/jstl/accounts.jsp`, 정답·해설은 `exercises/solutions/phase-02/jstl`에 분리했다. 정상 모듈과 기본 Maven reactor에 TODO/정답을 넣지 않았다.
- Phase 2와 같은 Servlet 6.0 / Tomcat 10.1.61 / Jakarta Tags API 3.0.2 + 구현 3.0.1을 사용한다. 공식 Jakarta Tags 3.0 및 Tomcat Jasper 문서를 확인했으며 `javax.servlet`을 혼합하지 않았다. `lessons/phase-02/jstl`에 한국어 교재 5종을 작성하고 Phase 1/2 및 README에 진입 링크를 추가했다.
- 루트 Maven `jstl-lab`, 모듈 `jstl-check` 프로필과 루트 Compose `jstl-lab` 서비스를 추가했다. Compose는 `127.0.0.1:${JSTL_LAB_PORT:-8087}`에 starter만 공개한다. Dockerfile 전용 ignore로 필요한 소스만 허용하고 정답을 복사하지 않는다. 기존 `.env`, DB, 컨테이너와 볼륨은 변경하지 않았다.
- **PASS:** `.\mvnw.cmd -Pjstl-lab -pl exercises/jstl-bank-lab -am verify`. 실제 내장 Tomcat/Jasper HTTP 검사 2개 통과, 완성 계약 5개는 의도적으로 skipped. starter WAR 빌드 성공. WAR 내용을 검사해 starter TODO 포함, 정답/해설/JUnit/내장 Tomcat/테스트 클래스 제외를 확인했다.
- **예상 실패 확인:** `.\mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml -Pjstl-check test`. 미완성 starter는 7개 중 smoke 2개 통과, 완성 동작 5개 실패, 오류·생략 0. 최초 `-Djstl.contract=true` 무인용 명령은 PowerShell 인자 해석으로 Maven 진입 단계에서 실패했으며, 인용에 의존하지 않는 `jstl-check` 프로필로 바꾸어 위 결과를 재현했다.
- **PASS:** `.\mvnw.cmd -o -f exercises/jstl-bank-lab/pom.xml -Pjstl-solution verify`. 정답 JSP 실제 렌더링 계약 7개, 실패·오류·생략 0. 전체/종류별 목록, 금액·상태, 필터 후 순번, 두 종류의 빈 결과, 이름 공백·HTML 이스케이프, 생성 링크, WEB-INF 접근 차단을 확인했다. starter/정답은 WAR 이름과 전개 디렉터리도 분리한다.
- **PASS:** `.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml test-compile exec:java`로 로컬 서버 시작 후 `http://127.0.0.1:8087/accounts`의 HTTP 200과 starter TODO를 확인했다. 검증 서버는 종료했고 8087 listener가 남지 않았음을 확인했다. 기존에 실행 중이던 다른 서버는 중지하지 않았다.
- **PASS:** `.\mvnw.cmd -o verify`, 기본 7모듈 171 tests, 실패·오류 0. 기본 reactor에 JSP 실습 모듈이 들어가지 않았음을 확인했다. `docker compose --profile jstl-lab config --quiet` 종료 코드 0과 `git diff --check`도 통과했다.
- 실행 환경은 로컬 JDK 21 / release 17이다. 실제 JDK 17, Docker 이미지 빌드·기동 및 DB 통합은 **NOT RUN / 대기**이며 이번 실습 전체 Docker 완료로 표시하지 않는다. 화면은 HTTP 응답으로 검증했고 실제 브라우저의 시각 검토는 미실행이다. 로그는 `.cache/jstl-starter.log`, `jstl-contract.log`, `jstl-solution.log`, `jstl-regression.log`에 보관했다. 전역 설치·시스템 설정 변경·Git commit/push는 수행하지 않았다.

#### Maven Wrapper 도입

- Apache 공식 `maven-wrapper-distribution:3.3.4:only-script`의 `mvnw`/`mvnw.cmd`와 `.mvn/wrapper/maven-wrapper.properties`를 추가했다. Maven은 기존 프로젝트와 동일한 3.10.0으로 고정했다. 공식 배포본 SHA-512를 대조한 후 SHA-256을 properties에 고정해 최초 Wrapper 다운로드 시 검증한다.
- 저장소 정책에 맞게 Wrapper에 프로젝트 캐시 설정을 추가했다. 배포본은 `.cache/maven-wrapper`, 의존성은 `.cache/maven-repository`, JVM 사용자 홈은 `.cache/java-home`, 임시 파일은 `.cache/tmp`에 둔다. Windows 설정은 SETLOCAL로 해당 프로세스에만 적용한다. Wrapper 파일의 줄바꿈은 `.gitattributes`로 지정했다.
- `scripts/test-local.ps1`/`.sh`는 Wrapper를 우선 사용하고, Wrapper가 없을 때만 기존 로컬 Maven/PATH 방식을 사용한다. README, 개발 가이드, Phase 1 과제·검증 문서를 수정했다.
- `.\mvnw.cmd -v`: **PASS / 종료 코드 0**, Maven 3.10.0 다운로드·체크섬 검증·실행 확인. 현 세션에서는 JDK 21 (`C:\Program Files\OpenJDK\jdk-21`)이 감지되었다. 앞선 Java 부재 기록과 달리 현재 환경에서는 실행 가능하다. Wrapper가 JDK를 설치하지는 않았다.
- `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Module 01-java-core`: **PASS**, 20 tests, 실패·오류·생략 0.
- 전체 `test-local.ps1 -Offline` 첫 시도는 미다운로드 `maven-war-plugin:3.5.1`로 의존성 해석 단계에서 실패했다. 코드 오류가 아니며, 이후 `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1`로 필요한 의존성을 받은 뒤 **BUILD SUCCESS / 종료 코드 0**, 7개 기본 모듈 171 tests, 실패·오류·생략 0을 확인했다. 결과는 각 모듈 `target/surefire-reports/TEST-*.xml`에 있다.
- Git sh의 `sh ./scripts/test-local.sh -o -pl modules/01-java-core -am`: **PASS**, 20 tests. 초기 POSIX JVM 경로 인용 문제는 수정 후 재검증했다. POSIX 구문 및 PowerShell 파서 검사와 `git diff --check`도 통과했다.
- 이번 실행은 JDK 21에서 `release 17`로 컴파일했다. 실제 JDK 17 실행, Linux/macOS 네이티브 실행, Docker/DB 통합, starter/정답 계약은 이번 변경에서 NOT RUN이다. 전역 설치·환경 설정 변경·Git push는 수행하지 않았다.

#### 학습 답변 검토

- 사용자 요청에 따라 `exercises/starter/phase-00/answer.md`의 원문을 보존하고 각 답변 아래 정정·보충 해설을 추가했다. 해설 위치는 이번 사용자의 명시적 파일 지정에 따랐다.
- Phase 0의 Compose, POM, Dockerfile 및 Servlet/DB probe/설정/HTTP health helper 코드와 대조했다. HTTP와 JDBC 주소, live/ready, HTTP와 DB 타임아웃, 볼륨의 영속성, Java와 Servlet API 선택을 구분했다.
- 문서 검토 작업으로 애플리케이션 코드와 실행 환경은 변경하지 않았다. Java/Docker 실행 테스트는 이번 범위에서 NOT RUN이며, 문서 변경은 `git diff --check`로 확인한다.

#### 후속 점검: Phase 0 테이블 누락 문의

- 일반 권한의 Docker 접근은 pipe 권한 오류였으나 승인된 샌드박스 외부 실행에서 프로젝트 컨테이너를 조회했다. `db`와 `legacy`는 healthy, `migrate`는 Exited (0)이었다. 현재 공개 포트는 PostgreSQL 5432, 웹 8080이다.
- 읽기 전용 조회로 `current_database() = bank_learning`, `current_user = bank_learning`, `to_regclass('bootstrap.app_metadata') = bootstrap.app_metadata` 및 실습용 메타데이터 행 존재를 확인했다.
- `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/verify-exercise.ps1`: 종료 코드 **0 / PASS**. 현재 학습자가 작성한 starter SQL이 검증을 통과했다. 학습자 파일은 수정하지 않았다.
- 사용자 후속 설명으로 최초 오류는 `syntax error at or near "FROM"`임을 확인했다. SELECT 열을 작성한 뒤 TODO 영역의 쉼표가 남아 다음 열을 요구한 문법 오류이며 테이블 누락이 아니다. 현재 파일은 해당 구분 기호가 정리되어 PASS한다.
- Phase 0 verification에 TODO 구분 기호와 문법 오류 설명, 접속 DB·포트·스키마 확인 절차를 보완했다. migration 재실행, 데이터 변경·삭제, 컨테이너 재시작은 수행하지 않았다. 전체 Java 테스트·재시작·다른 Phase 검증은 이번 범위에서 NOT RUN이다.

- `AGENTS.md`, `PLAN.md`, 이 문서, 루트 POM/Compose와 Phase 0 검증 절차를 대조했다. 아래 2026-10-25 PASS 표는 과거 기록이며 오늘 재실행한 결과가 아니다.
- 점검 시작 시 작업 폴더는 깨끗했다. `phase-00`, `main`, 로컬 추적 참조 `origin/main`은 모두 `a773109`였다. 원격 fetch/pull/push는 수행하지 않았으므로 원격 최신 여부는 미확인이다.
- `.env`는 존재하지만 `.tools`와 `artifacts`는 없다. 이들은 Git 제외 경로이므로 과거 로그와 도구가 새 작업 폴더에 자동 복원되지 않는다. 기존 `.env`는 변경하지 않았다. 문서의 15432/18084는 과거 환경 값이며 오늘의 적용 포트로 확인하지 않았다.
- `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/doctor.ps1`: 종료 코드 **1**. Docker Compose CLI v5.6.1 확인 후 Linux 엔진 named pipe 연결이 `permission denied`로 실패했다. 현재 실행 권한에서 엔진에 접근할 수 없는 환경 문제이며 엔진 중지 여부는 단정하지 않는다. Compose 설정 및 HTTP/DB 검증은 **NOT RUN / 대기**다.
- `powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Offline`: 종료 코드 **1**. 프로젝트 내부 Maven과 PATH Maven이 없어 빌드 진입 전 중단했다. PATH의 Java도 없다. Java 컴파일·테스트 결과는 **NOT RUN**이며 코드 실패로 판정하지 않는다.
- 최소 조치: 로컬 검증에는 JDK 17과 프로젝트 내부 Maven, 의존성 캐시를 준비한다. Docker 검증 재개 시 Linux 엔진 접근 권한을 확인하고 doctor부터 재실행한다. 전역 설치·시스템 설정 변경은 하지 않았다.
- 브랜치와 환경 확인 가이드를 `docs/development-workflow.md`에 추가했다. 이번 변경은 문서 동기화이며 애플리케이션 코드와 실습 정답은 수정하지 않았다.

기록일: 2026-10-25, Asia/Seoul. 초기 Docker 미준비 시 사용자가 **Docker 검증은 대기로 기록하고 나머지 코드·교재 구현 진행**을 선택하여 각 Phase의 로컬 검증을 순서대로 진행했다. 이후 엔진이 연결되어 대기했던 실제 컨테이너 검증도 완료했다. 전체 검증 스크립트는 23:26 KST 종료 코드 0이며, 웹 업무 시나리오와 일반 down/up 데이터 보존도 통과했다.

## Completed
- Phase 0: Maven WAR, Hello World/JSP, JDBC readiness, 재실행 가능한 migration/seed, Compose health/dependency/volume/profile.
- Phase 1: 메모리 고객·다중 계좌·정수 원 단위 BigDecimal 입출금·잔액·거래 이력, 불변 DTO와 저장소 경계.
- Phase 2: Servlet/JSP/JSTL/JDBC, PreparedStatement, HikariCP, 고객·계좌·이력, Form·Session·CSRF·303 PRG.
- Phase 3: 독립 Spring MVC/MyBatis WAR, XML/Annotation DI, Mapper XML·동적 기간 SQL·결과 매핑·고객 상태 제한.
- Phase 4: 이체·멱등 요청·한도·원장·취소 요청, Spring TX, 정렬된 FOR NO KEY UPDATE 잠금. 취소 요청은 자금 환급이 아니다.
- Phase 5: Spring Security 로그인·역할·CSRF·세션 고정 방어, 개인정보 마스킹, 감사, 요청 ID, SQL 템플릿 로그, 공통 오류.
- Phase 6: 별도 Boot 조회 API, DTO·JSON·Bean Validation·소유권·관리자 통계·Mockito. SELECT 전용 DB 역할과 복합 한도 조회의 REPEATABLE READ.
- 각 Phase에 교재 5종, 학습자 TODO·계약 테스트·선택 정답·해설. 정답/TODO는 기본 모듈과 JAR/WAR에서 제외.
- 장애 8종의 격리된 재현 fixture·분석 과제·힌트·수정 검증 계약·별도 해설. 실제 DB fixture는 phase5_lab 스키마.
- Java 8/11/17, javax/jakarta와 WAS, Oracle/Tibero SQL, iBATIS/MyBatis, 단일 DB와 외부 금융 거래 경계 문서.
- 모듈별 Dockerfile, 6개 migration·5개 seed, 단위/실습/통합 테스트 프로필과 Windows/POSIX 실행 스크립트.
- 초기화 순서의 UTF-8 필터 중복 등록과 브라우저 세션 쿠키 테스트 조건을 수정하고 회귀 검증.
- Boot 재패키징 후 Failsafe 클래스 경로 문제를 수정하고 실제 DB 통합 테스트를 재실행.
- Docker 이미지 빌드, 전체 웹 준비 상태, JSP·폼 업무 시나리오, migration/seed 재실행, 재시작 및 일반 종료·재실행 검증.

## In Progress
현 PC의 빌드 및 Docker/DB 재검증은 위 최신 점검과 같이 대기 중이다. TODO는 학습자가 직접 구현하도록 유지한다. 아래 실행 상태와 PASS 표는 2026-10-25 기록이다.

## Verification

| 검증 | 결과 | 근거와 범위 |
| --- | --- | --- |
| 전체 Maven reactor verify | **PASS** | Java 17.0.9 / 프로젝트 내부 Maven 3.10.0, 7개 모듈 모두 SUCCESS |
| 기본 JUnit/MVC/보안/Mockito | **PASS** | 171 tests, 0 failures/errors/skipped. 정상 검증 163개 + 격리 증상 재현 8개 |
| 별도 정답 계약 | **PASS** | Phase 1~6 합계 79개, 0 failures/errors/skipped |
| 미완성 TODO 계약 | **예상 실패 확인** | 모든 starter가 컴파일되고 수정 전 계약 테스트가 실패함 |
| Java Core CLI | **PASS** | 두 계좌의 입출금·잔액·거래 이력 실제 출력 |
| Boot JAR 실제 로컬 HTTP | **PASS** | 127.0.0.1:18086에서 기동, 401/403/400 및 DB 미준비 503 확인 후 종료 |
| JAR/WAR 패키징 | **PASS** | 5개 WAR에 Servlet API·JUnit·Mockito·실습/lab 클래스 없음, Boot 실행 manifest/의존성 확인 |
| 공통 Docker health helper | **PASS** | JDK 17 컴파일 및 실제 6개 웹 컨테이너 healthy |
| Compose 전체 프로필 | **PASS (설정)** | config --quiet 종료 코드 0 |
| PowerShell / POSIX scripts | **PASS (구문)** | PS parser와 Git sh -n, test-local.ps1 실제 전체 검증 |
| 교재/Markdown | **PASS (정적)** | 7단계 x 필수 교재 5종, 로컬 링크 검사 |
| 실제 PostgreSQL IT 18개 | **PASS** | 2/2/7/4/3개, 0 failures/errors/skipped. rollback·동시성·권한·스냅샷 포함 |
| Docker 빌드·CLI·HTTP/DB health | **PASS** | 모든 모듈 이미지, Java CLI, 6개 웹 준비 상태. 컨테이너 내 기본 171개도 PASS |
| 실제 JSP·폼·보안·API | **PASS** | Phase 2~6 업무 시나리오 5종. 생성·검색·멱등 이체·취소·로그인·CSRF·마스킹·XSS escape·조회 연계 |
| DB migration·seed·읽기 role | **PASS** | V001~V006/R001~R005 재실행, 기존 데이터 보존, API 쓰기 거절 SQLState 42501 |
| 재시작·영속성·일반 종료/실행 | **PASS** | DB/app restart marker 보존. down/up 후 계좌 전체 해시·요청/원장/스키마/marker 수 동일, 6개 웹 healthy |
| Phase 0 SQL 실습 검증 | **PASS** | 정답 쿼리를 실제 DB에서 읽기 전용으로 검증 |

### 기본 테스트 수

| Phase | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 합계 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 테스트 | 16 | 20 | 28 | 26 | 26 | 33 | 22 | 171 |

정답 계약은 21/12/7/7/21/11개다. 실제 PostgreSQL IT는 Phase 2/3/4/5/6에 각각 2/2/7/4/3개다. 장애 실습의 PASS는 증상 재현 성공이며 학습자의 수정 완료를 의미하지 않는다.

### 실제 실행과 산출물

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Offline
docker compose --profile '*' config --quiet
```

최종 전체 reactor는 **2026-10-25 23:02 KST BUILD SUCCESS**였다. 상세 로그는 `artifacts/local-verification.log`, 집계는 `artifacts/verification-summary.json`, 모듈별 Surefire XML은 각 `target/surefire-reports`에 있다. 정답은 별도 프로필/개별 POM으로 실행했고 starter의 실패도 실제 XML 결과로 확인했다.

Docker에서 `scripts/verify-all.ps1 -Restart -Integration`은 **23:26 KST BUILD SUCCESS**, 종료 코드 0이었다. `artifacts/docker-all-final.log`에 전체 reactor의 기본 171개와 실제 PostgreSQL IT 18개가 기록되어 있다. 최초 실행에서 발견한 Boot Failsafe 클래스 경로 문제는 Boot POM의 `classesDirectory`를 `target/classes`로 지정하여 해결했다. 실패 로그도 `artifacts/docker-all.log`에 보존했다.

실제 HTTP 검증은 `artifacts/docker-web-smoke.json`과 `.log`에 있다. Phase 4의 100원 이체를 동일 요청 ID로 재전송해 한 번만 반영되는지 확인하고, 취소 요청 후에도 잔액이 유지됨을 확인했다. Phase 6 API가 해당 원장을 읽는 것까지 검증했다. 초기 검증 스크립트의 한글 정규식은 PowerShell 5.1 인코딩 영향을 받아 ASCII 날짜 패턴으로 수정했다. 재실행은 5개 시나리오 모두 PASS다.

일반 종료·재실행은 `artifacts/docker-lifecycle.json`과 `.log`에 있다. `down`으로 모든 프로젝트 서비스가 종료된 뒤 기존 DB 볼륨으로 재생성했고, 이체 테스트 이후 잔액·거래·원장과 marker가 동일했다. `--volumes`나 DB 데이터 삭제를 사용하지 않았다. Phase 0의 읽기 전용 SQL 검증은 `artifacts/docker-sql-exercise.log`에 있다.

Boot 실행 JAR은 실제 내장 Tomcat으로 localhost에서 기동했다. 인증 없는 요청 401, 일반 사용자의 관리자 통계 403, 잘못된 반환 건수 400, 의도적으로 없는 DB 주소에 대한 준비 상태 503을 확인했다. 결과는 `artifacts/local-api-smoke.json`에 있다. 이는 DB 정상 연결 성공이 아니며 Docker 실행을 대체하지 않는다. 임시 서버는 종료했다.

Maven 배포 ZIP은 SHA-512 확인 후 .tools에만 풀었고 Maven 캐시·임시 파일은 프로젝트 내부에 둔다. 전역 패키지 설치, PC 설정 변경, 데이터 삭제, Git push, 외부 배포는 수행하지 않았다.

## Issues
- 해결되지 않은 검증 실패 없음. 초기 Docker 엔진 미준비·실행 권한 문제는 엔진 준비 후 승인된 실행 권한으로 검증하여 해소했다.
- 기존 5432/8084 포트가 점유되어 `.env`의 DB를 **15432**, 이체 웹 앱을 **18084**로 지정했다. 기존 서비스를 중지하지 않았다. 다른 웹 포트는 8080/8082/8083/8085/8086이다.
- Docker CLI는 프로세스 `DOCKER_CONFIG=.cache/docker`를 사용했다. 사용자 설정 파일 권한·WSL·Windows 서비스 설정은 변경하지 않았다.
- 기존 DB/API reader 암호는 migration 재실행으로 덮어쓰지 않는다. 기존 볼륨에서 .env만 변경했다면 실제 저장된 인증정보와 맞춰야 한다.

## Next
학습은 [README](README.md)의 Phase 0부터 시작하고 TODO를 직접 구현한다. 전체 환경을 다시 검증하려면 다음 명령을 사용한다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/verify-all.ps1 -Restart -Integration
```

또는 `sh scripts/verify-all.sh --restart --integration`을 사용한다. 과거 검증 당시 Hello World는 `http://127.0.0.1:8080/`, 이체 앱은 `http://127.0.0.1:18084/`에서 실행했다. 현재 실행 여부와 적용 포트는 재확인이 필요하다. 학습 종료 시 `docker compose --profile '*' down`으로 종료하며 DB 볼륨은 유지한다.
