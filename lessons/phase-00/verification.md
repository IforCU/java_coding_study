# Phase 0 — 실행 및 검증

아래는 프로젝트 루트에서 실행하는 재현 절차와 **기대 결과**입니다. 명령 예시와 기대 결과는 실제 PASS 기록이 아닙니다. 로컬 빌드·테스트와 컨테이너·실제 DB·재시작 검증의 최신 결과는 루트 `PROGRESS.md`를 확인합니다.

## 1. 준비 상태

Windows 11에서는 Docker Desktop과 Linux 컨테이너 엔진이 실행 중이어야 합니다. 프로젝트는 전역 Java/Maven 설치 대신 컨테이너 빌드를 기준으로 구성합니다. Linux/macOS에서도 Docker 엔진과 Compose 플러그인이 필요합니다.

```sh
docker version
docker compose version
docker compose --profile phase0 config --quiet
```

기대 결과: Docker 서버 정보를 읽을 수 있고 Compose 설정 검사 종료 코드가 0입니다. Docker 서버 연결 실패는 환경 미준비이며, 설정 검사 성공으로 대신 완료 처리하지 않습니다.

루트 `.env`가 없다면 `.env.example`을 복사합니다. 기존 `.env`가 있으면 내용을 먼저 확인해 보존합니다.

```powershell
Copy-Item .env.example .env
```

```sh
cp .env.example .env
```

위 복사 명령은 새 환경에서 한 번만 선택해 실행합니다. 기본 주소는 `http://127.0.0.1:8080`이며 `LEGACY_PORT`를 변경한 경우 해당 포트를 사용합니다.

## 2. 빌드와 시작

```sh
docker compose --profile phase0 up --build -d
docker compose --profile phase0 ps -a
```

기대 결과: WAR 이미지 빌드가 성공하고 `db`가 healthy, `migrate`가 종료 코드 0, `legacy`가 실행 상태가 됩니다. 초기 DB는 `bank_learning`, 사용자는 학습 전용 `bank_learning`이며 메타데이터는 `bootstrap` 스키마에 저장됩니다.

## 3. 자동 HTTP·DB 검증

Windows PowerShell:

```powershell
.\scripts\verify.ps1
```

Linux/macOS:

```sh
sh scripts/verify.sh
```

기대 결과: 스크립트가 성공 종료하고 Hello World JSP, live JSON, ready JSON 및 초기 메타데이터 검사가 통과합니다. 수동으로 관찰하려면 브라우저에서 다음 주소를 엽니다.

| 주소 | 기대 결과 |
| --- | --- |
| `http://127.0.0.1:8080/` | HTTP 200, Hello World가 포함된 HTML |
| `http://127.0.0.1:8080/health/live` | HTTP 200, 실행 상태 JSON |
| `http://127.0.0.1:8080/health/ready` | DB 조회가 성공하면 HTTP 200, 준비 상태 JSON |

ready의 성공은 학습 환경과 스키마 버전을 실제로 조회한 결과여야 합니다. DB 연결이나 메타데이터 확인이 실패하면 준비 실패를 반환해야 하고 자격 증명이나 SQL 내부 오류는 응답에 포함하면 안 됩니다.

## 4. Java 테스트

```sh
docker compose --profile test run --build --rm test
```

기대 결과: Maven의 Java 테스트가 실행되고 실패 없이 종료합니다. 단위 테스트 통과와 실제 Tomcat·PostgreSQL 연결 검증은 각각 기록합니다. 단위 테스트만으로 DB 통합 검증을 PASS로 기록하지 않습니다.

## 5. 재시작과 데이터 유지

Windows PowerShell:

```powershell
.\scripts\verify.ps1 -Restart
```

Linux/macOS:

```sh
sh scripts/verify.sh --restart
```

기대 결과: 재시작 전후 상태 점검이 통과하고 메타데이터가 유지됩니다. 일시적으로 준비 상태가 내려갈 수 있으므로 스크립트의 대기 결과를 확인합니다. 재시작은 DB 볼륨을 삭제하지 않습니다.

## 6. 선택 실습 검증

[exercise.md](exercise.md)의 starter SQL을 작성한 후 실행합니다. 기본 검증은 실습 TODO를 실행하지 않습니다.

```powershell
.\scripts\verify-exercise.ps1
```

```sh
sh scripts/verify-exercise.sh
```

기대 결과: 테이블에서 읽은 두 열이 지정 순서로 한 행 반환되어 검증이 성공합니다. TODO를 작성하지 않은 starter는 실패해야 합니다. 정답 파일은 별도 정답 디렉터리에서 선택해서 실행합니다.

### 테이블이 없다고 표시될 때

`syntax error at or near "FROM"`은 테이블 누락이 아니라 SQL 문법 오류입니다. TODO의 주석은 실행할 때 무시되지만 주석 밖의 쉼표는 남습니다. SELECT의 마지막 결과 열 뒤에 쉼표가 남지 않도록 TODO 자리와 구분 기호를 함께 정리합니다. 주석 자체를 지울 필요는 없으며, 끝의 세미콜론도 문장 종료 위치에 하나만 둡니다.

SQL 파일을 여는 것만으로 테이블이 만들어지지는 않습니다. `bootstrap.app_metadata`는 `database/migrations/V001__bootstrap.sql`을 `migrate` 서비스가 실행할 때 생성하며 초기 행은 `database/seed/R001__bootstrap.sql`이 넣습니다. 먼저 `docker compose --profile phase0 ps -a`에서 `db`의 healthy와 `migrate`의 `Exited (0)`를 확인합니다. DB healthy만으로 초기화 완료를 판단하지 않습니다.

IDE나 DB 도구에서 오류가 발생한다면 이 프로젝트의 PostgreSQL에 연결했는지 확인합니다. 호스트는 `127.0.0.1`, 포트는 `.env`의 `POSTGRES_PORT`(미지정 시 5432), DB는 `POSTGRES_DB`(미지정 시 `bank_learning`)입니다. 컨테이너 내부의 `db:5432`는 IDE의 접속 주소와 다릅니다. 실제 공개 포트는 위 `ps -a`의 PORTS에서도 확인할 수 있습니다.

연결한 DB에서 다음 진단 SQL을 실행합니다. 이는 실습 정답이 아니라 접속 위치와 테이블 존재 확인용입니다.

```sql
SELECT current_database(), current_user,
       to_regclass('bootstrap.app_metadata') AS metadata_table;
```

`metadata_table`이 NULL이면 그 DB에서 테이블을 찾지 못한 것입니다. 접속 위치를 먼저 확인하고 migration 상태와 로그를 점검합니다. 접속이 맞고 초기화가 누락되었다면 `docker compose --profile phase0 run --rm migrate`로 migration과 seed를 재실행할 수 있습니다. 이 명령은 모든 단계의 초기화 SQL을 실행하며 기존 데이터를 삭제하지 않습니다. 오류가 나면 원인을 확인하고 볼륨을 삭제하거나 DB를 초기화하지 않습니다.

테이블이 조회되고 실습 스크립트도 PASS하는데 IDE에만 경고가 남으면 SQL 파일에 연결된 데이터 소스를 선택하고 `bootstrap` 스키마를 탐색 대상으로 포함한 뒤 메타데이터를 새로고침합니다. `app_metadata`만 쓰면 기본 스키마에서 찾을 수 있으므로 과제에서 지정한 스키마 포함 이름을 사용합니다. IDE 경고와 실제 PostgreSQL 실행 오류는 구분해서 기록합니다.

## 7. 종료와 재현

```sh
docker compose --profile phase0 down
```

기대 결과: 이 프로젝트의 컨테이너와 네트워크가 종료되고 DB 볼륨은 유지됩니다. 종료 후 2~3단계를 다시 실행해 정상 동작을 확인합니다. 볼륨을 삭제하는 옵션은 사용하지 않습니다.

## 기록과 완료 판정

각 명령의 실행 날짜, 종료 코드, 결과 요약, 실패 원인을 `PROGRESS.md`에 기록합니다. 실행하지 못한 항목은 `NOT RUN`, 실제 실패는 원인과 함께 기록합니다. Docker 엔진 부재와 Java/SQL 오류를 구분합니다.

Phase 0은 Compose 빌드, DB health check, Java/JSP와 JDBC 실행, 테스트, 재시작 후 데이터 유지, 종료 및 README 절차 재현이 실제로 확인된 후에 전체 완료로 표시합니다. 실행하지 못한 항목은 `NOT RUN`으로 남기고 실제 통과 결과와 구분합니다.
