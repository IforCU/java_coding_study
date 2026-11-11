<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ko_KR"/>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>이체 상태 안내 · Phase 04</title><link rel="stylesheet" href="lab.css"></head><body><main>
<header><span class="label">BANK VIEW LAB / PHASE 04</span><h1>이체 상태 안내</h1><p>가상 데이터의 조회·표시 연습입니다. 실제 거래·인증·외부 API 호출을 실행하지 않습니다.</p></header>
<section class="panel"><form action="workbench" method="get"><label>처리 상태<select name="status"><option value="">전체</option><option>COMPLETED</option><option>PENDING</option><option>CANCEL_REQUESTED</option></select></label><button type="submit">조회</button><a href="workbench">초기화</a></form></section>
<section class="panel" aria-label="조회 결과">
<c:set var="visibleCount" value="${0}"/>
<table><caption>이체 내역</caption><thead><tr><th>ID</th><th>금액</th><th>상태</th><th>안내</th><th>조회</th></tr></thead><tbody>
<c:forEach items="${transfers}" var="entry">
<c:if test="${empty param.status or entry.status eq param.status}">
<c:set var="visibleCount" value="${visibleCount + 1}"/>
<c:url var="detailUrl" value="/workbench"><c:param name="id" value="${entry.id}"/></c:url>
<tr class="record-row"><td><c:out value="${entry.id}"/></td><td><fmt:formatNumber value="${entry.amount}" type="number" groupingUsed="true"/>원</td>
<c:choose>
<c:when test="${entry.status eq 'COMPLETED'}"><td>완료</td><td>완료된 이체입니다.</td></c:when>
<c:when test="${entry.status eq 'PENDING'}"><td>처리 중</td><td>처리 결과 확인 전 재전송하지 마세요.</td></c:when>
<c:otherwise><td>취소 요청</td><td>취소 요청은 환급 완료가 아닙니다.</td></c:otherwise>
</c:choose>
<td><a class="detail-link" href="<c:out value='${detailUrl}'/>">요청 ID 보기</a></td></tr>
</c:if></c:forEach></tbody></table>
<p>표시 건수: <span id="record-count"><c:out value="${visibleCount}"/></span></p>
<c:if test="${visibleCount eq 0}"><p id="empty-message">이체 내역이 없습니다.</p></c:if>
<c:if test="${not empty param.id}"><p id="selected-id"><c:out value="${param.id}"/></p></c:if>
</section></main></body></html>
