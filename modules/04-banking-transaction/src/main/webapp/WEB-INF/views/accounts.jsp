<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width"><title>금융 거래 학습</title></head>
<body><h1>Phase 4 · 계좌 이체</h1><p>로컬 학습 계좌입니다. 일일 한도는 Asia/Seoul 영업일 기준으로 출금 계좌에 적용합니다.</p>
<table border="1"><caption>계좌</caption><thead><tr><th>식별자</th><th>잔액(원)</th><th>일일 한도(원)</th><th>버전</th></tr></thead><tbody><c:forEach var="account" items="${accounts}"><tr><td><c:out value="${account.id}"/></td><td><c:out value="${account.balance}"/></td><td><c:out value="${account.dailyLimit}"/></td><td><c:out value="${account.version}"/></td></tr></c:forEach></tbody></table>
<form method="post" action="<c:url value='/transfers'/>"><input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>">
<p><label>요청 식별자 <input name="requestId" value="<c:out value='${requestId}'/>" size="38" required></label></p>
<p><label>출금 <select name="sourceId" required><c:forEach var="account" items="${accounts}"><option value="<c:out value='${account.id}'/>"><c:out value="${account.id}"/></option></c:forEach></select></label></p>
<p><label>입금 <select name="destinationId" required><c:forEach var="account" items="${accounts}"><option value="<c:out value='${account.id}'/>"><c:out value="${account.id}"/></option></c:forEach></select></label></p>
<p><label>금액(원) <input name="amount" inputmode="numeric" pattern="[0-9]+" maxlength="19" required></label><button>이체 요청</button></p></form>
<p>응답을 받지 못하면 같은 요청 식별자와 같은 계좌·금액으로 재요청하세요. 같은 식별자에 다른 내용을 보내면 거부합니다.</p>
</body></html>
