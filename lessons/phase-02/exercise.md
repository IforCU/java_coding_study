# Phase 2 실습 — 이름 검증과 레거시 JSP 분석

JSP를 직접 작성하는 과제는 별도의 [내 계좌 조회 화면 실습](jstl/exercise.md)에 있습니다. 실행부터 시작하려면 [JSTL 교재](jstl/lesson.md)를 읽으세요. 아래 기존 Java 과제와 독립적으로 진행할 수 있습니다.

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

먼저 정상 애플리케이션에서 고객 등록→고객 선택→계좌 등록→거래 내역 조회 경로를 관찰합니다. 개발자 도구의 Network에서 GET, POST, 303, 후속 GET을 구분하세요.

## 과제 1: 고객 이름 검증 TODO

수정할 파일은 [`CustomerNameValidator.java`](../../exercises/starter/phase-02/src/main/java/kr/banklearning/exercise/CustomerNameValidator.java)입니다. 정상 WAR와 별도인 실습 JAR 모듈입니다.

`normalize(String name)`은 다음 계약을 지킵니다.

- null은 `IllegalArgumentException`으로 거절합니다.
- 앞뒤 Java 공백 문자를 제거합니다. Java `Character.isWhitespace` 기준을 사용합니다.
- 정리한 결과의 `String.length()`가 1~80이어야 합니다.
- 비어 있거나 80자를 넘으면 `IllegalArgumentException`으로 거절합니다.
- 내부 공백, 한글, 대소문자는 보존합니다.
- 유효한 이름은 정리한 문자열을 반환합니다.

길이는 Java 문자열의 UTF-16 코드 단위 기준입니다. HTML 태그 제거, SQL 처리, 화면 출력은 이 작은 함수의 책임에 추가하지 않습니다. 검증과 출력 처리는 다른 경계에서 학습합니다.

Windows PowerShell:

```powershell
.\mvnw.cmd -Pexercise-phase2 -pl exercises/starter/phase-02 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Pexercise-phase2 -pl exercises/starter/phase-02 -am verify
```

TODO를 구현하기 전에는 실패해야 합니다. 테스트를 변경해 통과시키지 말고 구현을 수정합니다. 힌트는 [hints.md](hints.md)에 있습니다.

## 과제 2: 격리된 예전 JSP 분석

읽을 파일은 [`legacy-customer.jsp.txt`](../../exercises/starter/phase-02/legacy-customer.jsp.txt)입니다. 이 파일은 실행하지 않는 분석 텍스트이며 WAR나 `webapp`에 복사하지 않습니다.

다음 항목을 표로 작성하세요.

1. 요청 입력, SQL, HTML, 오류 처리를 담당하는 줄은 각각 어디인가요?
2. 정상·실패 경로에서 닫혀야 할 자원은 무엇인가요?
3. 입력 값과 SQL 구조는 어디에서 섞이나요?
4. 브라우저에 내부 정보나 해석되지 않아야 할 값이 전달되는 곳은 어디인가요?
5. 변경할 파일과 회귀 검증 항목을 어떤 기준으로 나눌 수 있나요?

분석 기록 양식은 [JSP 리팩터링 학습 문서](../../docs/legacy-analysis/jsp-refactoring.md)를 참고합니다. 이 과제는 취약한 코드를 실제 서비스로 띄워 확인하는 절차를 요구하지 않습니다.

## 과제 3: 호출 흐름 설명

`GET /accounts` 요청 하나를 골라 Servlet, Service, DAO, SQL 바인딩, ResultSet, DTO, JSP까지 함수 이름을 연결합니다. 선택 고객을 바꿨을 때 request parameter와 session attribute가 어디에서 읽히는지도 표시하세요.

HTTP 폼과 서버 입력 검증, SQL 값 바인딩, HTML 출력 이스케이프가 각각 어떤 문제를 다루는지 설명합니다. 완성 정답과 해설은 solutions에만 보관합니다.
