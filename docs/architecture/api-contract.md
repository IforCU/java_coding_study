# Phase 6 조회 API 계약

기본 주소는 `http://127.0.0.1:8086`입니다. Java 17 / Spring Boot 4.1.2의 독립 실행 JAR이며 실제 금융 서비스가 아닌 가상 데이터 조회 환경입니다. 아래 JSON은 계약 예시로 실제 실행 로그가 아닙니다.

## 인증과 접근 범위

`/health`는 공개하고 `/api/**`는 stateless HTTP Basic 인증을 요구합니다. 공개된 로컬 가상 계정 `learner`/`learning-user-password`는 USER, `admin`/`learning-admin-password`는 ADMIN입니다.

USER는 고객 ID `40000000-0000-0000-0000-000000000001`과 그 고객의 계좌만 조회합니다. ADMIN은 전체 조회와 관리자 통계에 접근합니다. 인증만 성공했다고 모든 ID를 조회할 수 있는 것은 아닙니다. 모든 업무 경로는 GET 조회이며 DB 역할은 `bank_learning_api`의 제한된 SELECT 권한입니다.

## 표준 JSON envelope

| 필드 | 성공 | 실패 |
| --- | --- | --- |
| `success` | `true` | `false` |
| `data` | 경로별 DTO 또는 목록 | `null` |
| `error` | `null` | `{ "code": "...", "message": "..." }` |
| `requestId` | 추적 ID 문자열 | 추적 ID 문자열 |

성공 예시:

```json
{
  "success": true,
  "data": {
    "id": "40000000-0000-0000-0000-000000000001",
    "name": "학습이체고객04",
    "status": "ACTIVE"
  },
  "error": null,
  "requestId": "demo-phase6-01"
}
```

실패의 `message`는 코드에 대응하는 안전한 한국어 설명입니다. 입력 원문, SQL, DB 연결 정보, 예외 객체는 반환하지 않습니다. 유효한 `X-Request-ID`를 재사용하고 허용되지 않은 값은 안전한 새 ID로 바꿉니다. 응답의 같은 추적 헤더와 `requestId`를 조사에 사용합니다.

## 경로와 데이터

| GET 경로 | `data` 형태 |
| --- | --- |
| `/api/customers/{id}` | Customer |
| `/api/accounts/{id}` | Account |
| `/api/accounts/{id}/transactions` | Transaction 목록 |
| `/api/accounts/{id}/limit` | Limit |
| `/api/admin/statistics` | Statistics, ADMIN 전용 |

DTO 필드:

| DTO | 필드 |
| --- | --- |
| Customer | `id`, `name`, `status` |
| Account | `id`, `customerId`, `balance`, `dailyLimit`, `version` |
| Transaction | `id`, `requestId`, `accountId`, `type`, `amount`, `balanceAfter`, `occurredAt` |
| Limit | `accountId`, `businessDate`, `dailyLimit`, `used`, `remaining` |
| Statistics | `from`, `to`, `transferCount`, `totalAmount` |

ID는 UUID 문자열, 날짜는 ISO 날짜, 거래 시각은 offset을 포함한 ISO 시각입니다. 금액은 원 단위 BigDecimal에서 JSON number로 직렬화합니다. 19자리 원화 값은 JavaScript Number로 파싱하면 정밀도를 잃을 수 있습니다. 정확성이 필요한 클라이언트는 임의 정밀도 숫자 파서를 사용해야 합니다. 서버가 문자열 금액을 반환한다고 가정하지 않습니다.

`/health`는 이 envelope 바깥의 상태 점검입니다. DB 준비 성공은 HTTP 200과 `{"status":"UP"}`, 실패는 HTTP 503과 `{"status":"DOWN"}`을 반환합니다. 상태 점검 응답에도 `X-Request-ID` 헤더를 제공합니다. 추적 ID 허용 형식은 영문자·숫자·하이픈 1~64자이며 누락·불일치 시 새 UUID를 발급합니다.

## 거래 조회 조건

`from`, `to`는 1~9999년의 선택 ISO 날짜 `yyyy-MM-dd`입니다. 누락·빈 값은 해당 날짜 조건을 생략합니다. 양쪽이 있으면 시작일이 종료일보다 늦을 수 없습니다. 날짜는 Asia/Seoul 기준 시작일 0시 이상, 종료일 다음 날 0시 미만으로 변환합니다.

`limit`은 1~200, 누락 기본값은 50입니다. 최대 반환 건수만 제한하며 offset, cursor, 전체 건수, 다음 페이지 토큰은 제공하지 않습니다. 거래는 `occurred_at DESC, id DESC`의 최근순입니다. 같은 시각의 거래도 ID 내림차순으로 순서를 정합니다.

## 통계 조건과 상태

`from`, `to`는 업무 날짜이며 양 끝을 포함합니다. 둘 다 생략하면 Asia/Seoul의 오늘 하루를 조회합니다. 한쪽 날짜만 있으면 누락 경계를 같은 날짜로 정해 해당 하루를 조회합니다. 양쪽이 있으면 지정 범위를 사용하고 최대 366일을 허용합니다. 통계는 `COMPLETED`, `CANCEL_REQUESTED` 이체를 합산합니다. 취소 요청 접수는 환불 처리가 아니므로 접수 상태의 이체도 경제적 처리 결과에 남습니다.

## 오류

| HTTP | `error.code` | 의미 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | UUID·날짜·기간·건수 등 입력 오류 |
| 401 | `UNAUTHORIZED` | 인증 필요 또는 실패 |
| 403 | `FORBIDDEN` | 역할·소유 범위 거절 |
| 404 | `NOT_FOUND` | 조회 가능한 범위의 대상 없음 |
| 405 | `INVALID_REQUEST` | 지원하지 않는 HTTP 메서드 |
| 503 | `STORE_UNAVAILABLE` | 저장소 사용 불가 |
| 500 | `INTERNAL_ERROR` | 공통 내부 오류 |

미인증과 권한 거절도 공통 형식을 따릅니다. 역할에 따라 존재 여부를 조회하기 전에 소유 범위를 거절할 수 있습니다. 클라이언트는 한국어 message 문자열 대신 상태와 code를 처리 기준으로 사용합니다.

## 검증과 소유권

한도 응답은 REPEATABLE_READ 트랜잭션에서 계좌 한도와 당일 사용액을 읽도록 구성합니다. 통계는 단일 집계 SQL입니다. 단위·MockMvc 테스트는 DTO·검증·보안·오류 계약을 확인하며 실제 DB 조회, 스냅샷 일관성, 조회 역할의 쓰기 거절과 기존 거래 데이터 보존은 IT에서 확인합니다. 최신 실행 결과는 [PROGRESS.md](../../PROGRESS.md)를 확인합니다. 자세한 경계는 [legacy-api-ownership.md](legacy-api-ownership.md), 재현 명령은 [Phase 6 verification](../../lessons/phase-06/verification.md)에 있습니다.
