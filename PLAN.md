# 개발 및 학습 계획

## 진행 원칙

2026-11-11 Phase 2~6 안내 갱신: 본편·실습·정답의 로컬 Maven 명령을 Windows/POSIX Wrapper로 통일한다. 기존 Phase 2 계좌 JSP 과제는 유지하고, Phase 3 거래 검색·Phase 4 이체 상태·Phase 5 감사 표시·Phase 6 API 응답 표시 과제를 기존 선택형 JSP lab에 추가한다. 본편 WAR/JAR와 기본 빌드는 보존하며 각 단계 starter/정답·교재 5종·HTTP 계약을 분리한다. 로컬 검증 후 관련 커밋만 main에 cherry-pick하고 phase-02로 복귀한다. Docker 통합은 대기로 기록한다.

2026-11-11 JSP/JSTL 보조 실습: Phase 2에 DB 없는 계좌 화면 TODO와 5종 교재를 추가한다. Java 17 / Jakarta Servlet 6 / Tomcat 10.1 / Jakarta Tags 3을 사용하고, `jstl-lab` 선택 프로필로 기본 빌드와 분리한다. 가상 BigDecimal 계좌 데이터는 Servlet이 전달하며 JSP는 반복·필터·조건·형식화·안전한 출력을 담당한다. starter/정답은 분리하고 실제 내장 Tomcat HTTP 계약 검사로 검증한다. Docker 검증은 대기로 유지한다.

2026-10-25 사용자 추가 지시에 따라 Docker 대기 중에도 각 Phase의 코드·교재와 로컬 검증을 순서대로 진행했다. Docker 엔진 복구 후 실제 컨테이너·PostgreSQL·HTTP·재시작·종료 재현 검증까지 통과하여 Phase 0~6 학습 환경을 완료했다. 실제 통과한 검증 범위와 실행 근거는 `PROGRESS.md`에 기록한다.

## 완료한 실행 검증

- PASS: 기본 테스트 171개와 선택 정답 계약 테스트 79개. 미완성 starter의 예상 실패는 정상 모듈 실패와 분리했다.
- PASS: 전체 이미지 빌드, 웹 앱 6개의 DB 준비 상태.
- PASS: Phase 0 JSP와 JDBC 연결, migration/seed 재실행, DB·앱 재시작 후 고유 marker 보존.
- PASS: 실제 PostgreSQL 통합 테스트 18개(Phase 2/3/4/5/6 각 2/2/7/4/3개), 실패·오류·생략 0개. Boot 테스트 실행 설정을 수정한 뒤 전체 reactor 재검증을 통과했다.
- PASS: Phase 2~6 HTTP/JSP·폼 시나리오 5개. 이체 멱등 재전송·취소 요청 후 잔액 보존, 로그인·CSRF·XSS 출력, 조회 API 연계를 포함한다.
- PASS: 일반 `down`으로 프로젝트 서비스를 종료한 뒤 볼륨을 유지해 `up`으로 재생성했다. 웹 앱 6개가 healthy이고 marker 수, Phase 4 계좌 행의 JSON 해시, 이체 요청·원장·스키마 이력 수가 전후 동일했다.
- 현재 `.env`: PostgreSQL 공개 포트 15432, Phase 4 공개 포트 18084. 기존 5432·8084 점유를 피해 설정했으며 나머지 웹 포트는 기본값이다.
- 명령·결과는 `PROGRESS.md`, 전체 실행 근거는 `artifacts/docker-all-final.log`, `artifacts/docker-web-smoke.json`, `artifacts/docker-lifecycle.json`에 기록했다.

| Phase | 목표와 구현 범위 | 주요 파일 | 다음 단계 진입 조건 |
| --- | --- | --- | --- |
| 0 | Maven WAR, Compose, PostgreSQL, Tomcat, Hello World, DB readiness, 영속성과 재시작 | 루트 설정, `modules/00-bootstrap`, `docker/legacy`, `database`, `scripts`, `lessons/phase-00` | 이미지 빌드, DB healthy, HTTP/JSP/DB 연결, 테스트, 마이그레이션 재실행, 재시작 및 종료 검증 통과 |
| 1 | 메모리 기반 고객·다중 계좌·입출금·잔액·거래 내역, BigDecimal, 캡슐화·인터페이스·DTO·예외 | `modules/01-java-core`, `lessons/phase-01`, 별도 starter/solutions | 금액 정밀도, 음수/0 입력, 잔액 부족, 소유 관계 및 거래 기록 단위 테스트 통과 |
| 2 | Servlet/JSP/JSTL/JDBC, 고객·계좌 등록/조회, 세션, Form, GET/POST, PRG, 연결 풀 | `modules/02-servlet-jsp`, `docker/legacy`, 전용 스키마 | 브라우저 요청부터 PreparedStatement/ResultSet까지 추적, 통합 테스트, 격리된 레거시 개선 과제 검증 |
| 3 | Spring MVC/MyBatis, XML·Annotation 설정, IoC/DI, 동적 SQL, 거래 기간 검색, 고객 상태 제한 | `modules/03-spring-mvc`, `docker/spring-mvc`, 전용 스키마 | Servlet 구현 보존, 매핑·검색·트랜잭션·상태 제한 통합 테스트 통과 |
| 4 | 이체, 일일 한도, 멱등 키, 거래 상태·취소 요청, 원장, ACID·격리·Lock | `modules/04-banking-transaction`, 전용 테스트 데이터 | 부분 실패 rollback, 중복/동시 요청, DB 예외, 응답 유실 시 잔액·원장·중복 방지 상태 검증 |
| 5 | 인증·인가·CSRF·XSS·세션·마스킹·감사·추적 ID, 유지보수 장애 분석 | `modules/05-security-operations`, `docs/troubleshooting`, 격리 실습 | 보안 회귀 테스트, 장애 8종의 재현·힌트·수정 TODO·검증·별도 해설 완성 |
| 6 | 별도 Spring Boot REST API, 조회·이력·한도·통계, Validation, 공통 오류, JUnit/Mockito | `modules/06-spring-boot`, `docker/spring-boot` | API 계약/권한/통합 테스트, 기존 DB 소유권·트랜잭션 영향 검증, 전체 재현 실행 통과 |

## Phase 0 목표와 수정 범위

- 루트에 `AGENTS.md`, `PLAN.md`, `README.md`, `PROGRESS.md`, `pom.xml`, `compose.yaml`, `.env.example`을 작성한다.
- `modules/00-bootstrap`에 정상 동작 기준 WAR와 JUnit 테스트를 둔다. Java 17 / Jakarta Servlet 6.0 / Tomcat 10.1 조합으로 시작한다.
- `database/migrations`와 `database/seed`는 데이터를 지우지 않고 재실행 가능하게 만든다.
- `scripts`에 환경 점검, 실행 검증, 재시작 확인 및 선택형 SQL 실습 검증을 제공한다.
- 학습자의 TODO SQL과 정답은 `exercises/starter/phase-00`, `exercises/solutions/phase-00`로 격리한다.
- Docker 엔진이 연결되지 않으면 실제 컨테이너 검증을 NOT RUN으로 기록한다. 사용자 추가 승인에 따라 로컬 검증 가능한 후속 단계는 이어간다.

## 반영한 기술 선택과 실행 검증

- 각 단계에서 공식 지원 정책과 안정 의존성 버전을 확인했다. 전체 이미지 빌드, 웹 앱 health, 세부 업무 HTTP 시나리오와 실제 DB 통합 검증을 통과했다.
- Servlet 6/Tomcat 10.1 WAR, Spring 7/Servlet 6.1/Tomcat 11 WAR, Boot 4 실행 JAR를 독립 구성했다.
- `javax.servlet` 기반 구형 코드는 실행이 필요할 때만 호환 Tomcat과 별도 프로필에 격리한다. Jakarta WAR에 혼합하지 않는다.
- Phase 2/3/4/5는 각각 전용 스키마를 사용하고 장애 실습은 `phase5_lab`으로 격리했다. Phase 6은 별도 DB 역할로 Phase 4 데이터를 읽는다.
- Java 8/11과 17, Tomcat/Servlet 호환표, Oracle/Tibero SQL 차이와 iBATIS/MyBatis 비교를 문서화했다.
- 단일 DB rollback과 외부 금융기관 거래의 재시도·대사·보상 경계를 문서화했다.
- 실제 PostgreSQL 통합 테스트는 `integration-tests` 프로필, 전체 컨테이너 검증은 `scripts/verify-all`로 실행한다. 진행 중·실패·미실행 항목은 `PROGRESS.md`에서 실제 통과 결과와 구분한다.

## 각 Phase의 교육 산출물

`lesson.md`, `architecture.md`, `exercise.md`, `hints.md`, `verification.md`를 만든다. 정상 기준 구현, 미완성 TODO, 검증용 테스트, 선택해서 열어보는 정답을 분리한다. 정답을 기본 Maven 모듈이나 Compose 실행 경로에 넣지 않는다.
