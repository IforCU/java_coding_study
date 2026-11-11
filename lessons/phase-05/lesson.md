# Phase 5 — 보안 경계와 유지보수 분석

직접 작성할 화면 과제는 [단계별 JSTL 실습](jstl/lesson.md)에 있습니다. 기존 계좌 목록 문제를 반복하지 않고 이 단계의 표시·상태·오류 분기를 연습합니다.

정상 모듈은 `modules/05-security-operations`입니다. Spring MVC와 Spring Security로 사용자 인증, 본인 정보 조회, 관리자 감사 조회, CSRF와 세션, 출력과 로깅 경계를 학습합니다. 의도적인 오류는 별도 테스트 fixture에만 둡니다.

## 인증과 인가

인증은 사용자가 누구인지 확인하고, 인가는 그 사용자가 요청한 기능과 데이터를 사용할 수 있는지 판단합니다. `/profile`은 인증한 자신의 정보를 조회하고 `/admin/audit`는 ADMIN 역할을 요구합니다. URL의 소유자 값을 바꿔 타인의 정보로 전환하지 않습니다. [Spring Security 요청 권한](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)

학습용 로그인은 `learner`/`learning-user-password`, 관리자 로그인은 `admin`/`learning-admin-password`입니다. 모두 저장소에 공개한 로컬 가상 계정이며 실제 정보는 사용하지 않습니다. 폼 로그인과 HTTP Basic을 함께 관찰합니다. Basic은 각 요청의 자격 증명으로 다시 인증될 수 있어 세션 만료와 분리해 확인합니다.

## 세션과 CSRF

로그인 후 세션 ID를 바꾸는 session fixation 보호와 HttpOnly 쿠키, 15분 유휴 세션 설정을 확인합니다. 세션 만료 후에는 다시 인증해야 합니다. 브라우저 쿠키 설정과 실제 동작은 Tomcat 실행 후 별도로 확인합니다. [인증 유지와 세션](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)

`POST /profile/note`는 CSRF 토큰과 서버 입력 검증을 통과해야 합니다. 로그인한 상태만으로 다른 사이트에서 시작된 변경 요청을 허용하지 않습니다. Spring Security의 필터가 토큰을 검증하며 `/csrf`는 인증된 요청에 토큰을 제공합니다. [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

## 입력, SQL, HTML의 경계

프로필 note는 정해진 길이로 검증합니다. SQL에는 PreparedStatement 바인딩을 사용하고 JSP는 `c:out`으로 HTML 출력 값을 이스케이프합니다. 입력이 유효한지, DB 값으로 전달되는지, 브라우저에서 어떻게 해석되는지는 서로 다른 문제입니다.

개인정보 마스킹은 표시 범위를 줄입니다. 원문을 로그에 남긴 뒤 화면에서만 가리는 흐름도 확인해야 합니다. 실습 `AuditMasker`는 정상 서비스와 분리된 정책 함수입니다.

## 감사와 요청 추적

RequestTraceFilter는 허용한 형식의 `X-Request-ID`만 받아 사용하거나 새 추적 ID를 만듭니다. 오류 응답과 서버의 요청 기록을 같은 ID로 연결할 수 있습니다. 임의 헤더나 줄바꿈을 그대로 로그에 연결하지 않습니다.

감사 기록은 누가 어떤 작업을 했는지 제한된 항목으로 남기며 비밀번호, 세션 토큰과 원본 계좌를 기록하지 않습니다. SQL 관찰 로그는 템플릿을 기록하고 바인딩 값·비밀번호를 덤프하지 않습니다. 공통 오류 응답은 오류 코드와 trace ID로 조사 경로를 제공하며 내부 예외를 그대로 노출하지 않습니다.

## 장애 학습 범위

SQL 조건, 풀 고갈, 느린 쿼리, 트랜잭션 범위, 권한 누락, 중복 거래, 세션 만료, 잘못된 매핑을 테스트 전용 fixture에서 분석합니다. 랩의 PASS는 증상 재현 성공이며 수정 완료와 다릅니다. [장애 랩 실행 안내](../../docs/troubleshooting/maintenance-labs.md)

[exercise.md](exercise.md)의 마스킹 TODO와 장애 분석 과제를 수행합니다. 실제 DB와 Tomcat 검증의 최신 결과는 루트 `PROGRESS.md`에서 확인합니다.
