# Phase 5 JSTL — 감사 내역과 마스킹

이번 보조 실습의 주제는 **권한 거절 표시·마스킹·안전한 감사 내역 출력**입니다. Phase 2 계좌 목록 실습과 겹치는 기본 설명은 [기존 교재](../../phase-02/jstl/lesson.md)를 참고합니다. 데이터는 가상 fixture이며 DB를 시작하거나 실제 거래·인증·API를 호출하지 않습니다.

Windows PowerShell에서 저장소 루트 기준으로 실행합니다. JDK 17을 준비하고 Maven은 Wrapper가 내려받습니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-phase5 test-compile exec:java
```

브라우저에서 `http://127.0.0.1:8087/workbench`를 엽니다. starter JSP를 수정한 뒤 새로고침하며, 종료는 Ctrl+C입니다. 기본 포트가 점유되면 `"-Dlab.port=18087"`을 추가합니다. Linux/macOS는 `sh ./mvnw`로 시작하면 됩니다.

본편 WAR의 Spring/Servlet 설정을 바꾸지 않습니다. 화면 보조 실습은 기존 lab의 Tomcat 10.1 / Servlet 6 / Jakarta Tags 3 환경에서 독립 실행합니다.

[구조](architecture.md), [과제](exercise.md), [힌트](hints.md), [검증](verification.md) 순서로 진행하세요. 정답 실행·해설은 정답 디렉터리에만 있습니다.
