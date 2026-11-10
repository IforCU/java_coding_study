# PostgreSQL에서 Oracle·Tibero 레거시 SQL 읽기

검토일: 2026-10-25. 이 프로젝트의 실행 DB는 PostgreSQL 17이다. 아래 비교는 Oracle Database 19c 문서와 Tibero 7.2.6 매뉴얼 및 별도로 표시한 Tibero 7.2.4 기술 자료를 기준으로 한다. JDBC 인터페이스를 사용해도 SQL·타입·잠금 동작까지 자동으로 같아지지는 않는다. Oracle/Tibero용 드라이버나 서버를 설치하거나 실행한 결과는 아니다.

아래 SQL은 학습 모듈의 타입·문법 계약과 재현 설명이다. 실제 PostgreSQL 적용과 통합 테스트의 최신 결과는 [PROGRESS.md](../../PROGRESS.md)에서 확인한다. 공식 문서 확인과 소스 검토를 DB 통합 테스트 PASS로 기록하지 않는다.

## Phase 2의 저장 계약

`phase2` 스키마는 `customer` → `account` → `transaction_history`로 연결된다. ID는 Java가 생성하는 UUID이고, 금액은 Java `BigDecimal`과 SQL `numeric(19,0)`으로 표현한다. 원화 정수만 허용하므로 Java에서 소수 입력을 먼저 거부해야 한다. PostgreSQL의 지정 scale 숫자 컬럼은 입력을 반올림할 수 있어 DB 타입 선언만으로 소수 입력 검증을 대신할 수 없다. [PostgreSQL 숫자 타입](https://www.postgresql.org/docs/17/datatype-numeric.html)

| 비교 지점 | PostgreSQL 17 | Oracle 19c / Tibero 전환 시 확인 |
| --- | --- | --- |
| 빈 문자열 | `''`와 `NULL`을 구분 | Oracle은 길이 0 문자열을 NULL로 처리한다. Tibero 7.2.4 공식 비교 자료도 같은 결과를 제시한다. 공백 한 글자는 별도 값이므로 Java의 trim·필수 검증을 유지한다. |
| 자동 번호 | `GENERATED ... AS IDENTITY`, `nextval('sequence')` | Oracle의 identity와 `sequence.NEXTVAL`을 구분한다. Tibero는 대상 버전의 CREATE TABLE/SEQUENCE 지원 문법을 확인한다. Phase 2는 번호 생성 SQL 대신 Java UUID를 사용한다. |
| 일부 행 조회 | `ORDER BY ... LIMIT ? OFFSET ?` 또는 표준 FETCH | Oracle 19c는 OFFSET/FETCH를 제공하며 구형 소스의 ROWNUM 방식과 의미를 비교한다. Tibero에도 PostgreSQL LIMIT를 그대로 옮기지 말고 설치 버전의 SQL 질의 문법을 확인한다. |
| UUID 저장 | 네이티브 `uuid`, JDBC UUID 바인딩 | Oracle `SYS_GUID()`는 RAW 16바이트를 반환한다. 하이픈 문자열 UUID와 저장 형식이 같다고 가정하지 않는다. Tibero 전환도 실제 RAW/문자열 컬럼 및 변환 규칙을 정해야 한다. |
| 시각 | `timestamptz`는 `timestamp with time zone`의 별칭 | Oracle/Tibero의 DATE, TIMESTAMP, TIME ZONE 타입과 드라이버 변환을 따로 확인한다. 문자열 포맷이나 세션 기본값에 맡기지 않는다. |
| 갱신 잠금 | `SELECT ... FOR UPDATE`로 행 잠금 | Oracle/Tibero에도 관련 문법이 있지만 대기 옵션, 격리 수준, 재조회와 페이지 제한 조합까지 동일하다고 가정하지 않는다. |
| 값 전달 | JDBC `?` 자리표시자와 타입별 setter | 세 DB에서 JDBC 바인딩 원칙은 유지한다. 테이블명·컬럼명·정렬 방향은 값 바인딩 대상이 아니므로 서버의 허용 목록으로 정한다. |

빈 문자열 비교 근거: [Oracle Nulls](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Nulls.html), [Tibero GTS NULL 비교](https://support.tibero.com/hc/en-us/articles/14235603762063-Comparing-RDBMS-NULL). PostgreSQL의 이름 필수 검증에는 `NOT NULL`과 비어 있지 않다는 조건을 함께 둔다. SQL에서 NULL을 찾을 때는 `= NULL` 대신 `IS NULL`을 사용한다.

자동 번호 비교 근거: [PostgreSQL identity](https://www.postgresql.org/docs/17/ddl-identity-columns.html), [Oracle CREATE TABLE의 identity_clause](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html), [Oracle 시퀀스](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Sequence-Pseudocolumns.html). 시퀀스의 번호는 거래 성공 순서나 무결한 연속 번호를 뜻하지 않는다. 업무용 거래번호 정책과 PK 생성은 별도로 설계한다.

## 조회 SQL의 작은 차이가 만드는 결과 차이

아래는 조회 문법 비교용이며 기본 실행에 넣는 이식 스크립트가 아니다.

```sql
-- PostgreSQL JDBC: 바인딩 순서는 size, offset이다.
SELECT id, name, status
FROM phase2.customer
ORDER BY id
LIMIT ? OFFSET ?;

-- Oracle 19c JDBC: 바인딩 순서는 offset, size이다.
-- 실제 Oracle 스키마에서는 ID 저장 타입도 함께 검토해야 한다.
SELECT id, name, status
FROM customer
ORDER BY id
OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
```

두 문장은 자리표시자 순서부터 다르다. 페이징은 반드시 고유한 순서를 만들어야 한다. `ORDER BY name`만 사용하면 같은 이름의 두 고객 위치가 바뀔 수 있으므로 `ORDER BY name, id`처럼 동률을 해소한다. 큰 OFFSET의 비용과 동시 삽입 중 중복·누락도 별도로 검토한다. Oracle 19c에서는 row limiting 절과 FOR UPDATE를 함께 쓸 수 없다는 제한도 있다. [PostgreSQL LIMIT/OFFSET](https://www.postgresql.org/docs/17/queries-limit.html), [Oracle SELECT](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html)

기간 검색은 `occurred_at >= ? AND occurred_at < ?`처럼 시작 포함·종료 제외로 정하고 업무 날짜의 시간대를 먼저 결정한다. PostgreSQL `timestamptz`는 시점을 UTC로 저장하고 세션 시간대에 맞춰 보여 주며, 입력 당시의 지역 이름 자체를 보존하지 않는다. 문자열이 비슷하게 보이는지만 비교하지 말고 Java의 시점과 DB에서 복원한 시점이 같은지 검증한다. [PostgreSQL 날짜/시간](https://www.postgresql.org/docs/17/datatype-datetime.html), [Oracle 날짜/시간 표현식](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Datetime-Expressions.html), [Tibero JDBC 타입 대응표](https://docs.tibero.com/tibero-manuals/7.2.6.manuals/jdbc-development-guide/tbjdbc-usage)

UUID는 [PostgreSQL uuid](https://www.postgresql.org/docs/17/datatype-uuid.html)와 [Oracle SYS_GUID](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SYS_GUID.html)를 비교한다. Java UUID 문자열을 Oracle/Tibero 문자 컬럼으로 바꾸거나 16바이트 표현으로 바꾸려면 변환 방향·대소문자·하이픈·인덱스 정렬을 명시한다. 운영 SQL의 PK를 문자열 치환만으로 이식하지 않는다.

## 트랜잭션·잠금·바인딩의 책임

`PreparedStatement`는 입력값을 SQL 구조와 분리한다. 다음 코드에서 SQL 구조는 고정되고 고객 ID만 바인딩된다. 이 코드는 API 설명용이며 실제 리소스 수명은 DAO/서비스의 트랜잭션 경계에 맞춘다.

```java
try (PreparedStatement statement = connection.prepareStatement(
        "SELECT id, balance FROM phase2.account WHERE customer_id = ? ORDER BY id")) {
    statement.setObject(1, customerId);
    try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
            BigDecimal balance = result.getBigDecimal("balance");
            // ResultSet을 닫기 전에 값/DTO로 복사한다.
        }
    }
}
```

PostgreSQL UUID용 `setObject(UUID)`가 다른 드라이버의 RAW 컬럼에도 그대로 맞는지는 별도 검증 대상이다. Oracle/Tibero 이식 시 setter·getter와 SQL 타입을 함께 바꾼다. 값 바인딩은 오류 노출이나 소유권 검사를 대신하지도 않는다. 타 고객의 UUID를 넣었을 때 조회를 허용할지는 서비스의 권한 계약으로 결정한다. [pgJDBC 문장 실행](https://jdbc.postgresql.org/documentation/query/), [Tibero JDBC 사용](https://docs.tibero.com/tibero-manuals/7.2.6.manuals/jdbc-development-guide/tbjdbc-usage)

SELECT FOR UPDATE는 같은 트랜잭션의 commit/rollback까지 잠금이 유지되는 범위를 이해하고 사용한다. 자동 커밋 상태에서 한 SELECT를 잠근 뒤 다른 Connection으로 UPDATE하면 하나의 업무 트랜잭션이 되지 않는다. 데드락과 timeout은 오류 코드·SQLState를 DB별로 분류하고, 민감한 SQL 예외 전체를 응답에 출력하지 않는다. 동시 이체와 원장 일치는 Phase 4에서 실제 경쟁 요청으로 검증한다. [PostgreSQL 잠금](https://www.postgresql.org/docs/17/explicit-locking.html), [Oracle SELECT FOR UPDATE](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html), [Tibero current/consistent read](https://support.tibero.com/hc/en-us/articles/14310546428687-How-to-Perform-UPDATE-in-Current-Consistent-Read-Mode)

## migration과 seed를 읽는 순서

1. V001이 `bootstrap.schema_history`에 버전 1을 만든다.
2. V002가 `phase2` 테이블·제약·인덱스와 버전 2 이력을 추가한다. `bootstrap.app_metadata.schema_version=1`은 Phase 0 계약이므로 변경하지 않는다.
3. R002는 가상 고객과 최초 100,000원 계좌를 추가한다. 이번 실행에서 계좌를 만든 경우에만 DEPOSIT 이력을 같이 추가한다. 기존 실습 잔액을 원래 값으로 돌리지 않는다.
4. [조회 파일](../../database/queries/phase2.sql)로 관계와 이력을 관찰한다. migration/seed 재실행 전후에는 기존 UUID의 이름·상태·잔액·이력이 보존되는지도 비교한다.

`ON CONFLICT DO NOTHING`, advisory lock, PostgreSQL DO 블록은 이 프로젝트의 PostgreSQL 문법이다. Oracle/Tibero에서는 버전·권한·오류 처리·동시 실행 전략을 정한 별도 migration이 필요하다. `CREATE TABLE IF NOT EXISTS`는 이미 존재하는 잘못된 컬럼이나 제약을 고쳐 주지 않는다. 기존 스키마가 다르면 새 버전 migration을 추가하며 자동 초기화하지 않는다. [PostgreSQL INSERT](https://www.postgresql.org/docs/17/sql-insert.html), [Tibero 7.2.6 SQL 안내서](https://docs.tibero.com/tibero-manuals/7.2.6.manuals/sql-reference-guide)

Phase 3에서 JDBC 호출을 MyBatis mapper로 옮길 때도 위 계약은 유지한다. iBATIS/MyBatis의 XML 문법·파라미터·트랜잭션 비교는 Phase 3 교재에서 다룬다. 프레임워크 변경이 DB 방언 차이를 제거하지는 않는다.
