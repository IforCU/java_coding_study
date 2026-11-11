# Phase 6 JSTL 정답과 해설

직접 작성한 뒤 비교하세요. 완성 JSP는 같은 디렉터리의 workbench.jsp입니다. 저장소 루트에서 선택적으로 실행합니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml "-Pjstl-phase6,jstl-solution" verify
```

화면 미리보기는 마지막 verify를 test-compile exec:java로 바꿉니다. 주소는 http://127.0.0.1:8087/workbench이며 starter 서버를 먼저 종료하세요. Linux/macOS는 sh ./mvnw를 사용합니다.

성공 여부를 먼저 c:choose로 나눈 다음 성공 블록에서 중첩 속성 api.data.accounts를 반복합니다. 실패 메시지와 requestId는 c:out으로 표시합니다. 이 fixture는 응답 구조를 읽는 UI 과제이며 실제 API 연계가 아닙니다. Boot API에 JSP 엔진을 추가하지 않고 실습용 WAR에서만 표시합니다.

출력 문자열은 c:out으로 보호하고 금액은 이미 검증된 BigDecimal의 표현만 담당합니다. 이 정답은 기본 모듈과 Compose 이미지에 포함하지 않습니다. WAR 이름은 jstl-bank-lab-06-solution.war로 starter·다른 단계와 분리됩니다.
