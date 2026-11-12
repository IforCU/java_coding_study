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
        <title>API 조회 결과 표시 · Phase 06</title>
        <link rel="stylesheet" href="lab.css">
    </head>
    <body>
        <main>
            <header>
                <span class="label">BANK VIEW LAB / PHASE 06</span>
                <h1>API 조회 결과 표시</h1>
                <p>가상 데이터의 조회·표시 연습입니다. 실제 거래·인증·외부 API 호출을 실행하지 않습니다.</p>
            </header>
            <section class="panel">
                <form action="workbench" method="get">
                    <label>
                        API 응답 시나리오
                        <select name="demo">
                            <option value="">성공</option>
                            <option value="empty">성공·빈 목록</option>
                            <option value="unavailable">조회 불가</option>
                        </select>
                    </label>
                    <button type="submit">조회</button>
                    <a href="workbench">초기화</a>
                </form>
            </section>
            <section class="panel" aria-label="조회 결과">
                <%-- TODO 1: 중첩 Map인 api.success를 c:choose로 구분한다. 실패 시 id=api-error에 api.message를 c:out으로 출력한다.
                TODO 2: 성공일 때만 api.data.accounts를 반복하고 class=record-row로 별칭과 원화 잔액을 표시한다.
                TODO 3: 성공 시 id=record-count에 건수만 표시하고 성공·빈 목록이면 id=empty-message에 '조회 계좌가 없습니다.'를 출력한다.
                TODO 4: 성공/실패 모두 id=request-id에 api.requestId를 c:out으로 표시한다.
                TODO 5: 실패에는 성공 표/빈 목록 안내를 출력하지 않는다. 원본 JSON, SQL 오류, 인증정보를 출력하지 않는다.
                이 화면은 API 응답 DTO fixture 표시 과제다. JSP에서 HTTP 호출하거나 Boot API에 JSP를 추가하지 않는다. --%>
                <p>요청 ID: <span id="request-id"><c:out value="${api.requestId}"/></span></p>
                <c:choose>
                    <c:when test="${api.success}">
                        <table>
                            <caption>API 조회 결과</caption>
                            <thead>
                                <tr>
                                    <th>별칭</th>
                                    <th>잔액</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach items="${api.data.accounts}" var="entry">
                                    <tr class="record-row">
                                        <td><c:out value="${entry.name}"/></td>
                                        <td><fmt:formatNumber value="${entry.balance}" type="number" groupingUsed="true"/>원</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        <p>표시 건수: <span id="record-count"><c:out value="${fn:length(api.data.accounts)}"/></span></p>
                        <c:if test="${empty api.data.accounts}">
                            <p id="empty-message">조회 계좌가 없습니다.</p>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <p id="api-error"><c:out value="${api.message}"/></p>
                    </c:otherwise>
                </c:choose>
            </section>
        </main>
    </body>
</html>
