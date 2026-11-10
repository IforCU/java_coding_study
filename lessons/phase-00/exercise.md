# Phase 0 실습 — 실행 구조 추적과 메타데이터 조회

정상 실행 기준 프로젝트를 먼저 실행한 뒤, 별도의 starter 파일만 수정합니다. 정답과 해설은 기본 화면과 이 문제에 포함하지 않습니다.

## 과제 1: 요청과 실행 구조 기록

다음 항목을 자신의 말로 정리합니다. 소스를 수정할 필요는 없습니다.

1. 브라우저가 호출하는 주소와 Java 컨테이너가 DB에 연결하는 주소는 어떻게 다른가요?
2. `GET /`의 JSP와 `GET /health/ready`의 Servlet은 각각 어떤 역할을 맡나요?
3. DB health check가 성공했는데 ready 요청이 실패할 수 있는 이유는 무엇인가요?
4. DB 데이터가 저장되는 볼륨과 WAR가 실행되는 컨테이너는 어떤 관계인가요?
5. Java 17 선택과 `jakarta.servlet` 선택은 왜 따로 확인해야 하나요?

확인할 파일은 루트 `compose.yaml`, `modules/00-bootstrap/pom.xml`, `kr.banklearning.bootstrap` 패키지, `database/migrations`와 `database/seed`입니다.

## 과제 2: SQL 직접 작성

수정 파일: [`exercises/starter/phase-00/read-metadata.sql`](../../exercises/starter/phase-00/read-metadata.sql)

미완성 TODO를 바꿔 다음 조건을 만족하는 읽기 전용 조회문 한 개를 작성하세요.

- 조회 대상은 `bootstrap.app_metadata`입니다.
- `local-learning` 환경의 행을 조회합니다.
- 결과 열은 `environment`, `schema_version` 두 개이며 이 순서를 지킵니다.
- 결과는 정확히 한 행이어야 합니다.
- 고정된 문자열이나 숫자를 반환해 검증값을 흉내 내지 말고 테이블의 값을 읽습니다.
- 데이터 변경, 스키마 변경, psql 메타 명령은 추가하지 않습니다.

이 파일은 기본 빌드와 기본 애플리케이션 실행에 포함되지 않습니다. 따라서 TODO가 남아 있어도 정상 모듈은 실행할 수 있어야 합니다.

Windows PowerShell에서 검증합니다.

```powershell
.\scripts\verify-exercise.ps1
```

Linux/macOS에서는 다음 명령을 사용합니다.

```sh
sh scripts/verify-exercise.sh
```

DB와 초기 스키마가 먼저 준비되어 있어야 합니다. 미완성 SQL의 문법 오류나 결과 불일치는 실습 실패로 표시되어야 합니다. 수정 후 성공 여부와 작성한 SQL의 각 부분이 어떤 역할을 하는지 기록하세요. 검증기는 출력값을 확인하므로, 실제로 테이블을 읽었는지도 스스로 코드 리뷰합니다.

## 과제 3: 재시작 관찰

일반 검증 스크립트의 재시작 옵션을 실행합니다. 명령은 [verification.md](verification.md)에 있습니다.

1. 재시작 전 Hello World, live, ready 응답을 확인합니다.
2. 재시작 후 같은 응답과 같은 메타데이터가 유지되는지 확인합니다.
3. 컨테이너와 볼륨 중 무엇이 데이터를 유지하는지 설명합니다.

컨테이너 재시작은 데이터 삭제 과제가 아닙니다. 볼륨 삭제나 DB 강제 초기화를 실행하지 않습니다.

막히면 [hints.md](hints.md)의 힌트를 순서대로 읽습니다. Phase 0 완료 판정은 실습 SQL 하나의 통과만으로 결정하지 않고, [verification.md](verification.md)의 실행 검증 결과를 함께 확인합니다.
