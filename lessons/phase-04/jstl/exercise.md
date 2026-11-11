# Phase 4 화면 과제 — 이체 상태 안내

수정 파일: [workbench.jsp](../../../exercises/starter/phase-04/jstl/workbench.jsp). Servlet과 테스트를 바꾸지 말고 JSP TODO를 작성하세요. JSP Java scriptlet이나 JDBC/HTTP 호출 없이 JSTL·EL을 사용합니다.

## 데이터

`transfers`: id, status(COMPLETED/PENDING/CANCEL_REQUESTED), amount(BigDecimal). T01은 100,000원 완료, T02는 50,000원 처리 중, T03은 30,000원 취소 요청입니다.

## 완성 조건

1. transfers를 반복하고 param.status가 비었거나 일치하는 행만 출력한다.

2. c:choose로 COMPLETED=완료, PENDING=처리 중, CANCEL_REQUESTED=취소 요청을 표시한다.

3. 상태별 안내 문구는 각각 '완료된 이체입니다.', '처리 결과 확인 전 재전송하지 마세요.',

        '취소 요청은 환급 완료가 아닙니다.'로 표시한다. 실제 이체나 취소는 실행하지 않는다.

4. c:url/c:param으로 /workbench?id=해당ID 링크를 생성한다. class=detail-link로 둔다.

5. id는 c:out, amount는 fmt:formatNumber와 '원'으로 출력한다.

6. id=record-count에 표시 건수. 0건이면 id=empty-message에 '이체 내역이 없습니다.'를 출력한다.

화면 검증에 사용하는 `record-row`, `record-count` 및 문구의 id를 유지하세요. 원본 데이터와 URL 입력의 문자열은 HTML 문맥에 맞게 이스케이프합니다. 데이터에 포함된 꺾쇠 문자는 태그로 실행되면 안 됩니다.

## 수동 시나리오

`?status=PENDING` → T02 1건, `?status=CANCEL_REQUESTED` → T03 1건, `?status=UNKNOWN` 및 `?demo=empty` → 0건.

[실행·검증](verification.md)의 완성 계약 검사를 실행하고, 화면이 보여 주는 상태와 서버의 실제 업무 처리 결과가 어떻게 다른지도 설명해 보세요.
