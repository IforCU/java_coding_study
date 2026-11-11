<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>거래 접수 결과</title>
    </head>
    <body>
        <h1>거래 접수 결과</h1>
        <p><a href="<c:url value='/accounts'/>">계좌 및 이체</a></p>
        <dl>
            <dt>요청 식별자</dt>
            <dd><c:out value="${receipt.requestId}"/></dd>
            <dt>상태</dt>
            <dd><c:out value="${receipt.status}"/></dd>
            <dt>출금 계좌</dt>
            <dd><c:out value="${receipt.sourceId}"/></dd>
            <dt>입금 계좌</dt>
            <dd><c:out value="${receipt.destinationId}"/></dd>
            <dt>금액</dt>
            <dd><c:out value="${receipt.amount}"/>원</dd>
            <dt>영업일</dt>
            <dd><c:out value="${receipt.businessDate}"/></dd>
        </dl>
        <c:if test="${receipt.status == 'COMPLETED'}">
            <form method="post" action="<c:url value='/transfers/${receipt.requestId}/cancel'/>">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                <button>취소 요청 접수</button>
            </form>
        </c:if>
        <p>취소 요청은 CANCEL_REQUESTED로 접수합니다. 이체를 자동으로 되돌리거나 원장을 삭제하지 않습니다.</p>
        <table border="1">
            <caption>이체 원장</caption>
            <thead>
                <tr>
                    <th>계좌</th>
                    <th>유형</th>
                    <th>금액(원)</th>
                    <th>거래 후 잔액(원)</th>
                    <th>시각(UTC)</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${ledger}">
                    <tr>
                        <td><c:out value="${entry.accountId}"/></td>
                        <td><c:out value="${entry.type}"/></td>
                        <td><c:out value="${entry.amount}"/></td>
                        <td><c:out value="${entry.balanceAfter}"/></td>
                        <td><c:out value="${entry.occurredAt}"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </body>
</html>
