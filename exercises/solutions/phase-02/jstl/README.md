# 은행 JSTL 화면 실습 정답과 해설

먼저 starter를 직접 작성하고 필요할 때만 이 디렉터리를 엽니다. 현재 `accounts.jsp`가 완성 예시입니다.

저장소 루트에서 정답을 검증합니다.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-solution verify
```

정답 화면을 별도로 미리 보려면 다음을 사용합니다. 기본 주소와 포트는 starter와 같으므로 기존 서버를 먼저 종료하세요.

```powershell
.\mvnw.cmd -f exercises/jstl-bank-lab/pom.xml -Pjstl-solution test-compile exec:java
```

Linux/macOS는 `sh ./mvnw`를 사용합니다. 정답 프로필은 `jstl-bank-lab-solution.war`, starter는 `jstl-bank-lab-starter.war`로 패키징되어 서로 분리됩니다. 루트 기본 빌드와 Compose 실습 이미지는 정답을 포함하지 않습니다.

## 해설

인사는 `fn:trim`과 `empty`로 기본값을 고르고 `c:set`으로 저장합니다. 값 출력은 `c:out`으로 처리합니다. HTML 문맥에 맞게 이스케이프해야 하므로 form의 value와 URL의 href에도 같은 주의를 기울입니다. `c:url`은 URL 생성 도구이며 HTML 이스케이프를 대신하지 않습니다.

전체 건수는 원본 목록의 길이입니다. 표시 건수는 0으로 초기화한 뒤 필터에 맞는 행에서만 증가시킵니다. 이 수를 순번에도 사용하면 적금만 골라도 1·2가 됩니다. 원본 반복의 index를 그대로 쓰면 중간 항목이 제외될 때 순번이 건너뜁니다. 이 카운터는 화면 표시용이며 금융 금액 계산과 다릅니다.

`c:choose`는 종류·상태 코드의 표시명을 선택합니다. 가상 데이터의 종류·상태 범위가 정해져 있으므로 마지막 경우를 otherwise로 처리했습니다. 실제 서비스에서 알 수 없는 코드가 들어올 수 있다면 표시 정책과 서버 검증을 따로 정해야 합니다. 표시 상태는 실제 거래 인가를 대체하지 않습니다.

원 금액은 BigDecimal로 준비하고 `fmt:formatNumber`로 표시만 합니다. 합산이나 소수 금액의 반올림을 JSP에 맡기지 않습니다. 표시 건수에 따라 결과 문구를 분기하므로 원본 0건과 필터 0건을 모두 처리합니다. `c:url`/`c:param`은 컨텍스트 경로와 파라미터를 고려한 링크를 만듭니다.
