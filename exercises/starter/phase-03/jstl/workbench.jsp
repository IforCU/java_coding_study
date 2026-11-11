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
        <title>거래 내역 검색 · Phase 03</title>
        <link rel="stylesheet" href="lab.css">
    </head>
    <body>
        <main>
            <header>
                <span class="label">BANK VIEW LAB / PHASE 03</span>
                <h1>거래 내역 검색</h1>
                <p>가상 데이터의 조회·표시 연습입니다. 실제 거래·인증·외부 API 호출을 실행하지 않습니다.</p>
            </header>
            <section class="panel">
                <form action="workbench" method="get">
                    <label>
                        거래 종류
                        <select name="kind">
                            <option value="">전체</option>
                            <option>DEPOSIT</option>
                            <option>WITHDRAWAL</option>
                            <option>TRANSFER</option>
                        </select>
                    </label>
                    <label>
                        메모 검색
                        <input name="q" placeholder="예: 급여">
                    </label>
                    <button type="submit">조회</button>
                    <a href="workbench">초기화</a>
                </form>
            </section>
            <section class="panel" aria-label="조회 결과">
                <%-- TODO 1: history를 c:forEach로 순회한다. kind가 비었거나 entry.type과 같은 행만 출력한다.
                TODO 2: fn:trim과 fn:toLowerCase, fn:contains로 메모 부분 검색도 함께 적용한다.
                TODO 3: 필터를 통과한 건수만 visibleCount로 센다. id=record-count에 숫자만 출력한다.
                TODO 4: 날짜는 fmt:formatDate pattern="yyyy-MM-dd HH:mm", timeZone="Asia/Seoul"로 표시한다.
                TODO 5: id, type, memo는 c:out, amount는 fmt:formatNumber(number)와 '원'으로 표시한다.
                TODO 6: 표시 건수가 0이면 id=empty-message에 '조회 내역이 없습니다.'를 출력한다. --%>
                <c:set var="visibleCount" value="${0}"/>
                <table>
                    <caption>거래 내역</caption>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>종류</th>
                            <th>메모</th>
                            <th>시각</th>
                            <th>금액</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${history}" var="entry">
                            <c:if test="${(empty param.kind or entry.type eq param.kind) and fn:contains(fn:toLowerCase(entry.memo), fn:toLowerCase(fn:trim(param.q)))}">
                                <c:set var="visibleCount" value="${visibleCount + 1}"/>    
                                <tr class="record-row">
                                    <td><c:out value="${entry.id}"/></td>
                                    <td><c:out value="${entry.type}"/></td>
                                    <td><c:out value="${entry.memo}"/></td>
                                    <td><fmt:formatDate value="${entry.occurredAt}" pattern="yyyy-MM-dd HH:mm" timeZone="Asia/Seoul"/></td>
                                    <td><fmt:formatNumber value="${entry.amount}" type="number" groupingUsed="true"/>원</td>
                                </tr>
                            </c:if>
                        </c:forEach>
                    </tbody>
                </table>
                <p>표시 건수: <span id="record-count"><c:out value="${visibleCount}"/></span></p>
                <c:if test="${visibleCount eq 0}">
                    <p id="empty-message">조회 내역이 없습니다.</p>
                </c:if>
            </section>
        </main>
    </body>
</html>
