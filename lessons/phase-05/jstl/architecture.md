# Phase 5 화면의 책임과 데이터

`GET /workbench` → `WorkbenchServlet` → request attribute → `WEB-INF/views/workbench.jsp` → HTML 응답입니다.

`canViewAudit`: fixture 서버가 결정한 boolean. `audit`: account(이미 마스킹된 문자열), action, requestId, id. 허용 시 2건이며 거절 시 Servlet이 빈 목록을 전달합니다. 원문 개인정보는 없습니다.

`jstl-phase5` 프로필이 이 단계의 JSP 디렉터리를 선택합니다. 해당 단계 starter JSP만 선택형 WAR에 넣으며 기본 빌드와 금융 업무 모듈에는 포함하지 않습니다. 정답은 별도 프로필·별도 WAR/전개 경로입니다. 단계별 데이터와 경로는 소스의 fixture 계약을 따릅니다.

권한 거절과 허용된 빈 목록은 다른 분기입니다. canViewAudit의 허용 분기 안에 표와 빈 결과 메시지를 넣습니다. 이미 마스킹된 번호를 원문으로 복원하거나 JSP에서 임의로 재마스킹하지 않습니다. 실제 인가는 Phase 5 서버가 담당하며 demo 파라미터는 테스트 시나리오에만 사용합니다.

기존 [공통 구조](../../phase-02/jstl/architecture.md)의 내장 Tomcat, WEB-INF 직접 접근 차단, JSP/Java 역할 분리를 그대로 사용합니다. 실습용 조회 필터와 실제 SQL 조회·보안·트랜잭션은 별개의 검증 범위입니다.
