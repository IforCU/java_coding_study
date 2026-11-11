# Phase 6 화면 과제 — API 조회 결과 표시

수정 파일: [workbench.jsp](../../../exercises/starter/phase-06/jstl/workbench.jsp). Servlet과 테스트를 바꾸지 말고 JSP TODO를 작성하세요. JSP Java scriptlet이나 JDBC/HTTP 호출 없이 JSTL·EL을 사용합니다.

## 데이터

`api`: success(boolean), message(안전한 사용자 메시지), requestId, data.accounts(별칭 name과 balance 목록). 성공 기본 목록은 2건이며 잔액은 1,250,000원·350,000원입니다. 원본 JSON이 아닌 표시용 Map fixture입니다.

## 완성 조건

1. 중첩 Map인 api.success를 c:choose로 구분한다. 실패 시 id=api-error에 api.message를 c:out으로 출력한다.

2. 성공일 때만 api.data.accounts를 반복하고 class=record-row로 별칭과 원화 잔액을 표시한다.

3. 성공 시 id=record-count에 건수만 표시하고 성공·빈 목록이면 id=empty-message에 '조회 계좌가 없습니다.'를 출력한다.

4. 성공/실패 모두 id=request-id에 api.requestId를 c:out으로 표시한다.

5. 실패에는 성공 표/빈 목록 안내를 출력하지 않는다. 원본 JSON, SQL 오류, 인증정보를 출력하지 않는다.

이 화면은 API 응답 DTO fixture 표시 과제다. JSP에서 HTTP 호출하거나 Boot API에 JSP를 추가하지 않는다.

화면 검증에 사용하는 `record-row`, `record-count` 및 문구의 id를 유지하세요. 원본 데이터와 URL 입력의 문자열은 HTML 문맥에 맞게 이스케이프합니다. 데이터에 포함된 꺾쇠 문자는 태그로 실행되면 안 됩니다.

## 수동 시나리오

기본 → 성공 2건, `?demo=empty` → 성공 0건, `?demo=unavailable` → 실패 메시지와 요청 ID(성공 표/건수/빈 결과 문구 없음).

[실행·검증](verification.md)의 완성 계약 검사를 실행하고, 화면이 보여 주는 상태와 서버의 실제 업무 처리 결과가 어떻게 다른지도 설명해 보세요.
