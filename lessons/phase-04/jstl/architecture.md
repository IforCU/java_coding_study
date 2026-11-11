# Phase 4 화면의 책임과 데이터

`GET /workbench` → `WorkbenchServlet` → request attribute → `WEB-INF/views/workbench.jsp` → HTML 응답입니다.

`transfers`: id, status(COMPLETED/PENDING/CANCEL_REQUESTED), amount(BigDecimal). T01은 100,000원 완료, T02는 50,000원 처리 중, T03은 30,000원 취소 요청입니다.

`jstl-phase4` 프로필이 이 단계의 JSP 디렉터리를 선택합니다. 해당 단계 starter JSP만 선택형 WAR에 넣으며 기본 빌드와 금융 업무 모듈에는 포함하지 않습니다. 정답은 별도 프로필·별도 WAR/전개 경로입니다. 단계별 데이터와 경로는 소스의 fixture 계약을 따릅니다.

JSP에서 상태별 문구만 선택합니다. CANCEL_REQUESTED는 환급 완료를 뜻하지 않습니다. c:url로 쿼리를 생성해도 HTML 속성에 넣을 때는 출력 이스케이프가 필요합니다. 링크는 요청 ID를 다시 보여줄 뿐이며 실제 조회·취소 API가 아닙니다.

기존 [공통 구조](../../phase-02/jstl/architecture.md)의 내장 Tomcat, WEB-INF 직접 접근 차단, JSP/Java 역할 분리를 그대로 사용합니다. 실습용 조회 필터와 실제 SQL 조회·보안·트랜잭션은 별개의 검증 범위입니다.
