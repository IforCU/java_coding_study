# 은행 화면 실습 — JSP EL과 JSTL

Phase 1 뒤에 바로 시작할 수 있는 Phase 2 보조 실습입니다. DB 설정 없이 가상 계좌 목록으로 화면 문법을 익힌 뒤, Phase 2 본편에서 같은 흐름을 JDBC 데이터와 연결합니다. 이전에 작성한 상점 JSP의 상품 목록을 은행의 계좌 목록으로 바꾸는 과제입니다.

## 시작

프로젝트 루트에서 JDK를 준비하고 실행합니다. Windows PowerShell:

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml test-compile exec:java
```

브라우저에서 `http://127.0.0.1:8087/accounts`를 엽니다. 처음에는 화면 틀과 TODO가 보이는 것이 정상입니다. 다른 터미널에서 [starter JSP](../../../exercises/starter/phase-02/jstl/accounts.jsp)를 수정하고 새로고침합니다. JSP 변경을 감지하는 데 잠시 걸릴 수 있습니다. 종료는 실행 터미널에서 Ctrl+C입니다. 기본 포트가 사용 중이면 `"-Dlab.port=18087"`을 명령에 추가합니다. 포트 옵션은 PowerShell에서 따옴표로 감쌉니다.

Linux/macOS는 `.\mvnw.cmd` 대신 `sh ./mvnw`를 사용합니다. 이 서버는 `127.0.0.1`에만 바인딩하며 DB를 시작하거나 수정하지 않습니다.

## 이번에 배우는 문법

| 문법 | 화면에서의 역할 |
| --- | --- |
| `${param.name}`, `${account.balance}` | 요청 파라미터와 Servlet이 전달한 데이터 읽기 |
| `c:set` | 표시 이름과 조회 건수 저장 |
| `fn:trim`, `empty` | 공백 입력과 기본 인사 처리 |
| `fn:length` | 필터 전 전체 계좌 수 |
| `c:forEach`, `c:if` | 계좌 반복과 종류별 표시 |
| `c:choose`, `c:when`, `c:otherwise` | 계좌 종류·상태 이름과 빈 결과 분기 |
| `c:out` | HTML 특수문자가 있는 이름을 안전하게 출력 |
| `fmt:setLocale`, `fmt:formatNumber` | 한국어 로케일과 원 단위 천 단위 구분 |
| `c:url`, `c:param` | 웹 컨텍스트 경로와 쿼리 파라미터를 고려한 링크 |

금액은 Servlet에서 `BigDecimal`로 준비합니다. JSP는 이미 준비된 정수 원 금액을 표시하며 이자 계산, 출금, 잔액 합산, 금액 검증을 담당하지 않습니다. 화면에 '정상'이라고 표시해도 실제 거래 권한이 부여되는 것은 아닙니다.

이름 입력은 학습용 인사입니다. 로그인이나 실제 고객 검색이 아니며, 계좌번호는 이미 마스킹된 가상 값만 제공합니다. JSP에 Java scriptlet이나 JDBC를 작성하지 않습니다.

## 현재 환경과의 차이

참고한 외부 Tomcat 9 파일은 이 프로젝트에 복사하지 않습니다. 이 실습은 기존 Phase 2와 같은 Jakarta Servlet 6.0, Tomcat 10.1, Jakarta Tags 3 조합으로 실행합니다. 태그 URI는 `jakarta.tags.core`, `jakarta.tags.fmt`, `jakarta.tags.functions`를 사용합니다. Tomcat 9용 `javax.servlet` 라이브러리를 함께 넣지 않습니다.

공식 자료: [Jakarta Tags 3.0](https://jakarta.ee/specifications/tags/3.0/), [Tomcat 10.1 JSP 엔진](https://tomcat.apache.org/tomcat-10.1-doc/jasper-howto.html).

다음은 [구조](architecture.md) → [과제](exercise.md) → [힌트](hints.md) → [검증](verification.md) 순서입니다.
