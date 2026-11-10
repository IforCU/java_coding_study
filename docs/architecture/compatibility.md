# 기술 선택과 호환성

검토일: 2026-10-25. 아래 버전은 공식 문서·배포 목록에서 확인한 선택이며 실제 이미지 pull 성공을 의미하지 않습니다. 후속 Phase 진입 시 지원 상태를 다시 확인합니다.

| 대상 | Phase 0 선택 | 이유와 공식 근거 |
| --- | --- | --- |
| Java | 17 | 요청의 공통 기준. 최신 보안 패치가 반영된 컨테이너 JDK 사용 |
| 빌드 | Maven `3.10.0-eclipse-temurin-17-noble` | 호스트 전역 설치 없이 [공식 Maven 이미지](https://hub.docker.com/_/maven)에서 빌드 |
| WAS | Tomcat `10.1.61-jdk17-temurin-noble` | Java 17에서 Servlet 6.0/JSP 3.1을 지원하는 [공식 Tomcat 이미지](https://hub.docker.com/_/tomcat), [버전 호환표](https://tomcat.apache.org/whichversion.html) |
| Servlet | Jakarta Servlet API 6.0.0, `provided` | API는 WAS가 제공. [공식 Servlet 6.0 명세](https://jakarta.ee/specifications/servlet/6.0/) |
| DB | PostgreSQL `17.11-bookworm` | [공식 이미지](https://hub.docker.com/_/postgres)와 [지원 정책](https://www.postgresql.org/support/versioning/) 확인 |
| JDBC | pgJDBC 42.7.14 | [공식 pgJDBC 배포](https://jdbc.postgresql.org/) |
| 테스트 | JUnit Jupiter 6.1.3 | Spring 7 테스트 요구사항을 충족하도록 전환하고 Phase 0~2 회귀 테스트도 통과. [공식 JUnit 문서](https://docs.junit.org/6.1.3/overview.html) |
| Maven plugins | Compiler 3.16.0 / WAR 3.5.1 / Surefire 3.6.0 | [Apache 플러그인 목록](https://maven.apache.org/plugins/)과 [Surefire 문서](https://maven.apache.org/surefire/maven-surefire-plugin/) 확인 |

## javax와 jakarta

Tomcat 9의 Servlet API는 `javax.servlet`, Tomcat 10 이상은 `jakarta.servlet`을 사용합니다. 이름만 바꿔서 두 API를 같은 WAR에 넣으면 호환되는 것이 아닙니다. 현재 기준 앱은 `jakarta.servlet`만 의존하며 Servlet API JAR는 WAR에 포함하지 않습니다. JDBC의 `java.sql`과 `javax.sql`은 Java SE API이므로 같은 방식으로 이름을 바꾸지 않습니다. [Tomcat migration guide](https://tomcat.apache.org/migration-10.html)

교육에서 말하는 레거시는 Servlet/JSP/JDBC의 직접적인 실행 구조와 유지보수 방식도 포함합니다. 따라서 Phase 0의 실행 기준은 유지보수되는 Jakarta 경로로 두고, 과거 `javax` 소스 분석은 별도 예제로 분리합니다. Tomcat 9.0은 2027-03-31 지원 종료 공지가 있어 후속 실습에 무조건 고정하지 않습니다. [Tomcat 9 종료 안내](https://tomcat.apache.org/tomcat-9.0.x-eos.html)

Phase 0의 Hello World에는 JSTL이 필요하지 않아 의존성을 추가하지 않았습니다. Phase 2~5 WAR에는 Jakarta Tags API 3.0.2와 구현체 3.0.1을 함께 넣고 `jakarta.tags.core` URI를 사용합니다. [Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/)

## Spring 모듈

Spring MVC와 Boot는 이전 Tomcat WAR와 독립된 애플리케이션입니다. Spring 7.0.10/MyBatis-Spring 4.1.0, Tomcat 11.0.27과 Boot 4.1.2를 사용합니다. [상세 호환성](spring-compatibility.md), 공식 [Spring 호환표](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-Versions)와 [Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)을 확인했습니다. 실제 이미지 빌드·실행 검증은 별도입니다.

## Java 8/11과 비교할 때

현재 `AppConfig` 등은 일반 클래스를 사용하지만 빌드 목표는 Java 17입니다. `HealthCheck`의 `java.net.http.HttpClient`는 Java 11부터 제공되므로 Java 8에서 그대로 컴파일되지 않습니다. Java 17의 `.class`는 Java 8/11 JVM에서 실행할 수 없습니다. 구형 실행환경으로 바꾸려면 소스·API·의존성·WAS를 함께 검토해야 합니다.

도메인 조회 DTO에는 record도 사용합니다. record는 Java 16부터 정식 기능이므로 Java 8/11로 소스를 옮길 때는 일반 클래스/JavaBean으로 바꾸고 호출부·매핑·테스트를 함께 검토해야 합니다.
