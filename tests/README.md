# 검증 안내

기본 빌드는 정상 모듈 7개를 포함합니다. TODO·정답 모듈은 별도 Maven 프로필을 선택할 때만 포함합니다. 최종 실행 결과는 [PROGRESS.md](../PROGRESS.md)가 기준입니다.

## 기본 로컬 검증

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./scripts/test-local.ps1 -Offline
```

최초 의존성 다운로드가 필요하면 `-Offline`을 생략합니다. 호스트 Maven을 사용하지 않는 경로:

```sh
docker compose --profile test run --build --rm test
```

| 모듈 | 기본 테스트 수 | 검증 범위 |
| --- | ---: | --- |
| 00-bootstrap | 16 | 설정·비밀정보 비노출·JDBC 자원·WAR 드라이버 로딩 |
| 01-java-core | 20 | 금액·다중 계좌·입출금·이력·메모리 동시 출금 |
| 02-servlet-jsp | 28 | Service/JDBC/Servlet·Form·CSRF·PRG |
| 03-spring-mvc | 26 | DI/XML·MyBatis SQL 생성·기간·상태·MockMvc |
| 04-banking-transaction | 26 | 이체 업무 규칙·멱등성·HTTP·JDBC 계약 |
| 05-security-operations | 33 | 보안 흐름·감사·추적·초기화 순서 25개, 격리된 증상 재현 8개 |
| 06-spring-boot | 22 | Mockito·실제 Boot 테스트 컨텍스트·JSON·Validation·권한 |
| 합계 | 171 | PostgreSQL 없는 기본 검증 |

JDBC 대역·Mockito·MockMvc 통과를 실제 DB/브라우저 렌더링 성공으로 해석하지 않습니다. 실제 동시 이체의 rollback·잠금은 아래 DB 통합 테스트에서 확인합니다.

## 실제 PostgreSQL 통합 검증

```sh
docker compose --profile integration run --build --rm integration-test
```

Maven의 `integration-tests` 프로필이 Failsafe로 `*IT.java`를 실행합니다. 기본 Surefire는 이를 실행하지 않습니다. DB 환경변수가 없으면 명시적 통합 검증이 실패하며 성공으로 자동 건너뛰지 않습니다.

| 테스트 클래스 | 수 | 범위 |
| --- | ---: | --- |
| JdbcBankDaoIT | 2 | PreparedStatement·UUID·timestamp·데이터 격리 |
| BankMapperIT | 2 | 실제 Mapper XML·기간 경계·상태·결과 매핑 |
| TransferServiceIT | 7 | Spring TX rollback·중복/동시 출금·DB 예외·응답 유실·한도/취소·역방향 잠금 |
| MaintenanceLabIT | 4 | 잘못된 WHERE·풀 고갈·느린 SQL·부분 커밋 증상 |
| BankReadRepositoryIT | 3 | 읽기 권한·쓰기 거절·조회 매핑·REPEATABLE READ |
| 합계 | 18 | 실제 PostgreSQL에서 18개 모두 PASS (2026-10-25) |

격리된 장애 실습의 PASS는 증상 재현을 뜻합니다. 수정 검증 계약은 [실습 문서](../docs/troubleshooting/maintenance-labs.md)에서 확인합니다. 성공적으로 commit한 테스트 fixture는 자체 UUID로 구분하고 보존하며 기존 데이터를 삭제하지 않습니다.

## 실습 TODO와 정답 계약

Phase 1~6 정답 계약 테스트는 총 79개입니다. 같은 계약을 TODO에 적용하면 수정 전에는 실패해야 합니다. 기본 테스트의 성공과 구분해서 기록합니다.

```sh
docker compose --profile exercise run --rm exercise-test -Pexercise-phase1 -pl exercises/starter/phase-01 -am test
```

Phase 0은 DB 실행 후 `verify-exercise`로 SQL을 검증합니다. 정답과 TODO는 정상 JAR/WAR에서 제외합니다.

## 실행·재시작

`scripts/verify-all.ps1 -Restart -Integration` 또는 `sh scripts/verify-all.sh --restart --integration`으로 전체 컨테이너와 HTTP/DB·영속성을 확인합니다. 2026-10-25 실제 Docker에서 기본 171개와 PostgreSQL 통합 18개, 6개 웹 앱의 HTTP/DB 준비 상태와 재시작 검증을 통과했습니다. JSP·폼·CSRF·이체·보안·API의 실제 HTTP 시나리오도 별도로 확인했습니다. 로그와 최종 종료·재현 결과는 [PROGRESS.md](../PROGRESS.md)를 참고하세요.
