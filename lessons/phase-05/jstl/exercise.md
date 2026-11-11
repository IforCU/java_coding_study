# Phase 5 화면 과제 — 감사 내역과 마스킹

수정 파일: [workbench.jsp](../../../exercises/starter/phase-05/jstl/workbench.jsp). Servlet과 테스트를 바꾸지 말고 JSP TODO를 작성하세요. JSP Java scriptlet이나 JDBC/HTTP 호출 없이 JSTL·EL을 사용합니다.

## 데이터

`canViewAudit`: fixture 서버가 결정한 boolean. `audit`: account(이미 마스킹된 문자열), action, requestId, id. 허용 시 2건이며 거절 시 Servlet이 빈 목록을 전달합니다. 원문 개인정보는 없습니다.

## 완성 조건

1. canViewAudit가 false면 id=denied-message에 '감사 내역을 볼 수 없습니다.'만 표시하고 표를 출력하지 않는다.

2. 허용된 경우만 audit를 반복하고 class=record-row로 표시한다.

3. 이미 마스킹된 entry.account, action, requestId를 c:out으로 출력한다. 원문 복원/재마스킹 금지.

4. id=record-count에 건수만 표시하고 허용된 0건이면 id=empty-message에 '감사 내역이 없습니다.'를 출력한다.

5. param.note는 id=note에서 c:out으로 출력한다.

화면 분기는 권한 검사가 아니다. demo는 서버 응답 재현용이며 실제 로그인/인가를 구현하지 않는다.

화면 검증에 사용하는 `record-row`, `record-count` 및 문구의 id를 유지하세요. 원본 데이터와 URL 입력의 문자열은 HTML 문맥에 맞게 이스케이프합니다. 데이터에 포함된 꺾쇠 문자는 태그로 실행되면 안 됩니다.

## 수동 시나리오

`?demo=denied` → 거절 문구만 표시(표/건수/빈 결과 문구 없음), `?demo=empty` → 허용·0건, `?note=%3Cscript%3E` → 문자로 출력.

[실행·검증](verification.md)의 완성 계약 검사를 실행하고, 화면이 보여 주는 상태와 서버의 실제 업무 처리 결과가 어떻게 다른지도 설명해 보세요.
