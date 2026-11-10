# Servlet/JSP/JDBC 요청을 따라 읽기

Phase 2의 목표는 고객·계좌 등록과 조회 요청이 Java 객체와 SQL로 바뀌는 과정을 추적하는 것이다. 정상 실행 코드는 [Phase 2 모듈](../../modules/02-servlet-jsp)에 두고, 미완성 과제와 정답은 기본 빌드에서 분리한다. Java 패키지의 기준 경로는 `kr.banklearning.servlet`이며 아래 호출 순서를 실제 소스와 대조한다.

이 문서는 소스 흐름과 재현 절차를 설명한다. 소스나 단위 테스트를 읽은 사실은 Tomcat/JSP/PostgreSQL 통합 검증이 아니다. 실제 실행 여부와 최신 결과는 [진행 기록](../../PROGRESS.md)에서 확인한다.

## 등록 후 조회까지의 전체 경로

```text
브라우저
  │ GET: 목록/등록 화면 요청
  ▼
Tomcat → Servlet 라우팅 → 서비스 → DAO
                                  │ DataSource.getConnection()
                                  ▼
                         Connection → PreparedStatement
                                  │ 값 바인딩 → executeQuery()
                                  ▼
                         PostgreSQL phase2 스키마
                                  │ ResultSet → DTO 목록
                                  ▼
Servlet → request attribute → JSP/JSTL → HTML 응답

브라우저
  │ POST: 고객/계좌 등록 form
  ▼
Servlet → 입력 파싱/검증 → 서비스 → DAO → executeUpdate()
  │ 성공 시 redirect
  ▼
브라우저 GET → 저장된 고객/계좌 다시 조회 → HTML 응답
```

GET은 화면과 데이터를 읽고, POST는 등록 상태를 변경한다. 정상 등록 후 redirect로 GET을 요청하는 흐름이 PRG(Post/Redirect/Get)다. 새로고침의 반복 POST를 줄이지만 중복 클릭이나 네트워크 재시도의 원자적 중복 방지를 보장하지는 않는다. 금융 거래의 멱등 키는 Phase 4에서 별도로 구현한다.

로컬 기본 주소는 `http://127.0.0.1:8082`다. `BankServlet`은 `/` 매핑으로 요청을 받은 뒤 `getServletPath()`에 따라 나눈다.

| 요청 | 실제 메서드 흐름 | 결과 |
| --- | --- | --- |
| `GET /customers` | `BankServlet.doGet` → `BankService.customers` → `JdbcBankDao.customers` | `customers` attribute → `/WEB-INF/views/customers.jsp` |
| `POST /customers` | CSRF 토큰 검사 → `BankService.createCustomer` → `JdbcBankDao.insertCustomer` | 세션 고객 선택 → HTTP 303 `/customers` |
| `GET /accounts?customerId=...` | `BankService.accounts` → 고객 존재 확인 → `JdbcBankDao.accounts` | `customers`, `accounts` attribute → `accounts.jsp` |
| `POST /accounts` | CSRF 토큰 검사 → `BankService.createAccount` → 고객 확인 → `JdbcBankDao.insertAccount` | 0원 계좌 등록 → HTTP 303 `/accounts` |
| `GET /history?accountId=...` | `BankService.account`·`history` → `JdbcBankDao.account`·`history` | `account`, `transactions` attribute → `history.jsp` |
| `POST /select-customer` | CSRF 토큰 검사 → `BankService.customer` | 세션 `selectedCustomerId` 변경 → HTTP 303 `/accounts` |
| `GET /health` | `BankService.ready` → `JdbcBankDao.ready` | 세 테이블 조회 성공 시 HTTP 200, 실패 시 503 |

`AppListener.contextInitialized`가 HikariDataSource → JdbcBankDao → BankService를 조립하고 ServletContext에 넣는다. `BankServlet.init`은 그 서비스를 꺼낸다. 종료 시 listener가 풀을 닫고 현재 WAR의 JDBC 드라이버 등록을 해제한다. DTO는 `BankViews`의 `CustomerView`, `AccountView`, `TransactionView` record이며 JSP EL용 getter도 제공한다. [초기화 코드](../../modules/02-servlet-jsp/src/main/java/kr/banklearning/servlet/config/AppListener.java), [Servlet](../../modules/02-servlet-jsp/src/main/java/kr/banklearning/servlet/web/BankServlet.java), [DAO](../../modules/02-servlet-jsp/src/main/java/kr/banklearning/servlet/dao/JdbcBankDao.java)

## 소스에서 확인할 다섯 경계

| 경계 | 확인할 입력·출력 | 자주 발생하는 유지보수 문제 |
| --- | --- | --- |
| HTTP → Servlet | 요청 method/path, form 이름, 문자열 UUID | form 이름과 `getParameter` 키가 다름, GET에서 저장 |
| Servlet → 서비스 | 검증된 이름·고객 ID, 업무 예외 | 빈 이름·잘못된 UUID를 DB 예외에 맡김 |
| 서비스 → DAO | 고객 존재, 소유 관계, 조회·등록 계약 | 화면 조건만 믿음, SQL이 JSP에 섞임 |
| DAO → JDBC | 고정 SQL, 바인딩 순서·타입, 영향받은 행 수 | 문자열 연결, 컬럼 순서 오독, 리소스 누수 |
| DTO → JSP | request attribute 이름, EL 속성, 출력 escape | 속성 이름 불일치, 사용자 이름을 HTML로 직접 출력 |

Servlet은 여러 요청이 같은 인스턴스에 들어올 수 있다. 요청별 입력, Connection, ResultSet, 응답 DTO를 Servlet 인스턴스 필드에 저장하면 요청이 섞인다. Servlet 필드는 공유 가능한 서비스·DataSource 참조에 한정하고 요청 데이터는 지역 변수와 request 범위에 둔다. [Jakarta Servlet 6.0 명세](https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0)

JSP는 조회 결과를 출력하는 뷰다. JSP 안에서 Connection을 열거나 SQL을 조합하지 않는다. JSTL `c:out`의 escape 동작을 유지하며 사용자 입력을 스크립트·이벤트 속성에 넣는 것은 피한다. `WEB-INF` 아래 뷰는 브라우저가 JSP 파일 경로를 직접 호출하는 대신 Servlet의 forward를 통해 렌더링한다. [Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/), [Jakarta Servlet 요청 전달](https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0)

## Connection과 ResultSet의 수명

DataSource는 Connection을 얻는 진입점이고 HikariCP는 실제 연결 풀 구현이다. 풀의 Connection에서 `close()`를 호출하면 정상 상황에는 빌린 연결을 풀에 반환한다. ResultSet과 Statement를 닫은 뒤 Connection을 반환한다. ResultSet을 Servlet/JSP까지 전달하지 말고 DAO 안에서 DTO로 복사한다. [HikariCP 연결 풀 설정](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby), [JDBC PreparedStatement](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/PreparedStatement.html)

하나의 SQL SELECT라도 정상 조회·빈 결과·바인딩 오류·SQL 예외 경로를 각각 읽는다. try-with-resources의 괄호에 무엇이 들어 있는지 확인하면 예외 때 닫히는 자원을 추적할 수 있다. DAO의 예외를 잡는 위치에서는 내부 SQL·DB URL·인증정보를 응답이나 로그에 그대로 넣지 않는다. 사용자에게는 일반 오류를 주고 실패 원인은 필요한 범위의 비밀정보 없는 진단으로 구분한다.

고객 없는 계좌를 막는 FK와 `balance >= 0` CHECK는 저장 제약이다. 현재 서비스가 고객 존재를 먼저 검사하더라도 경쟁 요청이나 다른 실행 경로에 대비한 DB 제약은 유지한다. 제약만으로 잔액과 거래 내역이 원자적으로 일치하지는 않는다. 여러 SQL을 묶는 업무는 같은 Connection의 트랜잭션 경계에서 처리해야 한다. [JDBC Connection](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Connection.html), [Phase 2 SQL 계약](../architecture/sql-portability.md)

## 세션과 화면 상태

request attribute는 현재 요청이 끝나면 사라지고 session attribute는 같은 세션의 후속 요청에서 남는다. redirect는 새 요청이므로 등록 성공 메시지를 request에만 넣으면 다음 화면에서 보이지 않는다. 세션을 사용한 메시지는 한 번 읽은 뒤 제거해 뒤이은 화면에 재표시되지 않게 한다.

현재 세션에는 `selectedCustomerId`와 기본 form 보호용 `csrfToken`을 둔다. GET에서 토큰을 화면에 전달하고 POST에서 기존 세션 토큰과 비교한다. 계좌 조회에 customerId가 없으면 선택한 고객을 쓰고, 선택도 없으면 전체 계좌를 조회한다. 고객 선택은 인증 기능이 아니다.

Phase 2의 세션 사용은 HTTP 상태 유지 학습이다. 세션에 고객 UUID가 있다는 사실만으로 인증이 완료되거나 해당 계좌의 소유권이 검증된 것은 아니다. Phase 5에서 인증·인가·세션 갱신·만료를 별도로 다룬다. 이 단계에서도 DB 비밀번호나 JDBC Connection을 세션에 넣지 않는다. 현재 구현에는 flash 메시지가 없으며 앞의 request/session 메시지 설명은 수명 차이를 이해하기 위한 예다.

## 요청 하나를 디버깅하는 순서

1. 브라우저 Network에서 요청 URL, method, form 필드, 상태 코드와 redirect Location을 확인한다.
2. Servlet 진입점에 중단점을 놓고 form 필드가 어떤 Java 값으로 변환되는지 확인한다.
3. 서비스의 고객 존재/입력 검증을 통과하는지 본다. 실패하면 DAO까지 갔는지 구분한다.
4. DAO의 SQL과 setter 순서를 비교한다. SQL에 입력 문자열이 직접 붙지 않는지 확인한다.
5. execute 결과와 ResultSet의 컬럼 이름을 DTO 매핑과 비교한다. 정수 원화도 `getBigDecimal`로 읽는다.
6. Servlet의 request attribute와 JSP의 EL 이름이 일치하는지 확인한다. 값이 있는데 화면이 비면 출력 경계를 조사한다.
7. 응답 후 Statement/ResultSet/Connection이 정리되는지 확인한다. 반복 요청 뒤 풀 고갈이 생기면 예외 경로까지 추적한다.

검색 명령은 소스 위치를 찾는 도구이며 실행 검증 결과가 아니다.

```powershell
rg -n 'doGet|doPost|WebServlet|sendRedirect|getRequestDispatcher' modules/02-servlet-jsp/src
rg -n 'getConnection|prepareStatement|setObject|setString|executeQuery|executeUpdate' modules/02-servlet-jsp/src
rg -n 'getSession|setAttribute|removeAttribute|c:out' modules/02-servlet-jsp/src
```

실제 SQL의 읽기 전용 관찰은 [phase2.sql](../../database/queries/phase2.sql)을 참고한다. 최초 가상 고객은 `20000000-0000-0000-0000-000000000001`, 계좌는 `20000000-0000-0000-0000-000000000101`이다. 새 DB에 처음 seed하면 100,000원 입금 이력이 한 건 생긴다. 실습 후에는 기존 상태를 보존하므로 seed 재실행이 잔액을 초기화하지 않는다.

[JdbcBankDaoIT](../../modules/02-servlet-jsp/src/test/java/kr/banklearning/servlet/dao/JdbcBankDaoIT.java)는 실제 PostgreSQL을 대상으로 Unicode·SQL처럼 보이는 입력의 값 보존, 고객 FK, 계좌별 조회, BigDecimal 및 시간대가 있는 시각의 이력 매핑을 검증한다. `DB_URL`, `DB_USER`, `DB_PASSWORD`가 없으면 명확히 실패한다. 테스트가 만든 UUID 데이터는 하나의 테스트 트랜잭션에만 두고 마지막에 rollback한다. 기존 seed를 삭제하거나 DDL을 실행하지 않는다. 기본 단위 테스트 실행과 별도로 `integration-tests` 프로필에서 수행하며, DB 미준비 시 미실행으로 기록한다.

## Java 8/11 소스를 읽을 때

현재 빌드는 Java 17과 `jakarta.servlet`을 사용한다. Java 8/11용 `.class`와 Java 17용 `.class`의 실행 가능 범위, Tomcat 9의 `javax.servlet`과 Tomcat 10.1의 `jakarta.servlet`을 구분한다. 예전 JAR를 현재 WAR에 복사해 섞지 않는다. JDBC `java.sql`·`javax.sql`은 Java SE API이므로 `jakarta.sql`로 바꾸지 않는다. 구체적인 WAS/Servlet 표는 [호환성 문서](../architecture/compatibility.md)를 읽는다.
