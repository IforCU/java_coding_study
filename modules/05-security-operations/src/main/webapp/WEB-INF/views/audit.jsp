<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="ko"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width"><title>감사 기록</title></head><body>
<h1>Phase 5 · 최근 감사 기록 50개</h1><p><a href="<c:url value='/profile'/>">내 프로필</a></p><p>요청 추적 ID: <c:out value="${requestId}"/></p>
<table border="1"><caption>허용·거부 기록</caption><thead><tr><th>시각(UTC)</th><th>행위자(마스킹)</th><th>작업</th><th>결과</th><th>요청 ID</th></tr></thead><tbody><c:forEach var="event" items="${events}"><tr><td><c:out value="${event.occurredAt}"/></td><td><c:out value="${event.actor}"/></td><td><c:out value="${event.action}"/></td><td><c:out value="${event.outcome}"/></td><td><c:out value="${event.requestId}"/></td></tr></c:forEach></tbody></table>
</body></html>
