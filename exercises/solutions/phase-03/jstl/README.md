# Phase 3 JSTL 정답과 해설

직접 작성한 뒤 비교하세요. 완성 JSP는 같은 디렉터리의 workbench.jsp입니다. 저장소 루트에서 선택적으로 실행합니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml "-Pjstl-phase3,jstl-solution" verify
```

화면 미리보기는 마지막 verify를 test-compile exec:java로 바꿉니다. 주소는 http://127.0.0.1:8087/workbench이며 starter 서버를 먼저 종료하세요. Linux/macOS는 sh ./mvnw를 사용합니다.

반복 안에서 두 검색 조건을 함께 검사하고 그 블록 안에서 건수를 증가시킵니다. fn:trim/toLowerCase/contains로 표시용 메모 검색을 수행합니다. fmt:formatDate에 Asia/Seoul을 명시해 실행 PC의 타임존과 분리합니다. 원문 메모는 c:out으로 보호합니다.

출력 문자열은 c:out으로 보호하고 금액은 이미 검증된 BigDecimal의 표현만 담당합니다. 이 정답은 기본 모듈과 Compose 이미지에 포함하지 않습니다. WAR 이름은 jstl-bank-lab-03-solution.war로 starter·다른 단계와 분리됩니다.
