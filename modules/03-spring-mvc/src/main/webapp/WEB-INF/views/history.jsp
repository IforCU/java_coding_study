<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>기간별 거래 조회</title>
    </head>
    <body>
        <h1>Phase 3 · 기간별 거래 조회</h1>
        <nav>
            <a href="<c:url value='/customers'/>">고객</a>
            |
            <a href="<c:url value='/accounts'/>">계좌</a>
        </nav>
        <p>계좌 <c:out value="${account.id}"/> · 잔액 <c:out value="${account.balance}"/>원</p>
        <form method="get" action="<c:url value='/history'/>">
            <input type="hidden" name="accountId" value="<c:out value='${account.id}'/>">
            <label>
                시작일
                <input type="date" name="from" value="<c:out value='${from}'/>">
            </label>
            <label>
                종료일
                <input type="date" name="to" value="<c:out value='${to}'/>">
            </label>
            <button>검색</button>
        </form>
        <p>날짜는 Asia/Seoul 기준이며 종료일 하루 전체를 포함합니다. 빈 날짜는 해당 경계를 제한하지 않습니다. 기록 시각은 DB가 반환한 시간대 오프셋을 함께 표시합니다.</p>
        <table border="1">
            <caption>거래 내역</caption>
            <thead>
                <tr>
                    <th>거래 식별자</th>
                    <th>시각</th>
                    <th>유형</th>
                    <th>금액(원)</th>
                    <th>거래 후 잔액(원)</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="transaction" items="${transactions}">
                    <tr>
                        <td><c:out value="${transaction.id}"/></td>
                        <td><c:out value="${transaction.occurredAt}"/></td>
                        <td><c:out value="${transaction.type}"/></td>
                        <td><c:out value="${transaction.amount}"/></td>
                        <td><c:out value="${transaction.balanceAfter}"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <c:if test="${empty transactions}">
            <p>검색 결과가 없습니다.</p>
        </c:if>
    </body>
</html>
