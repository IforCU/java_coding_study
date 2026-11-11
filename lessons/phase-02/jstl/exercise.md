# 과제 — 내 계좌 조회 화면 완성

수정 대상은 [accounts.jsp](../../../exercises/starter/phase-02/jstl/accounts.jsp)입니다. Servlet, 테스트, 정답 파일을 수정해 통과시키지 않습니다. Java scriptlet 없이 JSTL과 EL로 TODO 1~8을 완성하세요. 서버 실행은 [lesson.md](lesson.md)를 따릅니다.

## 데이터 계약

Servlet이 request attribute `accounts`로 목록을 제공합니다. 각 항목은 Map이며 아래 키를 사용합니다.

| 키 | 내용 |
| --- | --- |
| `name` | 가상 별칭. HTML 특수문자 포함 가능 |
| `number` | 이미 마스킹된 계좌번호 |
| `type` | `DEPOSIT` 또는 `SAVINGS` |
| `status` | `ACTIVE`, `BLOCKED`, `CLOSED` |
| `balance` | 원 단위 정수 `BigDecimal` |

준비된 계좌는 생활비 통장(입출금·정상·1,250,000원), 여행 적금(적금·정상·350,000원), 비상금 통장(입출금·거래 제한·50,000원), 종료된 적금(적금·해지·0원)입니다. 비상금 별칭에는 `<안전>`이라는 문자열도 포함합니다.

## 구현할 동작

1. `param.name`의 앞뒤 공백을 제거하고 비었으면 '학습자'로 표시합니다. `id="greeting"` 문단에 `이름님, 계좌를 확인하세요.`를 출력합니다.
2. `id="total-count"`에는 필터 전 전체 건수만 출력합니다.
3. 목록을 순회하며 `param.type`이 없거나 계좌 종류와 같은 행만 표시합니다. 각 행의 `class="account-row"`를 유지하고, 표시 순번은 1부터 연속으로 매깁니다.
4. 이름과 계좌번호를 `c:out`으로 출력합니다. `<안전>`이 HTML 태그로 해석되지 않아야 합니다.
5. 종류는 입출금/적금, 상태는 정상/거래 제한/해지로 표시합니다. 조건에 따른 표시에는 `c:choose`를 사용합니다.
6. 금액은 `fmt:formatNumber`로 천 단위 구분 후 '원'을 붙입니다. 0원도 보여야 하며 소수 원 단위나 반올림 규칙을 새로 만들지 않습니다.
7. 표시 건수가 0이면 `id="empty-message"`에 '조건에 맞는 계좌가 없습니다.'를 출력합니다. 그 외에는 `id="result-count"`에 건수만 출력합니다. 두 요소는 동시에 나타나지 않습니다.
8. `c:url`과 `c:param`으로 초기화·빈 목록 링크를 생성합니다. 빈 목록 링크는 `id="empty-link"`, 쿼리는 `demo=empty`입니다. URL을 HTML 속성에 넣을 때도 이스케이프합니다.

## 직접 확인할 주소

| 경로 | 기대 결과 |
| --- | --- |
| `/accounts` | 전체 4 / 표시 4 |
| `/accounts?type=DEPOSIT` | 전체 4 / 표시 2 |
| `/accounts?type=SAVINGS` | 전체 4 / 표시 2, 순번 1·2 |
| `/accounts?type=UNKNOWN` | 전체 4 / 표시 0, 빈 결과 문구 |
| `/accounts?demo=empty` | 전체 0 / 표시 0, 빈 결과 문구 |
| `/accounts?name=%20%20` | 기본 인사 |
| `/accounts?name=%3Cb%3Etest%3C%2Fb%3E` | 태그가 실행되지 않고 문자 그대로 보이는 인사 |

빈 원본과 필터에 맞는 항목이 없는 경우를 모두 다루세요. 목록이 비어 있다는 사실만 확인하면 두 번째 경우를 놓칩니다. 테스트가 찾는 id/class는 유지하되 HTML과 CSS를 자신의 취향에 맞게 바꾸어도 됩니다.

완성 후 [자동 검증](verification.md)을 실행하고, JSP 코드가 고객 인증이나 실제 거래 허용 여부를 판단하면 안 되는 이유도 자신의 말로 기록하세요.
