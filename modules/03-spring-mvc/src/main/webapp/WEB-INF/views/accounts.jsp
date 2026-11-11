<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>Spring MVC 계좌 관리</title>
    </head>
    <body>
        <h1>Phase 3 · 계좌 관리</h1>
        <nav>
            <a href="<c:url value='/customers'/>">고객</a>
            |
            <a href="<c:url value='/accounts'/>">전체 계좌</a>
        </nav>
        <form method="post" action="<c:url value='/accounts'/>">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <label>
                고객
                <select name="customerId" required>
                    <c:forEach var="customer" items="${customers}">
                        <option value="<c:out value='${customer.id}'/>"><c:out value="${customer.name}"/> (<c:out value="${customer.status}"/>)</option>
                    </c:forEach>
                </select>
            </label>
            <button>잔액 0원 계좌 개설</button>
        </form>
        <table border="1">
            <caption>계좌</caption>
            <thead>
                <tr>
                    <th>식별자</th>
                    <th>고객</th>
                    <th>잔액(원)</th>
                    <th>거래</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="account" items="${accounts}">
                    <tr>
                        <td><c:out value="${account.id}"/></td>
                        <td><c:out value="${account.customerId}"/></td>
                        <td><c:out value="${account.balance}"/></td>
                        <td>
                            <c:url var="historyUrl" value="/history">
                                <c:param name="accountId" value="${account.id}"/>
                            </c:url>
                            <a href="<c:out value='${historyUrl}'/>">내역 조회</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <c:if test="${empty accounts}">
            <p>표시할 계좌가 없습니다.</p>
        </c:if>
    </body>
</html>
