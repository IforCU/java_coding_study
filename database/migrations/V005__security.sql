BEGIN;
SELECT pg_advisory_xact_lock(7100000);
CREATE SCHEMA IF NOT EXISTS phase5;
CREATE TABLE IF NOT EXISTS phase5.profile (
    username varchar(40) PRIMARY KEY,
    display_name varchar(80) NOT NULL,
    email varchar(120) NOT NULL,
    account_hint varchar(40) NOT NULL,
    note varchar(100) NOT NULL DEFAULT ''
);
CREATE TABLE IF NOT EXISTS phase5.audit_event (
    id uuid PRIMARY KEY,
    actor varchar(80) NOT NULL,
    action varchar(80) NOT NULL,
    outcome varchar(20) NOT NULL,
    request_id varchar(64) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS phase5_audit_time_idx ON phase5.audit_event(occurred_at DESC, id);
-- 장애 재현용 데이터는 정상 프로필/감사 테이블과 별도 스키마에 격리한다.
CREATE SCHEMA IF NOT EXISTS phase5_lab;
CREATE TABLE IF NOT EXISTS phase5_lab.account (
    id uuid PRIMARY KEY,
    owner_id uuid NOT NULL,
    balance numeric(19, 0) NOT NULL CHECK (balance >= 0)
);
INSERT INTO bootstrap.schema_history(version, description)
VALUES (5, 'Phase 5 security, audit and isolated test labs')
ON CONFLICT(version) DO NOTHING;
COMMIT;
