# Phase 2 — Servlet, JSP, JDBC로 요청부터 SQL까지

화면 문법부터 직접 연습하려면 [은행 JSP/JSTL 보조 실습](jstl/lesson.md)을 먼저 진행하세요. DB 없이 가상 계좌로 반복·조건·금액 표시·안전한 출력·빈 목록 처리를 작성합니다. Phase 1 뒤에도 시작할 수 있으며 본편 JDBC 구현과 분리되어 있습니다.

이번 단계의 정상 실행 모듈은 `modules/02-servlet-jsp`입니다. Phase 1의 메모리 예제를 보존하고, 별도 WAR와 `phase2` DB 스키마로 웹 요청 흐름을 학습합니다.

핵심 목표는 고객·계좌 등록과 조회, 거래 내역 조회를 통해 HTTP부터 SQL까지 추적하는 것입니다. 웹 입출금 거래는 이후 단계에서 다룹니다.

## HTTP 요청과 Servlet

브라우저가 GET으로 조회 화면을 요청하면 `BankServlet.doGet`이 요청 경로와 파라미터를 읽습니다. `HttpServletRequest`는 입력과 request attribute를 제공하고 `HttpServletResponse`는 HTTP 상태, 헤더와 응답을 설정합니다. 조회 데이터를 attribute로 저장한 뒤 JSP에 forward합니다.

등록 폼은 POST를 사용합니다. 서버는 입력과 CSRF 토큰을 확인해 데이터를 저장하고, 성공하면 상태 303과 `Location`을 반환합니다. 브라우저가 다시 GET으로 목록을 열도록 하는 Post/Redirect/Get(PRG) 흐름입니다. 새로고침에 따른 동일 폼 재전송을 줄이지만, 금융 거래의 중복 방지를 완성하는 장치는 별도로 필요합니다.

| 방식 | 처리 위치 | URL과 요청 데이터 |
| --- | --- | --- |
| forward | 서버 내부에서 JSP로 전달 | 같은 요청과 attribute를 사용 |
| redirect | 브라우저가 응답을 받고 새 요청 | URL이 바뀌고 request attribute는 이어지지 않음 |

공식 API에서 [RequestDispatcher](https://tomcat.apache.org/tomcat-10.1-doc/servletapi/jakarta/servlet/RequestDispatcher.html)와 [HttpServletResponse](https://tomcat.apache.org/tomcat-10.1-doc/servletapi/jakarta/servlet/http/HttpServletResponse.html)의 책임을 확인할 수 있습니다.

## JSP EL과 JSTL

JSP는 `/WEB-INF/views`에 두고 Servlet의 forward로 접근합니다. `${customers}` 같은 EL은 request attribute에서 데이터를 읽습니다. JSTL `c:forEach`, `c:if`로 반복과 조건을 표현하고 `c:out`으로 HTML에 출력할 값을 이스케이프합니다. 화면에 JDBC 코드나 Java scriptlet을 넣지 않습니다.

`BankViews`의 DTO는 record에 JavaBean getter도 제공합니다. 이 모듈에서 JSP EL이 `${customer.name}`을 어떻게 읽는지 getter까지 따라가 보세요.

## JDBC와 연결 풀

`JdbcBankDao`는 HikariCP의 DataSource에서 Connection을 빌립니다. SQL 구조와 값은 PreparedStatement로 분리하고, ResultSet의 UUID·금액·시각을 DTO로 매핑합니다. `getBigDecimal`은 금융 금액의 정밀도를 유지합니다. Connection, PreparedStatement, ResultSet은 실패 경로에서도 정리해야 합니다. [JDBC PreparedStatement 공식 설명](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html)

HikariCP의 풀 크기와 대기 시간은 `AppListener`에서 설정합니다. 요청이 끝나면 Connection을 풀에 반환하고, WAR 종료 시 전체 풀을 닫습니다. 풀을 쓴다는 이유만으로 연결 정리가 생략되지 않습니다. [HikariCP 설정](https://github.com/brettwooldridge/HikariCP)

## 세션과 입력 검증

세션에는 선택한 고객 ID와 CSRF 토큰을 저장합니다. 고객 선택은 조회 편의 기능이며 인증·인가의 구현 범위는 후속 Phase입니다. POST는 세션의 토큰과 폼의 토큰을 비교하고 누락·불일치 시 거절합니다. 고객 이름과 UUID도 서버에서 검증합니다.

현재 WAR는 Java 17, Tomcat 10.1, Jakarta Servlet 6.0, JSP 3.1, JSTL 3 계열을 사용합니다. `javax.servlet` 기반 예제를 붙여 넣지 않고 같은 Jakarta 계열 API를 유지합니다. JDBC의 `javax.sql.DataSource`는 Java SE 패키지로 별도 의미입니다.

PostgreSQL의 UUID, numeric, 날짜·시각과 SQL 문법을 Oracle/Tibero 코드에 그대로 복사하지 않습니다. 대상 DB 버전에서 확인할 항목은 [SQL 이식성 문서](../../docs/architecture/sql-portability.md)에 있습니다.

정상 요청 구조는 [architecture.md](architecture.md), 직접 작성할 TODO와 레거시 분석 과제는 [exercise.md](exercise.md)를 확인합니다.
