# Phase 3 화면의 책임과 데이터

`GET /workbench` → `WorkbenchServlet` → request attribute → `WEB-INF/views/workbench.jsp` → HTML 응답입니다.

`history`: id, type(DEPOSIT/WITHDRAWAL/TRANSFER), memo, amount(BigDecimal), occurredAt(java.util.Date). 기준 3건이며 금액은 1,500,000원·120,000원·50,000원입니다. 첫 시각은 2026-11-01 09:00(Asia/Seoul)입니다.

`jstl-phase3` 프로필이 이 단계의 JSP 디렉터리를 선택합니다. 해당 단계 starter JSP만 선택형 WAR에 넣으며 기본 빌드와 금융 업무 모듈에는 포함하지 않습니다. 정답은 별도 프로필·별도 WAR/전개 경로입니다. 단계별 데이터와 경로는 소스의 fixture 계약을 따릅니다.

종류 조건과 메모 조건은 AND로 연결합니다. 공백을 정리한 검색어가 비면 모든 문자열에 포함됩니다. 날짜의 표시는 원본 Date와 지정한 타임존에 따라 달라집니다. 실제 DB 기간 검색은 Controller/Service/SQL의 책임이며 JSP는 이번 fixture의 표시 필터만 담당합니다.

기존 [공통 구조](../../phase-02/jstl/architecture.md)의 내장 Tomcat, WEB-INF 직접 접근 차단, JSP/Java 역할 분리를 그대로 사용합니다. 실습용 조회 필터와 실제 SQL 조회·보안·트랜잭션은 별개의 검증 범위입니다.
