# Phase 4 정답과 해설

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

이 정답은 기본 거래 WAR와 분리된 정책 실습입니다. 실제 DB 잠금을 획득하지 않고 순서만 계산합니다.

## 일관된 순서

서로 다른 두 계좌를 PostgreSQL UUID 순서로 정렬한 불변 목록을 반환합니다. null과 동일 계좌를 먼저 거절합니다. UUID의 정규 문자열은 고정 길이 소문자 16진수이며 하이픈 위치도 같아서 문자열 사전순이 16바이트의 unsigned 순서와 일치합니다. 따라서 정답은 `Comparator.comparing(UUID::toString)`을 사용합니다.

Java 17의 `UUID.compareTo` 구현은 두 long을 부호 있는 값으로 비교합니다. PostgreSQL의 uuid 비교는 바이트 배열의 memcmp를 사용하므로 `7fff...`와 `8000...` 경계에서 순서가 달라질 수 있습니다. 상위 64비트가 같은 경우 하위 64비트의 부호 경계도 확인해야 합니다. 이는 두 구현을 비교한 결과입니다. [OpenJDK UUID 구현](https://github.com/openjdk/jdk17u/blob/master/src/java.base/share/classes/java/util/UUID.java), [PostgreSQL 17 uuid 구현](https://github.com/postgres/postgres/blob/REL_17_STABLE/src/backend/utils/adt/uuid.c)

DB와 같은 순서가 항상 모든 시스템의 필수 조건인 것은 아니지만, 이 과제는 Java와 SQL의 순서를 하나로 고정하는 계약을 사용합니다. A→B와 B→A 요청 모두 같은 계좌부터 잠그면 서로 반대 방향으로 기다리는 위험을 줄일 수 있습니다. 다른 자원까지 포함한 전체 잠금 순서와 timeout·재시도 정책은 따로 설계해야 합니다.

`List.of`는 결과 목록을 수정할 수 없게 합니다. UUID 자체도 불변이므로 입력 값을 바꾸지 않습니다. 테스트는 정답과 다른 방식인 unsigned 바이트 비교를 기준으로 고정 경계와 재현 가능한 256개 입력 쌍을 확인합니다.

## 선택 검증

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -Psolution-phase4 -pl exercises/solutions/phase-04 -am verify
```

정답 모듈은 starter의 같은 계약 테스트를 사용합니다. 정렬 테스트의 성공이 PostgreSQL에서 실제 deadlock·rollback·동시 출금을 검증했다는 뜻은 아닙니다. 실제 DB 통합 검증 결과는 루트 `PROGRESS.md`에서 확인합니다.
