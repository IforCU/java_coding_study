<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="ko_KR"/>
<c:url var="emptyUrl" value="/accounts">
    <c:param name="demo" value="empty"/>
</c:url>
<!doctype html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>내 계좌 조회 · JSTL 실습</title>
        <link rel="stylesheet" href="lab.css">
    </head>
    <body>
        <main>
            <header>
                <span class="label">BANK VIEW LAB / PHASE 2</span>
                <h1>내 계좌 조회</h1>
                <p class="muted">가상 계좌로 연습하는 조회 전용 화면입니다. 이름은 표시용이며 인증 기능이 아닙니다.</p>
            </header>
            <section class="panel">
                <form method="get" action="accounts">
                    <label>
                        표시 이름
                        <input name="name" maxlength="30" placeholder="예: 학습자">
                    </label>
                    <label>
                        계좌 종류
                        <select name="type">
                            <option value="">전체</option>
                            <option value="DEPOSIT">입출금</option>
                            <option value="SAVINGS">적금</option>
                        </select>
                    </label>
                    <button type="submit">조회</button>
                    <a href="accounts">초기화</a>
                </form>
            </section>
            <section class="panel" aria-label="조회 결과">
                <%-- TODO 1: fn:trim(param.name)을 사용해 visitor를 c:set으로 저장한다.
                비어 있으면 '학습자'. c:out으로 '이름님, 계좌를 확인하세요.'를 출력한다.
                인사 문단의 id는 greeting으로 둔다. --%>
                <c:set var="visitor" value="${ empty fn:trim(param.name) ? '학습자' : fn:trim(param.name)}"/>
                <p id="greeting"><c:out value="${visitor}"/>님, 계좌를 확인하세요.</p>
                <%-- TODO 2: fn:length(accounts)로 전체 건수 표시. 숫자만 id=total-count 요소에 출력. --%>
                <p>전체 계좌: <span id="total-count"><c:out value="${fn:length(accounts)}"/></span>개</p>
                <%-- TODO 3: c:set으로 visibleCount=0을 초기화한다. c:forEach와 c:if로
                type이 비었거나 account.type과 같을 때만 아래 tr을 출력하고 건수를 증가시킨다.
                varStatus.index와 필터 후 표시 순번이 어떻게 다른지도 관찰한다. --%>
                <c:set var="visibleCount" value="${0}"/>
                <div class="table-wrap">
                    <table>
                        <caption>계좌 목록</caption>
                        <thead>
                            <tr>
                                <th>순번</th>
                                <th>별칭</th>
                                <th>계좌번호</th>
                                <th>종류</th>
                                <th>상태</th>
                                <th class="amount">잔액</th>
                            </tr>
                        </thead>
                        <tbody>
                              <c:forEach items="${accounts}" var="account" varStatus="row">
                                   <c:if test="${ empty param.type or account.type eq param.type}">
                                        <c:set var="visibleCount" value="${visibleCount + 1}"/>
                                        <tr class="account-row">
                                             <td><c:out value="${visibleCount}"/></td>
                                             <%-- TODO 4: account.name, account.number는 c:out으로 출력한다. --%>
                                             <td><c:out value="${account.name}"/></td>
                                             <td><c:out value="${account.number}"/></td>
                                             <%-- TODO 5: c:choose로 DEPOSIT은 입출금, SAVINGS는 적금으로 표시한다.
                                             상태 ACTIVE는 정상, BLOCKED는 거래 제한, 그 외 CLOSED는 해지로 표시한다.
                                             이 표시는 실제 거래 허용 여부를 결정하지 않는다. --%>
                                             <c:choose>
                                                  <c:when test="${account.type eq 'DEPOSIT'}">
                                                       <td>입출금</td>
                                                  </c:when>
                                                  <c:otherwise>
                                                       <td>적금</td>
                                                  </c:otherwise>
                                             </c:choose>
                                             
                                             <c:choose>
                                                  <c:when test="${account.status eq 'ACTIVE'}">
                                                       <td>정상</td>
                                                  </c:when>
                                                  <c:when test="${account.status eq 'BLOCKED'}">
                                                       <td>거래 제한</td>
                                                  </c:when>
                                                  <c:otherwise>
                                                       <td>해지</td>
                                                  </c:otherwise>
                                             </c:choose>

                                             <%-- TODO 6: fmt:setLocale(ko_KR), fmt:formatNumber(number, groupingUsed=true)로
                                             잔액에 천 단위 구분과 '원' 표시. 합계 계산·반올림은 하지 않는다. --%>
                                             
                                             <td class="amount"><fmt:formatNumber value="${account.balance}" type="number" groupingUsed="true"/>원</td>
                                        </tr>
                                   </c:if>
                              </c:forEach>
                        </tbody>
                    </table>
                </div>
                <%-- TODO 7: visibleCount가 0이면 id=empty-message에 '조건에 맞는 계좌가 없습니다.'
                그 외에는 id=result-count에 건수만 표시한다. 원본 빈 목록과 필터 결과 0건 모두 처리. --%>
                <c:choose>
                    <c:when test="${visibleCount eq 0}">
                        <p id="empty-message">조건에 맞는 계좌가 없습니다.</p>
                    </c:when>
                    <c:otherwise>
                        <p>조회 결과: <span id="result-count"><c:out value="${visibleCount}"/></span>개</p>
                    </c:otherwise>
                </c:choose>
                <%-- TODO 8: c:url과 c:param으로 초기화 링크와 demo=empty 링크를 생성한다.
                빈 목록 링크의 id는 empty-link. 모든 문자열 출력은 c:out으로 보호한다. --%>
                <a id="empty-link" href="<c:out value='${emptyUrl}'/>">빈 목록 테스트</a>
            </section>
            <footer>
                <p class="muted">검증할 상태: 전체 · 종류별 · 검색 결과 없음 · 원본 빈 목록 · HTML 특수문자</p>
            </footer>
        </main>
    </body>
</html>
