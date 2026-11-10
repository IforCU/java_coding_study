# 개발환경 문제 해결

## Docker 엔진 named pipe를 찾을 수 없음

확인한 증상은 `docker version`에서 클라이언트 정보만 나오고 `docker_engine` 또는 `dockerDesktopLinuxEngine`에 연결할 수 없다는 오류입니다. Docker Desktop 설치만으로 엔진이 실행되는 것은 아닙니다.

최소 조치:

1. 사용자가 Docker Desktop을 실행하고 Linux 엔진이 준비될 때까지 기다립니다.
2. 프로젝트 루트에서 `docker info`가 Server 정보와 `OSType: linux`를 반환하는지 확인합니다.
3. `scripts/doctor.ps1` 또는 `scripts/doctor.sh`를 실행합니다.
4. README의 실행과 `verify` 재시작 검증을 실행합니다.

이 저장소의 스크립트는 Docker 설치, WSL/가상화 설정, Windows 서비스 설정을 바꾸지 않습니다. 엔진이 준비되지 않으면 Phase 0을 완료 처리하지 않습니다.

## Docker config 접근 거부

초기 에이전트 실행 환경에서 사용자 Docker 설정 파일의 접근 거부 경고도 관찰했습니다. 이 경고와 엔진 named pipe 부재는 별도 문제입니다. 일반 사용자 터미널에서 `docker info`와 `docker compose ...`를 먼저 확인하세요. 설정 파일의 권한이나 내용을 자동 수정하지 않습니다. 이번 검증에서는 프로세스의 `DOCKER_CONFIG`를 프로젝트 `.cache/docker`로 지정하고 승인된 Docker 실행 권한을 사용했습니다. 사용자 전역 설정은 변경하지 않았습니다.

## 호스트 Maven 없음

정상적인 Docker 경로에서는 문제가 아닙니다. `test` 프로필과 다단계 Dockerfile이 Maven을 제공합니다. 작업 중 추가 검증용 Maven을 내려받는 경우에도 프로젝트 내부 `.tools`와 `.cache`만 사용하며 전역 설치하지 않습니다. 해당 로컬 도구는 기본 실행에 필요하지 않고 버전 관리에서 제외합니다.

## 포트 충돌

`.env`의 해당 서비스 포트를 비어 있는 포트로 바꿔 다시 실행합니다. 예를 들어 DB는 `POSTGRES_PORT`, 이체 앱은 `TRANSACTION_PORT`입니다. 포트는 계속 `127.0.0.1`에만 바인딩됩니다. 자동 검증은 Compose에 실제 공개된 앱 포트를 조회합니다. 이번 실행에서는 기존 5432/8084 서비스를 보존하고 DB 15432, 이체 앱 18084를 사용했습니다.

## DB 인증 실패 또는 준비 상태 503

먼저 `docker compose --profile phase0 logs db migrate legacy`를 확인합니다. 기존 DB 볼륨이 있으면 `.env` 변경으로 계정·암호·DB 이름이 자동 갱신되지 않습니다. 기존 값을 확인하고 맞추세요. 데이터 삭제로 해결하는 명령은 자동 실행하지 않습니다.

`migrate`가 실패하면 앱은 시작되지 않습니다. `/health/live`만 UP이고 `/health/ready`가 503이면 DB 접속·migration 완료·스키마 버전을 구분해 확인합니다. HTTP 응답에는 인증정보나 내부 SQL 예외를 노출하지 않습니다.

## 의존성 다운로드 실패

이미지 pull과 Maven 첫 빌드에는 인터넷 연결이 필요합니다. 네트워크/프록시/실행 권한 문제는 컴파일 오류와 구분합니다. 제한을 이유로 테스트를 생략하고 PASS로 기록하지 않습니다.

## Boot 패키징 후 통합 테스트의 ClassNotFoundException

커스텀 parent POM을 사용하는 Boot 모듈은 `package` 단계에서 실행 JAR로 재패키징됩니다. Failsafe가 이 JAR을 일반 클래스 경로로 읽으면 `BOOT-INF/classes`의 클래스를 찾지 못합니다. 이 프로젝트는 Boot 모듈의 Failsafe `classesDirectory`를 `${project.build.outputDirectory}`로 지정하여 컴파일된 클래스로 실제 DB 통합 테스트를 실행합니다. [Spring Boot 공식 안내](https://docs.spring.io/spring-boot/maven-plugin/integration-tests.html)에 설명된 설정입니다.
