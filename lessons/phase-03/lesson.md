# Phase 3 — Spring MVC와 MyBatis로 책임 연결하기

정상 모듈은 `modules/03-spring-mvc`입니다. Phase 2의 Servlet/JDBC WAR를 유지하고 별도 Spring MVC WAR와 `phase3` 스키마를 사용합니다. 고객·계좌 관리에 고객 상태와 거래 기간 검색을 추가합니다.

## IoC, DI, Bean

Spring 컨테이너가 객체를 만들고 생성자에 필요한 의존성을 전달합니다. 이 프로젝트에서 `BankController`는 `BankService`를, 서비스는 `BankMapper`를 받습니다. 호출자가 DAO와 풀을 직접 조립하던 Phase 2와 객체 구성 위치를 비교해 보세요. 컨테이너가 관리하는 객체를 Bean이라 부릅니다. [Spring IoC와 DI](https://docs.spring.io/spring-framework/reference/core/beans/introduction.html)

`RootConfig`는 `root-context.xml`을 가져옵니다. XML에서 DataSource, SqlSessionFactory, Mapper 스캔, TransactionManager를 연결하고 서비스 패키지를 스캔합니다. `WebConfig`의 Java 설정과 `@EnableWebMvc`는 Controller, JSP ViewResolver, Interceptor를 구성합니다. XML과 Annotation은 서로 배타적인 방식이 아니라 역할에 맞춰 함께 사용할 수 있습니다.

## DispatcherServlet과 Controller

`WebInitializer`가 DispatcherServlet을 `/`에 등록합니다. DispatcherServlet은 요청 경로에 맞는 Controller 메서드를 찾고 반환한 view 이름을 JSP로 연결합니다. `@GetMapping`, `@PostMapping`, `@RequestParam`, `Model`을 Phase 2의 `doGet`, `doPost`, 파라미터와 request attribute 처리에 대응시켜 보세요. [DispatcherServlet 공식 문서](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html)

등록은 303 redirect를 사용하고 조회는 JSP view를 반환합니다. `CsrfInterceptor`가 Controller 앞에서 폼 토큰을 확인합니다. 애플리케이션 초기화, HTTP 매핑, 입력·업무 검증, SQL의 위치를 분리합니다.

## MyBatis Mapper와 결과 매핑

`BankMapper`는 인터페이스이고 `BankMapper.xml`의 namespace와 statement ID가 메서드에 연결됩니다. Mapper 프록시가 JDBC 실행과 결과 변환을 처리합니다. `resultMap`은 DB 열과 DTO 생성자 인자의 타입·순서를 명시합니다. 금액은 BigDecimal, 조회 시각은 OffsetDateTime, UUID는 이 모듈의 DTO에서 문자열로 전달합니다. [Mapper XML과 결과 매핑](https://mybatis.org/mybatis-3/sqlmap-xml.html)

`if`, `where` 같은 동적 SQL 태그로 선택 조건을 추가합니다. 검색 값을 SQL 텍스트로 직접 합치지 않고 바인딩합니다. 동적 조건은 어떤 조건이 생성되는지와 값이 어떤 순서로 전달되는지를 함께 검증해야 합니다. [MyBatis 동적 SQL](https://mybatis.org/mybatis-3/dynamic-sql.html)

## 트랜잭션 경계

서비스의 `@Transactional`이 업무 호출의 경계를 선언하고 DataSourceTransactionManager가 연결의 commit/rollback을 관리합니다. SqlSessionFactory와 TransactionManager는 같은 DataSource를 사용해야 합니다. 풀은 애플리케이션이 시작할 때 만들고 종료할 때 닫습니다. [MyBatis-Spring 트랜잭션 구성](https://mybatis.org/spring/transactions.html)

현재 단계의 등록·상태 변경은 단일 업무 처리의 경계를 학습합니다. 실제 DB rollback, 이체의 부분 실패, 동시성과 멱등성은 별도 검증 범위입니다. Annotation이 있다는 이유만으로 실제 DB 검증을 통과한 것은 아닙니다.

## 추가 업무 규칙

고객 상태는 `ACTIVE`, `SUSPENDED`입니다. 정지 고객의 새 계좌 개설은 409로 거절합니다. 기간 검색의 화면 입력은 ISO 날짜이며 Asia/Seoul 기준 시작일 0시부터 종료일 다음 날 0시 직전까지 포함합니다. 내부 Mapper는 시작 포함·끝 제외 시각 구간으로 받습니다.

Java 17 기준에서 이 WAR는 Spring 7, Tomcat 11, Servlet 6.1을 사용합니다. Phase 2의 Tomcat 10.1 WAR와 독립해 호환성을 유지합니다. 예전 iBATIS 코드와 비교할 때는 [iBATIS/MyBatis 문서](../../docs/legacy-analysis/ibatis-mybatis.md)를 참고합니다.

직접 구현할 Mapper 과제는 [exercise.md](exercise.md), 실제 실행 구분은 [verification.md](verification.md)에 있습니다.
