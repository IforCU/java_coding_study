# Phase 2 정답과 해설

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

이 디렉터리는 과제를 시도한 뒤 비교하는 자료입니다. 정상 WAR와 기본 빌드에 포함되지 않습니다.

## 고객 이름 정규화

`CustomerNameValidator`는 null을 먼저 거절하고 `strip()`으로 앞뒤 Java 공백을 제거합니다. 제거한 결과의 `String.length()`가 1~80인지 검사한 뒤 그대로 반환합니다. 길이 검사를 정규화 전에 하면 공백을 포함한 유효한 80자 이름을 잘못 거절할 수 있습니다. 내부 공백과 대소문자는 바꾸지 않습니다.

`trim()`은 제한된 문자 범위를 처리하므로 이 과제의 `Character.isWhitespace` 기준 공백에는 `strip()`을 사용합니다. 이름 검증과 SQL 바인딩, HTML 출력 처리는 서로 다른 책임입니다. 이름 길이 검사를 통과했더라도 SQL은 PreparedStatement로 바인딩하고 출력 위치에 맞춰 이스케이프해야 합니다.

## 격리된 JSP 분석 과제 해설

`legacy-customer.jsp.txt`는 실행하지 않는 텍스트 자료입니다. 원본을 WAR로 옮겨 동작을 재현할 필요가 없습니다.

| 문제 | 개선 책임 |
| --- | --- |
| JSP가 요청 값·SQL·HTML을 함께 처리 | Servlet은 HTTP, Service는 입력·업무 규칙, DAO는 JDBC, JSP는 표현으로 분리 |
| 이름을 SQL 문자열에 연결 | DAO가 고정 SQL과 바인딩 값을 구분하도록 변경 |
| Connection·Statement·ResultSet 미정리 | DAO의 try-with-resources로 예외 경로까지 정리 |
| 예외 객체를 그대로 화면에 출력 | 내부 오류를 제한된 사용자 메시지와 상태 코드로 변환 |
| DB 값을 HTML에 직접 출력 | JSP의 `c:out` 등 문맥에 맞는 출력 이스케이프 사용 |

연결 풀에서 빌린 Connection의 `close()`는 일반적인 사용 경로에서 연결을 풀로 반환합니다. Connection을 닫지 않으면 다음 요청이 빌릴 수 있는 연결이 줄어듭니다. 전체 풀은 애플리케이션 종료 시 닫습니다.

`javax.sql.DataSource`는 Java SE JDBC API이므로 `javax.servlet`과 다른 패키지입니다. Jakarta Servlet WAR가 `javax.sql`을 사용하는 것은 Servlet API 혼합에 해당하지 않습니다.

## 선택 검증

Windows PowerShell:

```powershell
.\mvnw.cmd -Psolution-phase2 -pl exercises/solutions/phase-02 -am verify
```

Linux/macOS:

```sh
sh ./mvnw -Psolution-phase2 -pl exercises/solutions/phase-02 -am verify
```

정답 모듈도 starter의 동일 계약 테스트를 사용합니다. 테스트를 변경해 기대값을 낮추지 말고 본인의 구현과 경계 조건을 비교하세요. 실제 결과는 루트 `PROGRESS.md`에서 확인합니다.
