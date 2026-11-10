# Java 17의 Spring MVC·MyBatis 실행 조합

검토일: 2026-10-25. 공식 프로젝트 릴리스와 Maven Central 배포 메타데이터에서 확인한 안정 버전을 사용한다. 최신 metadata의 latest/release가 RC를 가리킬 수 있으므로 RC·M·SNAPSHOT을 제외해 선택했다. Docker 이미지 목록 확인과 실제 pull·실행 성공은 별도다.

| 대상 | Phase 3 선택 | 호환성 근거 |
| --- | --- | --- |
| Java | 17 | Spring 7과 Tomcat 11의 최소 실행 기준 충족 |
| Spring Framework/MVC | 7.0.10 | [공식 안정 릴리스](https://github.com/spring-projects/spring-framework/releases/tag/v7.0.10), Java 17 및 Jakarta EE 11 기준 |
| Servlet API | 6.1.0, provided | [Jakarta Servlet 6.1](https://jakarta.ee/specifications/servlet/6.1/)은 Java 17 이상 |
| Tomcat | `11.0.27-jdk17-temurin-noble` | [Docker 공식 태그 목록](https://github.com/docker-library/official-images/blob/master/library/tomcat), [Tomcat 11 호환성](https://tomcat.apache.org/migration-11.0.html) |
| MyBatis | 3.6.0 | [공식 릴리스](https://github.com/mybatis/mybatis-3/releases/tag/mybatis-3.6.0), Java 11 이상으로 Java 17에서 사용 가능 |
| MyBatis-Spring | 4.1.0 | [공식 릴리스](https://github.com/mybatis/spring/releases/tag/mybatis-spring-4.1.0), Java 17·Spring 7 조합 |
| HikariCP | 7.1.0 | [공식 Maven 좌표와 Java 11+ 요건](https://github.com/brettwooldridge/HikariCP#artifacts) |
| 테스트 | JUnit Jupiter 6.1.3 | [JUnit Java 17 요건](https://docs.junit.org/6.1.3/overview.html), Spring 7의 SpringExtension은 Jupiter 6.0+ 필수 |

Spring 7은 Servlet 6.1 기준이므로 Phase 0/2의 Tomcat 10.1·Servlet 6.0 WAR 실행 환경을 그대로 재사용하지 않는다. Phase 2 모듈을 보존하고 Phase 3에 별도 WAR와 컨테이너 구성을 둔다. Tomcat 11은 Servlet 6.1·JSP 4.0을 제공한다. [Spring 버전 정책](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions), [Tomcat 버전 표](https://tomcat.apache.org/whichversion.html)

MyBatis-Spring 공식 표에서 3.0 계열은 Spring 6.x용이고 4.0 계열부터 Spring 7용이다. 선택한 4.1.0의 배포 POM은 Java 17과 Spring 7.0.8을 기준으로 빌드되며 현재 프로젝트는 Spring 7.0.10을 명시한다. 버전 숫자를 맞춰 보이게 하려고 MyBatis 3과 MyBatis-Spring 3을 묶으면 안 된다. [공식 호환성 표](https://mybatis.org/spring/), [4.1.0 릴리스 POM](https://raw.githubusercontent.com/mybatis/spring/mybatis-spring-4.1.0/pom.xml)

Spring 7의 `SpringExtension`은 JUnit Jupiter 6.0 이상을 요구한다. 기존 Jupiter 5 테스트 코드와 annotation 이름이 비슷해도 플랫폼 의존성이 5로 남으면 Spring 7 통합 테스트를 지원하는 조합이 아니다. 현재 공통 BOM은 6.1.3으로 정렬하며 Java 17에서 테스트한다. [Spring 테스트 최소 버전](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/support-classes.html)

## XML과 annotation이 맡는 역할

```text
WebInitializer
 ├─ RootConfig @ImportResource("root-context.xml")
 │   ├─ PoolFactory → Hikari DataSource
 │   ├─ SqlSessionFactoryBean → BankMapper.xml
 │   ├─ MapperScannerConfigurer → BankMapper 프록시
 │   ├─ DataSourceTransactionManager → 같은 DataSource
 │   └─ component-scan → @Service BankService
 └─ WebConfig → Spring MVC Controller·JSP view 설정

HTTP → DispatcherServlet → BankController
                           → Spring이 주입한 BankService
                           → BankMapper 프록시 → JDBC → PostgreSQL phase3
```

XML은 DataSource·mapper·트랜잭션 관리자 연결을 눈으로 추적하게 하고 annotation은 Java 클래스의 역할과 트랜잭션 경계를 표현한다. 같은 bean을 XML과 Java 설정에서 중복 등록하지 않는다. 업무 코드가 DataSource 비밀번호를 알 필요는 없다. 풀 생성과 종료는 인프라 설정에서 관리한다. [현재 root-context.xml](../../modules/03-spring-mvc/src/main/resources/root-context.xml), [RootConfig](../../modules/03-spring-mvc/src/main/java/kr/banklearning/mvc/config/RootConfig.java)

SqlSessionFactory와 트랜잭션 관리자가 같은 DataSource를 사용해야 하나의 Spring 트랜잭션에 참여한다. mapper가 매번 별도 수동 Connection을 열거나 서비스가 SqlSession을 임의로 commit하면 이 경계를 깨뜨릴 수 있다. [MyBatis-Spring 트랜잭션 문서](https://mybatis.org/spring/transactions.html)

## javax·jakarta와 WAR 패키징

Servlet API는 `jakarta.servlet`을 사용하며 WAS가 제공하므로 dependency scope는 provided다. `javax.servlet` JAR를 함께 넣지 않는다. JDBC의 `java.sql`·`javax.sql`은 Java SE API이므로 이름을 바꾸지 않는다. Jakarta Tags/JSTL은 JSP의 태그 실행을 위해 API와 구현체를 WAR에 포함한다. [Tomcat namespace migration](https://tomcat.apache.org/migration-10.html), [Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/)

로컬 WAR 빌드 뒤에는 Servlet API가 WAR 안에 중복되지 않는지, Spring·MyBatis·pgJDBC·JSTL 구현체가 포함되는지 확인한다. 의존성 해결 성공, WAR 패키징, 실제 Tomcat 배포, JSP 렌더링과 DB 연결은 각각 다른 검증이다.

## 후속 Security·Boot 모듈에서 유지할 기준

Spring Security의 확인된 안정판은 7.1.2이고 Java 17 이상을 요구한다. 인증·인가와 세션 보호는 별도 Phase 5 모듈에서 검증한다. [Security 릴리스](https://github.com/spring-projects/spring-security/releases/tag/7.1.2), [Java 요구사항](https://docs.spring.io/spring-security/reference/prerequisites.html)

Spring Boot의 확인된 안정판은 4.1.2이며 Java 17, Spring Framework 7.0.10 이상, Servlet 6.1/Tomcat 11을 요구한다. Phase 6에서는 Boot BOM이 관리하는 조합을 우선하고 수동 MVC WAR와 실행·의존성 경계를 분리한다. [Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)

Boot 4.1.2 BOM은 JUnit 6.0.3, Mockito 5.23.0, HikariCP 7.0.2를 관리한다. 독립 최신 안정판 JUnit 6.1.3·Mockito 5.24.0·HikariCP 7.1.0과 숫자가 달라도 오류가 아니다. 공통 parent의 dependencyManagement가 Boot BOM을 가리지 않는지 effective POM에서 확인하고 선택한 조합을 기록한다. [Boot 관리 좌표](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)

## 검증 상태의 구분

로컬 단위 테스트, XML 파싱/패키징, 실제 컨테이너·DB 실행의 최신 결과는 [PROGRESS.md](../../PROGRESS.md)에서 확인한다. 실제 DB용 `BankMapperIT`는 별도 opt-in이며 필수 DB 환경변수가 없으면 실패한다. MyBatis XML과 실제 PostgreSQL 타입 매핑·기간 경계를 검증하지만 Spring transaction proxy나 JSP 렌더링까지 검증하는 테스트는 아니다. 컨테이너 빌드·DB·HTTP·재시작 검증을 실행하지 못했다면 NOT RUN으로 남긴다.
