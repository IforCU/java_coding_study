# Phase 0 아키텍처

이 문서는 Phase 0 WAR의 최소 실행·DB 연결 경로를 설명합니다. 현재 저장소에는 Phase 0~6 코드가 있으며 공통 `migrate` 서비스는 V001~V006과 R001~R005를 순서대로 처리합니다. 아래 bootstrap 흐름은 그중 Phase 0의 범위입니다. 실제 Docker/DB 실행 검증 상태는 [PROGRESS.md](../../PROGRESS.md)에 따로 기록합니다.

```text
브라우저 / 검증 스크립트 (127.0.0.1:8080)
  ├─ GET / → Tomcat JSP 엔진 → index.jsp → HTML
  ├─ GET /health/live → BootstrapServlet → HTTP 200
  └─ GET /health/ready → BootstrapServlet
                         → DatabaseProbe
                         → ConnectionFactory (JdbcConnectionFactory)
                         → JDBC PreparedStatement
                         → PostgreSQL bootstrap.app_metadata
```

`db` health check 이후 공통 `migrate`가 모든 V 파일을 이름순으로 실행한 다음 모든 R 파일을 실행합니다. Phase 0에 필요한 스키마와 가상 메타데이터는 V001/R001이 구성합니다. `phase0` 프로필도 현재 공통 migration 경로를 사용하므로 후속 스키마와 API 조회 역할이 함께 준비됩니다. migration 서비스가 성공적으로 종료한 후에만 `legacy`를 시작합니다. 앱 readiness가 실패하면 컨테이너가 unhealthy가 되며, Compose 검증은 성공으로 보고하지 않습니다. `restart: unless-stopped`는 프로세스 종료 시 재시작 정책이며 unhealthy 자체가 자동 재시작을 뜻하지 않습니다.

DB 연결은 짧은 probe마다 열고 try-with-resources로 닫습니다. 연결·소켓·쿼리 timeout을 제한합니다. `PGSimpleDataSource`로 WAR의 드라이버를 초기화하고 Servlet 종료 시 등록을 해제합니다. Tomcat의 부모 class loader에서 JDBC 검색이 먼저 끝날 수 있어 DriverManager 자동 검색에만 의존하지 않습니다. [Tomcat 공식 설명](https://tomcat.apache.org/tomcat-10.1-doc/jndi-datasource-examples-howto.html#DriverManager,_the_service_provider_mechanism_and_memory_leaks)

Phase 0은 최소 연결 확인 예제입니다. `PGSimpleDataSource` 자체는 연결 풀이 아니며 이 WAR에는 고객 입력이나 금융 거래가 없습니다. 별도 Phase 2 WAR는 Hikari 연결 풀과 Servlet·서비스·DAO 계층을 구현합니다. [Phase 2 호출 흐름](../legacy-analysis/servlet-flow.md)

메타데이터와 영속성 검사 marker는 `bootstrap` 스키마에 둡니다. 후속 모듈은 `phase2`, `phase3`, `phase4`, `phase5`를 사용하고 보안 장애 실습은 `phase5_lab`으로 격리합니다. Phase 6은 Phase 4 데이터를 별도 reader 역할로 조회하며 업무 테이블을 새로 만들지 않습니다. Phase 0의 접속은 Docker 공식 이미지가 초기화하는 로컬 학습 계정을 사용합니다. V006은 API 실행용 `bank_learning_api` 역할을 따로 생성하고 명시적인 SELECT 권한을 부여합니다. [API 데이터 소유권](legacy-api-ownership.md)

## 마이그레이션과 영속성

- `V001__bootstrap.sql`은 Phase 0의 스키마·테이블과 버전 1 적용 이력을 생성합니다.
- `R001__bootstrap.sql`은 Phase 0 샘플 메타데이터 한 행을 최초 한 번만 추가합니다. 기존 행은 덮어쓰지 않습니다. `bootstrap.app_metadata.schema_version=1`은 후속 migration 뒤에도 Phase 0 readiness 계약으로 유지합니다.
- V002는 Servlet/JDBC, V003은 Spring MVC/MyBatis, V004는 이체·원장, V005는 보안·격리 실습 스키마를 추가합니다. V006은 API 조회 역할과 접근 권한을 구성합니다.
- 모든 migration/seed는 같은 advisory lock `7100000`을 사용하고 파일별 트랜잭션으로 실행합니다. R002/R003은 새 계좌를 만든 경우에만 최초 입금 이력을 추가하며 R004의 opening balance와 R005의 가상 프로필도 기존 값을 덮어쓰지 않습니다. API는 조회 전용이므로 R006은 없습니다.
- `bootstrap.schema_history`는 현재 V001~V006의 적용 버전을 기록하는 최소 장치입니다. 파일 checksum 기반 migration 엔진은 아닙니다.
- 기존 V001의 의미를 유지한 채 후속 V 파일을 추가했습니다. `CREATE TABLE IF NOT EXISTS`는 기존의 다른 컬럼·제약을 자동 수정하지 않으므로 예전 스키마가 현재 요구사항과 다르면 조회/readiness 또는 통합 검증이 실패할 수 있습니다. 데이터 초기화로 이를 숨기지 않습니다.
- PostgreSQL 볼륨은 프로젝트별 Compose 볼륨입니다. 일반 `down`은 볼륨을 삭제하지 않습니다.

이미지의 패키지 버전은 고정했지만 이미지 digest까지 고정하지는 않았습니다. 검증한 뒤 digest 고정 여부를 검토할 수 있습니다. 실제 pull/build 가능 여부는 Docker 실행 검증과 별도로 기록합니다.
