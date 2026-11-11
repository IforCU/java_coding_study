# Phase 4 화면 검증

저장소 루트, Windows PowerShell 기준입니다. Linux/macOS는 `sh ./mvnw`로 시작합니다. 처음에는 인터넷이 필요하며 의존성과 임시 파일은 프로젝트 .cache에 저장합니다. 실제 실행 결과는 루트 PROGRESS.md에 별도로 기록합니다.

## 준비·컴파일

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-phase4 verify
```

기대 결과: 실제 Tomcat/Jasper 컴파일·HTTP·CSS·WEB-INF 접근 차단 smoke 검사 1개 통과, 완성 계약 4개는 의도적으로 생략됩니다. 화면 틀이 뜬다는 사실만으로 과제 완료가 아닙니다.

## 완성 계약

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml "-Pjstl-phase4,jstl-check" verify
```

PowerShell에서 복수 프로필은 위처럼 따옴표로 묶습니다. TODO 상태는 동작 검사 4개가 실패해야 하며, 완성 후에는 총 5개 검사 모두 실패·오류·생략 없이 통과해야 합니다. 기본 화면, 빈 목록, 이체 상태 분기·요청 ID 링크·금액 표시, 특수문자 이스케이프를 실제 HTTP 응답으로 확인합니다.

로그·XML은 exercises/jstl-bank-lab/target/surefire-reports에 생성됩니다. 프로필별 실행은 같은 모듈의 보고서 경로를 사용하므로 병렬로 실행하지 말고 필요하면 단계별 로그를 .cache에 따로 저장하세요.

## Docker 대체 실행

같은 루트 Compose 서비스를 재사용하고 빌드 단계만 선택합니다. 기본 02 실습이 이미 실행 중이면 같은 서비스를 선택한 starter 이미지로 갱신한다는 점을 확인하세요.

```powershell
docker compose --profile jstl-lab build --build-arg JSTL_PHASE=04 jstl-lab
docker compose --profile jstl-lab up -d --no-build jstl-lab
```

주소는 기본 `http://127.0.0.1:8087/workbench`입니다. 로컬 미리보기와 동시에 같은 포트를 사용하지 마세요. 종료는 `docker compose --profile jstl-lab stop jstl-lab`입니다. 빌드 인자를 매번 주지 않으려면 프로젝트 .env의 `JSTL_LAB_PHASE=04`를 선택할 수도 있습니다. Docker를 실행하지 못하면 NOT RUN으로 남깁니다. 본편의 실제 DB·API·보안 검증을 대신하지 않습니다.
