# Phase 6 — 기존 업무를 보존하는 조회 REST API

직접 작성할 화면 과제는 [단계별 JSTL 실습](jstl/lesson.md)에 있습니다. 기존 계좌 목록 문제를 반복하지 않고 이 단계의 표시·상태·오류 분기를 연습합니다.

정상 모듈은 `modules/06-spring-boot`입니다. 기존 JSP/WAR를 유지하면서 별도 Spring Boot 애플리케이션으로 고객·계좌·거래·한도·통계를 조회합니다. 이 API는 Phase 4 데이터의 조회자이며 이체와 원장 변경은 기존 거래 모듈이 소유합니다.

## Boot와 기존 MVC 비교

Spring Boot도 Controller, Service, Repository와 Spring MVC 요청 흐름을 사용합니다. 실행 진입점 `BankApiApplication`과 자동 설정, 내장 서버, 실행 JAR로 구성·실행 방식을 정리합니다. Java 17과 Boot 4.1.2를 기준으로 하며 Jackson 3 JSON 매핑을 사용합니다. [Boot 실행 요구사항](https://docs.spring.io/spring-boot/system-requirements.html), [Boot JSON 설정](https://docs.spring.io/spring-boot/reference/features/json.html)

| 관점 | 기존 JSP/WAR | 조회 API |
| --- | --- | --- |
| 표현 | JSP view와 HTML | DTO를 JSON으로 직렬화 |
| 실행 | 외부 Tomcat에 WAR 배포 | 내장 서버를 포함한 실행 JAR |
| 입력 | 폼과 request parameter | 경로·query parameter와 검증 |
| 결과 | forward/redirect | HTTP 상태와 표준 JSON envelope |
| 인증 | 폼 세션 또는 Basic | 요청마다 Basic, stateless |
| DB 책임 | Phase 4가 이체·원장 변경 | 제한된 DB 역할로 SELECT만 수행 |

## REST, DTO와 JSON

조회 대상은 경로의 리소스 ID로 표현하고 날짜·건수는 query parameter로 받습니다. DB 행이나 내부 예외를 그대로 노출하지 않고 DTO로 응답 필드를 고정합니다. 성공과 실패는 같은 envelope의 `success`, `data`, `error`, `requestId`로 전달합니다. HTTP 상태와 오류 코드도 함께 확인합니다.

금액은 Java BigDecimal로 읽고 현재 JSON 계약에서는 number로 전달합니다. 서버의 정확한 정수 원화 값과 클라이언트 숫자 파싱 정밀도를 구분해야 합니다. 큰 정수를 JavaScript Number로 읽을 때의 손실 가능성도 계약에서 확인합니다. 필드·경로·예시는 [API 계약](../../docs/architecture/api-contract.md)에 있습니다.

## 검증과 공통 오류

Bean Validation은 조회 건수 등 입력 경계의 제약을 선언합니다. Controller Advice는 형식·검증·업무·저장소 오류를 안전한 오류 코드와 requestId로 바꿉니다. 인증·인가 필터에서 거절된 요청도 같은 응답 형식으로 추적할 수 있어야 합니다. [Spring MVC 입력 검증](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)

USER는 자신의 고객·계좌 범위만 조회하고 ADMIN은 관리자 통계를 포함한 전체 조회 범위를 가집니다. 사용자 입력의 고객 ID만으로 소유 범위를 정하지 않습니다.

## 기존 DB와 함께 쓰기

API의 DB 계정은 `bank_learning_api`이며 SELECT 권한만 부여합니다. 애플리케이션의 readOnly 선언과 DB 권한은 서로 다른 경계입니다. GET 경로에서 원장·계좌·일일 사용액을 바꾸지 않으며, 조회를 위해 기존 서비스의 쓰기 권한을 공유하지 않습니다.

여러 번의 SELECT로 한 응답을 만든다면 같은 시점의 데이터를 읽는지 확인해야 합니다. `readOnly=true`만으로 모든 격리 수준에서 하나의 스냅샷이 보장되지는 않습니다. 한도 조회는 REPEATABLE_READ를 명시해 계좌 한도와 일일 사용액을 같은 스냅샷에서 읽도록 구성합니다. 통계는 하나의 집계 SQL입니다. 실제 PostgreSQL 검증의 최신 결과는 루트 `PROGRESS.md`를 확인합니다. [레거시/API DB 소유권](../../docs/architecture/legacy-api-ownership.md)

## JUnit과 Mockito의 역할

Mockito로 저장소를 대체하면 입력·소유권·기간 변환·오류 분기를 DB 없이 재현할 수 있습니다. 호출이 금지된 조건에서 저장소를 호출하지 않는지도 확인합니다. 실제 JDBC 매핑·DB 권한·시각 범위·동시 상태는 PostgreSQL IT에서 따로 검증합니다.

소스는 `web.BankController` → `service.BankReadService` → `repository.JdbcBankReadRepository` → `dto.BankViews`와 `ApiEnvelope` 순서로 읽습니다. 오류는 `web.ApiErrorAdvice`, 인증과 역할은 `config.SecurityConfig`에서 연결합니다.

실습은 [exercise.md](exercise.md)의 최대 반환 건수 정책입니다. 기본 API는 완성된 별도 코드로 실행되고 TODO는 선택 프로필에서만 테스트합니다.
