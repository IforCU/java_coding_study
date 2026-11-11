# 실행과 검증

명령은 저장소 루트, Windows PowerShell 기준입니다. Linux/macOS에서는 `.\mvnw.cmd` 대신 `sh ./mvnw`를 사용합니다. JDK 17이 기준이며, Wrapper 최초 실행과 새 의존성 다운로드에는 인터넷이 필요합니다. 아래 기대 결과는 실행 기록이 아니며 최신 결과는 루트 `PROGRESS.md`에서 확인합니다.

## 1. 기본 화면 빌드

```powershell
.\mvnw.cmd -Pjstl-lab -pl exercises/jstl-bank-lab -am verify
```

기대 결과: JSP 실제 컴파일·HTTP 응답과 CSS, WEB-INF 직접 접근 차단 검사 2개 통과. 완성 동작 검사 5개는 이 단계에서 건너뜁니다. starter의 TODO가 남아 있어도 실행 준비는 가능해야 합니다. `exercises/jstl-bank-lab/target/jstl-bank-lab-starter.war`가 생성됩니다.

## 2. 브라우저 실습

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml test-compile exec:java
```

`http://127.0.0.1:8087/accounts`에서 TODO를 작성하며 결과를 봅니다. Ctrl+C로 종료합니다. 이 실행 방식은 소스 JSP 변경을 읽으며 WAR 재패키징이 필요하지 않습니다. [과제의 시나리오](exercise.md)를 모두 관찰하세요.

## 3. 완성 계약 검사

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-check verify
```

기대 결과: 수정 전 starter는 컴파일이 성공해도 동작 검사 5개가 실패합니다. 완성 후에는 총 7개 검사가 실패·오류·생략 없이 통과해야 합니다. 전체/종류별 목록, 순번, 상태, 금액, 원본 빈 목록, 필터 0건, 공백 이름, HTML 이스케이프, 생성 링크를 실제 내장 Tomcat HTTP 응답으로 검사합니다. 결과는 해당 모듈 `target/surefire-reports`에 기록됩니다. 테스트 자체가 '미완성은 정상'이라며 완성 계약을 PASS 처리하지 않습니다.

## 4. Docker 대체 실행

```powershell
docker compose --profile jstl-lab config --quiet
docker compose --profile jstl-lab up --build -d jstl-lab
docker compose --profile jstl-lab ps -a
```

접속 주소는 기본 `http://127.0.0.1:8087/accounts`이며 `JSTL_LAB_PORT`로 변경할 수 있습니다. 로컬 미리보기와 동시에 같은 포트를 사용하지 마세요. 이 프로필은 starter만 포함하고 정답 소스는 이미지에 복사하지 않습니다. Docker 방식에서는 JSP 수정 후 빌드를 다시 실행합니다.

종료할 때는 실습 서비스만 중지합니다.

```powershell
docker compose --profile jstl-lab stop jstl-lab
```

Docker 실행을 하지 않았다면 NOT RUN으로 기록합니다. 로컬 내장 Tomcat 검증으로 Docker 이미지 실행을 PASS 처리하지 않습니다. 기존 Phase 2의 PostgreSQL/JDBC 검증도 이 화면 실습과 별개입니다.
