-- Phase 0 정답 예시: 학습자가 요청할 때 선택해서 확인합니다.
-- 기본 애플리케이션 빌드, 마이그레이션, 실행에서는 사용하지 않습니다.

SELECT environment, schema_version
FROM bootstrap.app_metadata
WHERE environment = 'local-learning';
