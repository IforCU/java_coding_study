# Phase 6 정답과 해설

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

정답은 정상 API와 분리된 단독 정책 모듈입니다. TODO를 직접 구현한 뒤 비교합니다.

null은 요청 파라미터 누락을 의미하므로 기본값 50을 반환합니다. 값이 제공되면 1~200인지 확인하고 유효한 값은 그대로 반환합니다. 범위 밖 입력을 1이나 200으로 자동 보정하지 않고 `IllegalArgumentException`으로 거절합니다. 예외 메시지에 전달된 값을 넣지 않습니다.

매 호출의 인자만 사용하므로 이전 요청이 다음 기본값을 바꾸지 않습니다. `Integer`를 사용해 누락과 0을 구분하고, null 검사를 먼저 하여 언박싱 오류를 피합니다. 실제 API는 HTTP 변환·Bean Validation·공통 오류 응답을 통해 대응하는 검증을 수행합니다. 이 실습 클래스는 정상 API에서 호출하지 않습니다.

이 정책은 최대 반환 건수 제한입니다. offset, cursor, 전체 건수나 다음 페이지 토큰을 제공하는 페이지 탐색 구현이 아닙니다. 응답 크기를 제한해도 모든 느린 쿼리 문제가 해결되는 것은 아니므로 인덱스와 기간 조건도 별도로 확인합니다.

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -Psolution-phase6 -pl exercises/solutions/phase-06 -am verify
```

starter의 같은 계약 테스트를 사용합니다. 실제 실행 결과는 루트 `PROGRESS.md`에서 확인합니다.
