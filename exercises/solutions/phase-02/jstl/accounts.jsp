<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ko_KR"/>
<c:url var="accountsUrl" value="/accounts"/>
<c:url var="emptyUrl" value="/accounts"><c:param name="demo" value="empty"/></c:url>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>내 계좌 조회 · JSTL 실습</title><link rel="stylesheet" href="lab.css"></head><body><main>
<header><span class="label">BANK VIEW LAB / PHASE 2</span><h1>내 계좌 조회</h1>
<p class="muted">가상 계좌로 연습하는 조회 전용 화면입니다. 이름은 표시용이며 인증 기능이 아닙니다.</p></header>
<section class="panel"><form method="get" action="<c:out value='${accountsUrl}'/>">
<label>표시 이름<input name="name" maxlength="30" value="<c:out value='${param.name}'/>" placeholder="예: 학습자"></label>
<label>계좌 종류<select name="type"><option value="">전체</option><option value="DEPOSIT" ${param.type eq 'DEPOSIT' ? 'selected' : ''}>입출금</option><option value="SAVINGS" ${param.type eq 'SAVINGS' ? 'selected' : ''}>적금</option></select></label>
<button type="submit">조회</button><a href="<c:out value='${accountsUrl}'/>">초기화</a></form></section>
<section class="panel" aria-label="조회 결과">
<c:set var="visitor" value="${empty fn:trim(param.name) ? '학습자' : fn:trim(param.name)}"/>
<p id="greeting"><c:out value="${visitor}"/>님, 계좌를 확인하세요.</p>
<p>전체 계좌: <span id="total-count"><c:out value="${fn:length(accounts)}"/></span>개</p>
<c:set var="visibleCount" value="${0}"/>
<div class="table-wrap"><table><caption>계좌 목록</caption>
<thead><tr><th>순번</th><th>별칭</th><th>계좌번호</th><th>종류</th><th>상태</th><th class="amount">잔액</th></tr></thead><tbody>
<c:forEach items="${accounts}" var="account" varStatus="row">
<c:if test="${empty param.type or account.type eq param.type}">
<c:set var="visibleCount" value="${visibleCount + 1}"/>
<tr class="account-row"><td><c:out value="${visibleCount}"/></td>
<td><c:out value="${account.name}"/></td><td><c:out value="${account.number}"/></td>
<td><c:choose><c:when test="${account.type eq 'DEPOSIT'}">입출금</c:when><c:otherwise>적금</c:otherwise></c:choose></td>
<td><c:choose><c:when test="${account.status eq 'ACTIVE'}">정상</c:when><c:when test="${account.status eq 'BLOCKED'}">거래 제한</c:when><c:otherwise>해지</c:otherwise></c:choose></td>
<td class="amount"><fmt:formatNumber value="${account.balance}" type="number" groupingUsed="true"/>원</td></tr>
</c:if></c:forEach></tbody></table></div>
<c:choose><c:when test="${visibleCount eq 0}"><p id="empty-message">조건에 맞는 계좌가 없습니다.</p></c:when>
<c:otherwise><p>조회 결과: <span id="result-count"><c:out value="${visibleCount}"/></span>개</p></c:otherwise></c:choose>
<a id="empty-link" href="<c:out value='${emptyUrl}'/>">빈 목록 테스트</a>
</section><footer><p class="muted">학습용 가상 데이터 · 실제 거래 기능 없음</p></footer>
</main></body></html>
