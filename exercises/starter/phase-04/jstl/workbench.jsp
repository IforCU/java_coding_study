<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>이체 상태 안내 · Phase 04</title>
        <link rel="stylesheet" href="lab.css">
    </head>
    <body>
        <main>
            <header>
                <span class="label">BANK VIEW LAB / PHASE 04</span>
                <h1>이체 상태 안내</h1>
                <p>가상 데이터의 조회·표시 연습입니다. 실제 거래·인증·외부 API 호출을 실행하지 않습니다.</p>
            </header>
            <section class="panel">
                <form action="workbench" method="get">
                    <label>
                        처리 상태
                        <select name="status">
                            <option value="">전체</option>
                            <option>COMPLETED</option>
                            <option>PENDING</option>
                            <option>CANCEL_REQUESTED</option>
                        </select>
                    </label>
                    <button type="submit">조회</button>
                    <a href="workbench">초기화</a>
                </form>
            </section>
            <section class="panel" aria-label="조회 결과">
                <%-- TODO 1: transfers를 반복하고 param.status가 비었거나 일치하는 행만 출력한다.
                TODO 2: c:choose로 COMPLETED=완료, PENDING=처리 중, CANCEL_REQUESTED=취소 요청을 표시한다.
                TODO 3: 상태별 안내 문구는 각각 '완료된 이체입니다.', '처리 결과 확인 전 재전송하지 마세요.',
                '취소 요청은 환급 완료가 아닙니다.'로 표시한다. 실제 이체나 취소는 실행하지 않는다.
                TODO 4: c:url/c:param으로 /workbench?id=해당ID 링크를 생성한다. class=detail-link로 둔다.
                TODO 5: id는 c:out, amount는 fmt:formatNumber와 '원'으로 출력한다.
                TODO 6: id=record-count에 표시 건수. 0건이면 id=empty-message에 '이체 내역이 없습니다.'를 출력한다. --%>
                <p class="placeholder">JSTL로 아래 예시 행을 반복·조건 처리하고 데이터와 결과 문구를 완성하세요.</p>
                <table>
                    <caption>실습 결과</caption>
                    <tbody>
                        <tr class="record-row">
                            <td>TODO</td>
                        </tr>
                    </tbody>
                </table>
                <p>표시 건수: <span id="record-count">TODO</span></p>
            </section>
        </main>
    </body>
</html>
