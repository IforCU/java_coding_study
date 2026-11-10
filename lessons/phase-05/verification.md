# Phase 5 — 보안과 장애 랩 검증

프로젝트 루트 기준입니다. 아래는 재현 절차와 기대 결과이며 실제 실행 기록은 `PROGRESS.md`에서 확인합니다. 단위 테스트, 장애 증상 재현, 실제 Docker·DB·Tomcat 검증 결과를 구분합니다.

## 정상 빌드와 보안 단위 테스트

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -pl modules/05-security-operations -am verify
```

기대 결과: 정상 `security-operations.war`가 생성되고 MockMvc의 인증·역할·CSRF·입력·오류·추적 단위 검증이 통과합니다. test/labs 클래스와 starter·정답은 WAR에 들어가지 않습니다. MockMvc 성공을 실제 Tomcat 쿠키·JSP·PostgreSQL 실행 성공으로 집계하지 않습니다.

## Docker 실행 절차

```sh
docker compose --profile phase5 up --build -d
docker compose --profile phase5 ps -a
```

기본 주소는 `http://127.0.0.1:8085`입니다. 로컬 가상 계정은 [lesson.md](lesson.md)에 있습니다.

브라우저와 HTTP 테스트로 다음을 확인합니다.

1. `/health`는 미인증 상태에서 조회할 수 있습니다.
2. `learner` 로그인 후 `/profile`이 본인 정보를 표시합니다.
3. USER 역할은 `/admin/audit`에 접근할 수 없고 ADMIN은 접근할 수 있습니다.
4. 유효한 CSRF와 note의 정상 입력은 처리되고, 누락·불일치 CSRF는 403입니다.
5. note 길이 경계를 검증하고 화면의 특수문자가 HTML로 실행되지 않도록 출력되는지 확인합니다.
6. 로그인 시 세션 ID 변경, HttpOnly 쿠키, 유휴 만료와 로그아웃을 확인합니다.
7. 허용 추적 ID가 응답에 이어지고 잘못된 헤더는 안전한 새 값으로 대체되는지 확인합니다.
8. 오류 응답과 감사 기록에 비밀번호·세션 토큰·계좌 원문·내부 SQL 오류가 없는지 확인합니다.

세션 만료 검증은 HTTP Basic 헤더 없이 수행합니다. note 입력은 1~100자 계약을 사용하며 실제 처리 결과와 감사 부작용도 확인합니다.

## 격리 장애 랩

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -pl modules/05-security-operations -am -Dtest=MaintenanceLabTest -Dsurefire.failIfNoSpecifiedTests=false test
```

기대 결과: 여덟 의도적인 장애 증상이 fixture에서 관찰됩니다. 이 PASS는 교정 완료가 아닙니다. 개별 증상·과제는 [incidents.md](../../exercises/starter/phase-05/incidents.md)를 확인합니다.

실제 DB가 준비되면 선택 IT를 실행합니다.

```sh
docker compose --profile integration run --build --rm integration-test
```

호스트에서 학습 DB의 `DB_URL`, `DB_USER`, `DB_PASSWORD`가 준비되었다면 해당 모듈만 실행할 수 있습니다.

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -pl modules/05-security-operations -am -Pintegration-tests verify
```

실제 SQL 조건, Hikari 풀 대기·회복, statement timeout, 잘못된 auto-commit 범위를 `phase5_lab` fixture에서 검증합니다. DB 환경이 없으면 실패 원인 또는 `NOT RUN`을 기록하고 자동 생략을 PASS로 집계하지 않습니다. 자세한 범위는 [장애 랩 안내](../../docs/troubleshooting/maintenance-labs.md)를 확인합니다.

## 선택 마스킹 실습

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -Pexercise-phase5 -pl exercises/starter/phase-05 -am verify
```

TODO는 실패가 기대됩니다. 작성 후 ASCII 숫자·구분자·길이·마스킹·원문 비노출 계약이 통과해야 합니다. 정답은 별도 solutions 프로필로만 선택합니다.

## 종료와 기록

```sh
docker compose --profile phase5 down
```

볼륨을 유지합니다. 정상 보안 테스트, 장애 증상 재현, 수정안 회귀 검증, 실제 DB·Tomcat 결과를 각각 기록합니다.
