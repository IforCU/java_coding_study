<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>Spring MVC 고객 관리</title>
    </head>
    <body>
        <h1>Phase 3 · 고객 관리</h1>
        <nav>
            <a href="<c:url value='/customers'/>">고객</a>
            |
            <a href="<c:url value='/accounts'/>">계좌</a>
        </nav>
        <p>로컬 학습용 가상 고객만 입력하세요. SUSPENDED 고객은 신규 계좌를 개설할 수 없습니다.</p>
        <form method="post" action="<c:url value='/customers'/>">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
            <label>
                이름
                <input name="name" maxlength="80" required>
            </label>
            <button>고객 등록</button>
        </form>
        <table border="1">
            <caption>고객</caption>
            <thead>
                <tr>
                    <th>식별자</th>
                    <th>이름</th>
                    <th>상태</th>
                    <th>상태 변경</th>
                    <th>계좌</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="customer" items="${customers}">
                    <tr>
                        <td><c:out value="${customer.id}"/></td>
                        <td><c:out value="${customer.name}"/></td>
                        <td><c:out value="${customer.status}"/></td>
                        <td>
                            <form method="post" action="<c:url value='/customers/status'/>">
                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
                                <input type="hidden" name="customerId" value="<c:out value='${customer.id}'/>">
                                <select name="status" aria-label="새 고객 상태">
                                    <option value="ACTIVE">ACTIVE</option>
                                    <option value="SUSPENDED">SUSPENDED</option>
                                </select>
                                <button>변경</button>
                            </form>
                        </td>
                        <td>
                            <c:url var="accountsUrl" value="/accounts">
                                <c:param name="customerId" value="${customer.id}"/>
                            </c:url>
                            <a href="<c:out value='${accountsUrl}'/>">계좌 보기</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <c:if test="${empty customers}">
            <p>등록된 고객이 없습니다.</p>
        </c:if>
    </body>
</html>
