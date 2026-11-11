# Phase 6 JSTL — API 조회 결과 표시

이번 보조 실습의 주제는 **중첩 API DTO 표시·성공/빈 목록/실패 구분**입니다. Phase 2 계좌 목록 실습과 겹치는 기본 설명은 [기존 교재](../../phase-02/jstl/lesson.md)를 참고합니다. 데이터는 가상 fixture이며 DB를 시작하거나 실제 거래·인증·API를 호출하지 않습니다.

Windows PowerShell에서 저장소 루트 기준으로 실행합니다. JDK 17을 준비하고 Maven은 Wrapper가 내려받습니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-phase6 test-compile exec:java
```

브라우저에서 `http://127.0.0.1:8087/workbench`를 엽니다. starter JSP를 수정한 뒤 새로고침하며, 종료는 Ctrl+C입니다. 기본 포트가 점유되면 `"-Dlab.port=18087"`을 추가합니다. Linux/macOS는 `sh ./mvnw`로 시작하면 됩니다.

본편 Phase 6은 Boot REST API 그대로 유지합니다. 이 화면은 별도의 Tomcat 10.1 실습 WAR에서 표시용 API DTO를 읽습니다.

[구조](architecture.md), [과제](exercise.md), [힌트](hints.md), [검증](verification.md) 순서로 진행하세요. 정답 실행·해설은 정답 디렉터리에만 있습니다.
