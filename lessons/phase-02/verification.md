# Phase 2 — 실행과 검증

새 계좌 화면 보조 실습의 로컬 미리보기·실제 JSP 렌더링·완성 계약 검사는 [JSTL 검증](jstl/verification.md)을 따릅니다. 아래 DB 기반 모듈의 검증과 구분합니다.

명령은 프로젝트 루트 기준의 재현 절차입니다. 아래 기대 결과를 실제 PASS 기록으로 읽지 않습니다. 로컬 빌드·테스트와 Docker·실제 PostgreSQL·Tomcat 통합 검증의 최신 결과는 `PROGRESS.md`를 확인합니다.

## 로컬 빌드와 단위 테스트

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -pl modules/02-servlet-jsp -am verify
```

기대 결과: Java 17 빌드와 단위 테스트가 성공하고 `modules/02-servlet-jsp/target/servlet-jsp.war`가 생성됩니다. WAR에는 정상 소스와 JSP만 포함되고 starter TODO, 정답, `legacy-customer.jsp.txt`가 들어가면 안 됩니다.

실제 DB가 준비되기 전에는 DAO 대역을 사용한 서비스·HTTP 테스트와 실제 DB 통합 테스트 결과를 분리해 기록합니다. 빌드 성공만으로 SQL 실행이나 JSP 렌더링을 PASS 처리하지 않습니다.

## Docker 실행 절차

Docker 엔진이 준비되면 실행합니다.

```sh
docker compose --profile phase2 up --build -d
docker compose --profile phase2 ps -a
```

기본 주소는 `http://127.0.0.1:8082`입니다. 루트 환경변수에서 공개 포트를 바꾸었다면 해당 포트를 사용합니다. DB health, 마이그레이션 성공, `servlet-jsp` 상태를 구분해 확인합니다.

## 브라우저 수동 검증

1. `/health`에서 HTTP 200과 `{"status":"UP"}`를 확인합니다.
2. `/customers`에서 가상 초기 고객과 등록 폼이 보이는지 확인합니다.
3. 가상 고객을 등록하고 개발자 도구에서 POST→303→GET을 확인합니다.
4. 등록 후 새로고침하여 등록 요청 대신 목록 GET이 반복되는지 확인합니다.
5. 고객을 선택하고 계좌 화면에서 해당 고객의 계좌 목록을 확인합니다.
6. 계좌를 등록하여 잔액이 0원인지 확인합니다.
7. 전체 계좌 조회에서 초기 계좌를 열고 거래 내역의 금액과 거래 후 잔액을 확인합니다.

초기 seed의 기준값은 `학습고객02`, 초기 계좌 잔액 100,000원, 최초 입금 이력 100,000원입니다. 이미 학습 데이터를 변경한 환경에서는 기존 값을 보존하므로 초기 상태를 임의로 덮어쓰지 않습니다.

## 오류 분기

다음 요청은 개발자 도구나 격리된 HTTP 테스트로 확인합니다. DB를 초기화하거나 데이터 삭제로 검증하지 않습니다.

| 조건 | 기대 결과 |
| --- | --- |
| 잘못된 UUID 파라미터 | 400, 제한된 입력 오류 메시지 |
| CSRF 토큰 누락·불일치 POST | 403, DB 변경 없음 |
| 유효한 토큰과 유효하지 않은 이름 | 400, 고객 증가 없음 |
| 존재하지 않는 계좌 UUID | 404 |
| DB 사용 불가 | health 또는 업무 조회 503, 비밀정보·SQL 내부 예외 비노출 |

## 선택 실습

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -Pexercise-phase2 -pl exercises/starter/phase-02 -am verify
```

미완성 TODO는 실패가 기대됩니다. 작성 후 null·공백·길이 경계·정규화 보존 테스트가 통과해야 합니다. 기본 `mvn -Dmaven.repo.local=.cache/maven-repository verify`는 실습을 선택하지 않습니다. 정답 비교 실행은 별도 solutions 안내를 확인합니다.

## 종료와 기록

```sh
docker compose --profile phase2 down
```

DB 볼륨은 유지합니다. 빌드·단위 테스트, 실제 DB, JSP 렌더링, 세션·CSRF·PRG, 종료/재시작을 각각 기록하고 실행하지 못한 항목은 `NOT RUN`으로 남깁니다.
