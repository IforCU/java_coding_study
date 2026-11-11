# Phase 6 화면의 책임과 데이터

`GET /workbench` → `WorkbenchServlet` → request attribute → `WEB-INF/views/workbench.jsp` → HTML 응답입니다.

`api`: success(boolean), message(안전한 사용자 메시지), requestId, data.accounts(별칭 name과 balance 목록). 성공 기본 목록은 2건이며 잔액은 1,250,000원·350,000원입니다. 원본 JSON이 아닌 표시용 Map fixture입니다.

`jstl-phase6` 프로필이 이 단계의 JSP 디렉터리를 선택합니다. 해당 단계 starter JSP만 선택형 WAR에 넣으며 기본 빌드와 금융 업무 모듈에는 포함하지 않습니다. 정답은 별도 프로필·별도 WAR/전개 경로입니다. 단계별 데이터와 경로는 소스의 fixture 계약을 따릅니다.

먼저 api.success로 분기하고 성공한 경우에만 목록의 empty를 검사합니다. 실패와 성공·빈 목록은 서로 다릅니다. JSP에서 JSON을 파싱하거나 인증정보를 가진 HTTP 호출을 작성하지 않습니다. 실제 Boot API의 네트워크·HTTP·인증 계약은 본편 검증에서 따로 확인합니다.

기존 [공통 구조](../../phase-02/jstl/architecture.md)의 내장 Tomcat, WEB-INF 직접 접근 차단, JSP/Java 역할 분리를 그대로 사용합니다. 실습용 조회 필터와 실제 SQL 조회·보안·트랜잭션은 별개의 검증 범위입니다.
