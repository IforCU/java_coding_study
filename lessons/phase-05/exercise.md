# Phase 5 실습 — 감사 표시 마스킹과 장애 분석

아래 Maven 명령은 프로젝트 루트에서 실행합니다.

## 마스킹 TODO

수정 파일은 [`AuditMasker.java`](../../exercises/starter/phase-05/src/main/java/kr/banklearning/exercise/AuditMasker.java)입니다. 정상 보안 WAR와 분리된 실습 모듈입니다.

`maskAccount(String account)`는 다음 계약을 지킵니다.

- 허용 구분자는 ASCII 공백 `U+0020`과 하이픈 `U+002D`입니다. 제거한 뒤 검증합니다.
- 정규화한 값은 ASCII 숫자 8~20자리여야 합니다.
- null, 빈 값, 잘못된 길이, 그 밖의 문자는 `IllegalArgumentException`으로 거절합니다.
- 마지막 네 자리만 표시하고 앞의 모든 자리는 `*`로 바꿉니다.
- 반환 길이는 정규화한 숫자의 길이와 같습니다.
- 앞자리 0을 보존하고 입력을 변경하지 않습니다.
- 잘못된 입력 원문을 예외 메시지에 포함하지 않습니다.

| 가상 입력 | 기대 표시 |
| --- | --- |
| `1234-5678` | `****5678` |
| `00000001` | `****0001` |
| `1234 5678 9012` | `********9012` |

Unicode 숫자, 탭, 줄바꿈, 다른 종류의 하이픈과 보이지 않는 공백은 허용 구분자가 아닙니다. 임의 문자를 모두 지워 유효한 값처럼 바꾸지 않습니다.

```sh
mvn -Dmaven.repo.local=.cache/maven-repository -Pexercise-phase5 -pl exercises/starter/phase-05 -am verify
```

TODO 상태에서는 실패해야 합니다. 구현 후 길이 경계, 구분자, ASCII 범위, 원문 비노출, 반복 호출 검증을 통과시킵니다. 테스트를 바꾸어 기대값을 낮추지 않습니다.

## 장애 분석 과제

[`incidents.md`](../../exercises/starter/phase-05/incidents.md)에 여덟 사건의 증상, 재현, 분석 파일, 힌트, 과제와 검증 항목이 있습니다. 정상 WAR와 분리된 테스트 fixture에서만 관찰합니다.

각 사건에 대해 다음을 기록합니다.

1. 관찰한 상태와 기대한 상태
2. 요청 ID·사용자·DB 작업의 경계
3. 원인 후보와 확인할 증거
4. 변경할 책임과 보존할 공개 동작
5. 새로 추가할 정상·실패 회귀 검증

원본 랩의 증상 관찰 PASS와 본인이 제안한 수정안의 검증 결과를 나누어 기록합니다. 실제 DB에서 실행하지 못한 재현은 `NOT RUN`입니다. 완성 마스킹과 장애 해설은 solutions에만 보관합니다.
