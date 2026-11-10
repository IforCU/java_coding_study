# iBATIS 2 소스를 MyBatis 3 관점에서 읽기

Phase 3은 Servlet/JDBC 구현을 보존하고 별도 Spring MVC/MyBatis WAR를 추가한다. iBATIS 2 코드는 오래된 금융 프로젝트를 읽기 위한 비교 대상이며 실행 모듈에 iBATIS JAR를 추가하지 않는다. 현재 구현의 매핑 계약은 [BankMapper.java](../../modules/03-spring-mvc/src/main/java/kr/banklearning/mvc/mapper/BankMapper.java)와 [BankMapper.xml](../../modules/03-spring-mvc/src/main/resources/kr/banklearning/mvc/mapper/BankMapper.xml)에서 확인한다.

아래 내용은 Mapper 계약과 검증 범위를 설명한다. XML 파싱·단위 테스트와 실제 PostgreSQL 실행은 구분하며 최신 결과는 [진행 기록](../../PROGRESS.md)에서 확인한다.

## 같은 SQL 매핑이지만 설정과 호출 API는 다르다

| 읽는 지점 | iBATIS 2 계열 소스 | 현재 MyBatis 3 소스 |
| --- | --- | --- |
| 패키지 | `com.ibatis.*` | `org.apache.ibatis.*`, Spring 연동은 `org.mybatis.spring.*` |
| 실행 진입점 | SqlMapClient와 문자열 statement ID | SqlSession 또는 mapper 인터페이스 프록시 |
| XML 루트 | `<sqlMap namespace="...">` | `<mapper namespace="인터페이스의 전체 이름">` |
| 입력 타입 | `parameterClass`·parameterMap | `parameterType` 또는 mapper 메서드/`@Param`에서 해석 |
| 결과 타입 | `resultClass`, resultMap의 `class` | `resultType`, resultMap의 `type` |
| 값 바인딩 | `#id#` 형태 | `#{id}` 형태 |
| 문자열 치환 | `$...$` 형태 | `${...}` 형태; 외부 입력을 넣지 않음 |
| 동적 조건 | `<dynamic>`, `<isNotNull>`, `<isEqual>` 등 | `<if>`, `<choose>`, `<where>`, `<trim>`, `<foreach>` 등 |
| 트랜잭션 | 기존 SqlMapClient·프레임워크의 관리 경계 확인 | 현재는 Spring 서비스 `@Transactional`과 MyBatis-Spring 연동 |

iBATIS 비교 근거는 [Apache에 보존된 Java 2 개발자 가이드](https://svn.apache.org/repos/asf/ibatis/java/ibatis-2/trunk/ibatis-2-docs/en/iBATIS-SqlMaps-2_en.pdf)와 [iBATIS 2 소스 저장소](https://github.com/mybatis/ibatis-2)다. 위 표의 이름을 일괄 치환하는 작업만으로 이식이 끝나는 것은 아니다. 반환 타입, NULL 처리, 동적 조건, 키 생성, 캐시와 트랜잭션을 각각 검증해야 한다. MyBatis 문법은 [Mapper XML](https://mybatis.org/mybatis-3/sqlmap-xml.html)과 [동적 SQL](https://mybatis.org/mybatis-3/dynamic-sql.html)을 기준으로 한다.

## namespace·메서드·파라미터를 한 묶음으로 읽기

현재 `BankMapper`의 `customer(@Param("id") String id)`는 XML의 namespace `kr.banklearning.mvc.mapper.BankMapper`와 `<select id="customer">`에 연결된다. SQL의 `#{id}`는 메서드 인자 이름과 맞아야 한다. DTO를 받는 `insertCustomer(@Param("customer") CustomerView customer)`의 SQL은 `#{customer.name}`처럼 지정한 이름 아래 속성을 사용한다.

현재 ID는 DTO에서 문자열이며 PostgreSQL에는 UUID로 저장한다. 따라서 XML에 `CAST(#{id} AS uuid)`가 있고 결과에는 `CAST(id AS varchar)`가 있다. mapper로 바뀌어도 [DB 타입과 방언 계약](../architecture/sql-portability.md)은 그대로 존재한다. SQL 문법을 재사용하기 전에 실제 컬럼 타입과 TypeHandler를 함께 확인한다.

`#{...}`는 JDBC의 값 자리표시자로 처리한다. `${...}`는 SQL 텍스트에 직접 삽입되므로 고객 이름이나 정렬 요청값을 넣으면 SQL 구조가 바뀔 수 있다. 컬럼명·정렬 방향은 서버가 정한 허용 목록을 선택하도록 설계한다. SQL 바인딩은 인증이나 소유권 검사를 대신하지 않는다. [MyBatis 파라미터와 문자열 치환](https://mybatis.org/mybatis-3/sqlmap-xml.html#Parameters)

## 결과 매핑은 컬럼과 Java 생성자의 계약이다

`BankViews`는 record DTO를 사용한다. `customerView` resultMap은 id/name/status 컬럼을 생성자 인자 순서와 타입에 맞춰 지정한다. 계좌 금액은 `BigDecimal`, 이력 시각은 `OffsetDateTime`으로 읽는다. 컬럼 이름이 바뀌거나 DTO 생성자 순서가 바뀌면 SQL 조회 자체가 성공해도 Java 객체를 만들지 못할 수 있다.

레거시 소스에서 `SELECT *`와 자동 매핑을 함께 사용하면 테이블 변경이 화면에 예상치 못한 영향을 줄 수 있다. 현재 XML은 조회 컬럼과 constructor resultMap을 명시한다. XML 파싱 성공만으로 실제 드라이버의 UUID·numeric·timestamptz 변환이 검증된 것은 아니다. [MyBatis Result Maps](https://mybatis.org/mybatis-3/sqlmap-xml.html#Result_Maps)

## 기간 조건이 없을 때와 경계값이 있을 때

현재 이력 검색은 다음 세 요소를 연결해 읽는다.

1. `BankService.history`가 입력 날짜를 파싱하고 시작일이 종료일보다 늦으면 거부한다.
2. 시작일은 Asia/Seoul 자정, 종료일은 다음 날 자정으로 바꾼다.
3. mapper XML이 값이 있는 조건만 추가해 `occurred_at >= fromInclusive`, `occurred_at < toExclusive`를 만든다.

예를 들어 2026-10-25 하루를 선택하면 25일 자정은 포함하고 26일 자정은 제외한다. 종료일을 `23:59:59`로 만들어 소수 초 거래를 놓치는 방식을 피한다. `<where>`는 조건이 있을 때 WHERE를 붙이고 시작 AND/OR를 정리하지만 잘못된 업무 날짜나 소유권을 검사하지는 않는다. 시작·종료 모두 없음, 시작만 있음, 종료만 있음, 둘 다 있음의 네 경로를 확인한다. [MyBatis 동적 SQL](https://mybatis.org/mybatis-3/dynamic-sql.html)

## Spring 트랜잭션을 유지하며 이식하기

현재 [root-context.xml](../../modules/03-spring-mvc/src/main/resources/root-context.xml)은 하나의 DataSource를 SqlSessionFactoryBean과 DataSourceTransactionManager에 연결한다. mapper scanner가 인터페이스 프록시를 등록하고 서비스는 생성자로 `BankMapper`를 주입받는다. 관리되는 mapper에서 임의로 `SqlSession.commit()`이나 `close()`를 호출하지 않는다. [MyBatis-Spring 트랜잭션](https://mybatis.org/spring/transactions.html)

`@Transactional` 메서드를 새 객체로 직접 생성해 호출하면 Spring 프록시가 적용되지 않는다. 테스트에서 `new BankService(mapper)`를 쓸 때는 업무 분기만 검증하고 트랜잭션 AOP까지 검증했다고 말하지 않는다. 같은 클래스 안의 메서드 호출도 프록시 경계를 우회할 수 있으므로 annotation 유무만 보고 commit 범위를 판단하지 않는다. [Spring 선언적 트랜잭션](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

현재 [BankMapperIT](../../modules/03-spring-mvc/src/test/java/kr/banklearning/mvc/mapper/BankMapperIT.java)는 실제 PostgreSQL에서 XML 매핑·CRUD·상태 제한·기간 조건을 검증하는 opt-in 테스트다. 테스트가 직접 연 트랜잭션에만 데이터를 넣고 rollback하며 Spring proxy/HTTP 렌더링 검증은 별도로 남긴다. 실제 실행하지 않았다면 NOT RUN이다.

## 오래된 매핑 파일을 분석하는 순서

먼저 호출하는 Java 코드와 완전한 statement ID를 찾는다. 입력 DTO·Map의 키를 기록하고, 바인딩과 문자열 치환을 구분한다. 다음으로 생성 가능한 SQL 분기를 나열하고 컬럼/resultMap을 대조한다. 마지막으로 DataSource와 commit/rollback의 소유자를 확인한다. 한 statement를 옮긴 뒤 빈 결과·NULL·잘못된 날짜·거래 경계 시각·DB 예외를 검증하고 범위를 넓힌다.

정지 고객의 신규 계좌 개설 제한은 현재 `BankService.createAccount`의 업무 규칙이다. 화면 버튼 숨김이나 XML 조건만으로 대체하지 않는다. 현재 구현의 확인 후 INSERT와 동시에 다른 요청이 고객 상태를 바꾸는 경쟁 상황은 별도 잠금/격리 정책과 실제 DB 테스트가 필요한 경계이며, 순차 호출 테스트 통과가 동시성 보장을 뜻하지 않는다.
