BEGIN;
-- 마이그레이션/seed 동시 실행을 직렬화한다. 데이터 삭제는 수행하지 않는다.
SELECT pg_advisory_xact_lock(7100000);
CREATE SCHEMA IF NOT EXISTS bootstrap;

CREATE TABLE IF NOT EXISTS bootstrap.schema_history (
    version integer PRIMARY KEY,
    description text NOT NULL,
    installed_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bootstrap.app_metadata (
    environment varchar(40) PRIMARY KEY,
    schema_version integer NOT NULL CHECK (schema_version > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bootstrap.verification_marker (
    marker_id text PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO bootstrap.schema_history (version, description)
VALUES (1, 'Phase 0 bootstrap schema')
ON CONFLICT (version) DO NOTHING;
COMMIT;
