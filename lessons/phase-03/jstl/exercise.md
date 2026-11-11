# Phase 3 화면 과제 — 거래 내역 검색

수정 파일: [workbench.jsp](../../../exercises/starter/phase-03/jstl/workbench.jsp). Servlet과 테스트를 바꾸지 말고 JSP TODO를 작성하세요. JSP Java scriptlet이나 JDBC/HTTP 호출 없이 JSTL·EL을 사용합니다.

## 데이터

`history`: id, type(DEPOSIT/WITHDRAWAL/TRANSFER), memo, amount(BigDecimal), occurredAt(java.util.Date). 기준 3건이며 금액은 1,500,000원·120,000원·50,000원입니다. 첫 시각은 2026-11-01 09:00(Asia/Seoul)입니다.

## 완성 조건

1. history를 c:forEach로 순회한다. kind가 비었거나 entry.type과 같은 행만 출력한다.

2. fn:trim과 fn:toLowerCase, fn:contains로 메모 부분 검색도 함께 적용한다.

3. 필터를 통과한 건수만 visibleCount로 센다. id=record-count에 숫자만 출력한다.

4. 날짜는 fmt:formatDate pattern="yyyy-MM-dd HH:mm", timeZone="Asia/Seoul"로 표시한다.

5. id, type, memo는 c:out, amount는 fmt:formatNumber(number)와 '원'으로 표시한다.

6. 표시 건수가 0이면 id=empty-message에 '조회 내역이 없습니다.'를 출력한다.

화면 검증에 사용하는 `record-row`, `record-count` 및 문구의 id를 유지하세요. 원본 데이터와 URL 입력의 문자열은 HTML 문맥에 맞게 이스케이프합니다. 데이터에 포함된 꺾쇠 문자는 태그로 실행되면 안 됩니다.

## 수동 시나리오

`?kind=DEPOSIT` → 1건, `?q=급여` → 1건, `?kind=WITHDRAWAL&q=급여` → 0건, `?demo=empty` → 0건.

[실행·검증](verification.md)의 완성 계약 검사를 실행하고, 화면이 보여 주는 상태와 서버의 실제 업무 처리 결과가 어떻게 다른지도 설명해 보세요.
