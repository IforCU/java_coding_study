# Phase 5 JSTL 정답과 해설

직접 작성한 뒤 비교하세요. 완성 JSP는 같은 디렉터리의 workbench.jsp입니다. 저장소 루트에서 선택적으로 실행합니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml "-Pjstl-phase5,jstl-solution" verify
```

화면 미리보기는 마지막 verify를 test-compile exec:java로 바꿉니다. 주소는 http://127.0.0.1:8087/workbench이며 starter 서버를 먼저 종료하세요. Linux/macOS는 sh ./mvnw를 사용합니다.

최상위 c:choose에서 권한 결과를 먼저 나눕니다. 거절 분기에는 데이터 표를 출력하지 않습니다. 허용 분기에서만 반복·건수·빈 목록을 처리합니다. fixture Servlet도 거절 시 audit 데이터를 제거합니다. 메모와 요청 ID·마스킹 번호는 c:out으로 출력하며 HTML 숨김만으로 접근 제어를 구현하지 않습니다.

출력 문자열은 c:out으로 보호하고 금액은 이미 검증된 BigDecimal의 표현만 담당합니다. 이 정답은 기본 모듈과 Compose 이미지에 포함하지 않습니다. WAR 이름은 jstl-bank-lab-05-solution.war로 starter·다른 단계와 분리됩니다.
