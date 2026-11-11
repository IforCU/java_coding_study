<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>거래 내역</title>
    </head>
    <body>
        <h1>Phase 2 · 거래 내역</h1>
        <nav>
            <a href="<c:url value='/customers'/>">고객</a>
            |
            <a href="<c:url value='/accounts'/>">계좌</a>
        </nav>
        <p>계좌 <c:out value="${account.id}"/> / 현재 잔액 <c:out value="${account.balance}"/>원</p>
        <p>이 단계는 저장된 내역 조회를 학습합니다. 입출금 웹 기능은 제공하지 않습니다.</p>
        <table border="1">
            <caption>시간순 거래 기록(UTC)</caption>
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
            <p>거래 내역이 없습니다.</p>
        </c:if>
    </body>
</html>
