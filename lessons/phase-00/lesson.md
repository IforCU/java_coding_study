# Phase 0 — Java 웹 애플리케이션을 실행하는 환경

이번 단계에서는 은행 업무 기능을 만들기 전에 브라우저 요청이 Java와 DB까지 도달하는 최소 환경을 익힙니다. 정상 실행 기준 모듈은 `modules/00-bootstrap`이며, 학습자가 수정하는 SQL은 `exercises/starter/phase-00`에 따로 있습니다.

이 문서와 검증 명령은 학습을 위한 설명·재현 절차입니다. 로컬 테스트, Docker 실행, 실제 DB 연결과 재시작의 최신 검증 결과는 루트 `PROGRESS.md`를 확인합니다.

학습을 마치면 다음 내용을 설명할 수 있어야 합니다.

- Maven이 Java 소스와 JSP를 WAR로 묶는 과정
- Tomcat이 WAR를 실행하고 HTTP 요청을 Servlet과 JSP에 전달하는 과정
- Compose의 서비스 이름으로 Java 컨테이너가 DB를 찾는 원리
- 프로세스가 응답하는 상태와 DB를 사용할 수 있는 상태의 차이
- 컨테이너를 다시 만들어도 DB 볼륨이 유지되는 이유

## 실행 단위 이해

Java 소스는 바로 웹 서버가 되지 않습니다. Maven 빌드가 클래스와 웹 리소스를 WAR 파일로 묶고, Tomcat이 이를 배포합니다. 이 프로젝트의 WAR는 루트 경로에 배포되어 `http://127.0.0.1:8080/`에서 Hello World JSP를 제공합니다.

JSP는 HTML 응답을 만드는 화면 계층입니다. DB 연결 정보를 읽거나 SQL을 실행하는 역할은 Java 클래스에 둡니다. 이 단계에서는 화면과 상태 점검을 분리해 다음 세 요청을 관찰합니다.

| 요청 | 확인하는 것 | DB 의존성 |
| --- | --- | --- |
| `GET /` | Tomcat의 JSP 실행과 HTML 응답 | 없음 |
| `GET /health/live` | Java 애플리케이션의 응답 가능 여부 | 없음 |
| `GET /health/ready` | 설정된 DB 연결과 초기 메타데이터 조회 | 있음 |

DB가 중지되더라도 live는 응답할 수 있습니다. ready는 DB에서 기대하는 환경과 스키마 버전을 확인하지 못하면 준비되지 않은 상태를 반환합니다. 단순한 TCP 연결 성공만으로 애플리케이션이 사용할 테이블까지 준비되었다고 판단할 수는 없습니다.

## 컨테이너와 네트워크

`compose.yaml`이 `db`, `migrate`, `legacy` 서비스를 관리합니다. DB가 건강한 상태가 되면 마이그레이션이 실행되고, 준비된 스키마를 사용해 웹 애플리케이션이 시작하도록 의존성을 둡니다.

브라우저는 호스트의 `127.0.0.1`로 요청합니다. Java 컨테이너는 Compose 내부 네트워크의 서비스 이름 `db`로 DB에 연결합니다. Java 컨테이너 안의 `localhost`는 그 Java 컨테이너 자신이므로 DB 주소로 사용하지 않습니다. 외부에 공개하는 포트는 로컬 주소에만 바인딩합니다.

기본 HTTP 포트는 8080, PostgreSQL 포트는 5432입니다. 다른 로컬 프로그램과 충돌하면 루트 `.env`에서 `LEGACY_PORT`, `POSTGRES_PORT`를 바꿉니다. DB 컨테이너 간 통신 주소와 호스트에 공개한 포트는 구분해서 읽습니다.

DB 데이터는 컨테이너의 임시 파일 계층 대신 이름 있는 볼륨에 저장합니다. `docker compose --profile phase0 down`은 컨테이너와 네트워크를 정리하며 DB 볼륨을 유지합니다. 데이터 삭제나 볼륨 강제 초기화는 이 학습 절차에 포함하지 않습니다.

## Java와 Tomcat 호환성

공통 기준은 Java 17입니다. Phase 0은 Tomcat 10.1과 Jakarta Servlet 6.0, Jakarta Pages(JSP) 3.1을 사용합니다. Tomcat 공식 호환성 표에서 10.1은 Java 11 이상을 지원하므로 Java 17과 조합할 수 있습니다. [Tomcat 버전별 지원 사양](https://tomcat.apache.org/whichversion.html)

Java 8/11 레거시 프로젝트를 분석할 때는 Java 버전과 웹 API 버전을 따로 확인합니다. Java 8에서 컴파일한 코드를 Java 17로 바꾸는 일과, `javax.servlet` 코드를 `jakarta.servlet`로 옮기는 일은 서로 다른 호환성 작업입니다. Tomcat 9에서 10으로 넘어가며 Servlet API의 패키지가 바뀌었고 재컴파일이 필요합니다. 이 WAR에는 `jakarta.servlet` 계열만 사용합니다. [Tomcat 10 마이그레이션 가이드](https://tomcat.apache.org/migration-10.html)

## 소스 읽는 순서

1. 루트 `compose.yaml`에서 이미지, 빌드 경로, 환경변수, 의존성과 볼륨을 찾습니다.
2. `modules/00-bootstrap/pom.xml`에서 Java 릴리스와 WAR 패키징, Servlet API 의존성을 확인합니다.
3. `BootstrapServlet`에서 두 health 요청을 찾고, live와 ready의 차이를 비교합니다.
4. `AppConfig` → `JdbcConnectionFactory` → `DatabaseProbe`로 DB 연결과 조회를 따라갑니다.
5. `database/migrations/V001__bootstrap.sql`과 `database/seed/R001__bootstrap.sql`에서 테이블 구조와 학습용 데이터를 확인합니다.

서비스 시작과 검증은 [verification.md](verification.md), 직접 작성할 과제는 [exercise.md](exercise.md)에 있습니다. 이 문서가 존재하는 것만으로 실행 검증이 완료된 것은 아닙니다. 실제 판정은 루트 `PROGRESS.md`를 확인합니다.
