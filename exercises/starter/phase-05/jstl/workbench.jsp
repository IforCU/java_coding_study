<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ko_KR"/>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>감사 내역과 마스킹 · Phase 05</title>
        <link rel="stylesheet" href="lab.css">
    </head>
    <body>
        <main>
            <header>
                <span class="label">BANK VIEW LAB / PHASE 05</span>
                <h1>감사 내역과 마스킹</h1>
                <p>가상 데이터의 조회·표시 연습입니다. 실제 거래·인증·외부 API 호출을 실행하지 않습니다.</p>
            </header>
            <section class="panel">
                <form action="workbench" method="get">
                    <label>
                        서버 결과 시나리오
                        <select name="demo">
                            <option value="">허용</option>
                            <option value="empty">빈 내역</option>
                            <option value="denied">거절</option>
                        </select>
                    </label>
                    <label>
                        화면 메모
                        <input name="note" placeholder="특수문자 출력 확인">
                    </label>
                    <button type="submit">조회</button>
                    <a href="workbench">초기화</a>
                </form>
            </section>
            <section class="panel" aria-label="조회 결과">
                <%-- TODO 1: canViewAudit가 false면 id=denied-message에 '감사 내역을 볼 수 없습니다.'만 표시하고 표를 출력하지 않는다.
                TODO 2: 허용된 경우만 audit를 반복하고 class=record-row로 표시한다.
                TODO 3: 이미 마스킹된 entry.account, action, requestId를 c:out으로 출력한다. 원문 복원/재마스킹 금지.
                TODO 4: id=record-count에 건수만 표시하고 허용된 0건이면 id=empty-message에 '감사 내역이 없습니다.'를 출력한다.
                TODO 5: param.note는 id=note에서 c:out으로 출력한다.
                화면 분기는 권한 검사가 아니다. demo는 서버 응답 재현용이며 실제 로그인/인가를 구현하지 않는다. --%>
                <p id="note"><c:out value="${param.note}"/></p>
                <c:choose>
                        <c:when test="${not canViewAudit}">
                            <p id="denied-message">감사 내역을 볼 수 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <table>
                                <caption>감사 내역</caption>
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>계좌</th>
                                        <th>요청 ID</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${audit}" var="entry">
                                        <tr class="record-row">
                                            <td><c:out value="${entry.account}"/></td>
                                            <td><c:out value="${entry.action}"/></td>
                                            <td><c:out value="${entry.requestId}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                            <p>표시 건수: <span id="record-count"><c:out value="${fn:length(audit)}"/></span></p>
                            <c:if test="${empty audit}">
                                <p id="empty-message">감사 내역이 없습니다.</p>
                            </c:if>
                        </c:otherwise>
                    </c:choose>
            </section>
        </main>
    </body>
</html>
