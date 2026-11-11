<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width">
        <title>보안 학습 프로필</title>
    </head>
    <body>
        <h1>Phase 5 · 내 프로필</h1>
        <p>요청 추적 ID: <c:out value="${requestId}"/></p>
        <p><a href="<c:url value='/admin/audit'/>">관리자 감사 기록</a></p>
        <dl>
            <dt>사용자</dt>
            <dd><c:out value="${profile.username}"/></dd>
            <dt>표시 이름</dt>
            <dd><c:out value="${profile.displayName}"/></dd>
            <dt>이메일</dt>
            <dd><c:out value="${profile.email}"/></dd>
            <dt>계좌 힌트</dt>
            <dd><c:out value="${profile.accountHint}"/></dd>
            <dt>메모</dt>
            <dd><c:out value="${profile.note}"/></dd>
        </dl>
        <form method="post" action="<c:url value='/profile/note'/>">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>">
            <label>
                내 메모
                <textarea name="note" maxlength="100" required><c:out value="${profile.note}"/></textarea>
            </label>
            <button>내 메모 저장</button>
        </form>
        <form method="post" action="<c:url value='/logout'/>">
            <input type="hidden" name="<c:out value='${_csrf.parameterName}'/>" value="<c:out value='${_csrf.token}'/>">
            <button>로그아웃</button>
        </form>
    </body>
</html>
