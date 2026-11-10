BEGIN;
-- Phase 0과 같은 잠금을 사용하여 migration/seed 동시 실행을 직렬화한다.
SELECT pg_advisory_xact_lock(7100000);

DO $migration$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM bootstrap.schema_history WHERE version = 1) THEN
        RAISE EXCEPTION 'Apply V001 before V002';
    END IF;
END;
$migration$;

CREATE SCHEMA IF NOT EXISTS phase2;

CREATE TABLE IF NOT EXISTS phase2.customer (
    id uuid PRIMARY KEY,
    name varchar(80) NOT NULL CHECK (char_length(btrim(name)) BETWEEN 1 AND 80),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS phase2.account (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES phase2.customer (id),
    balance numeric(19, 0) NOT NULL DEFAULT 0 CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS phase2.transaction_history (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES phase2.account (id),
    type varchar(20) NOT NULL CHECK (char_length(btrim(type)) > 0),
    amount numeric(19, 0) NOT NULL CHECK (amount > 0),
    balance_after numeric(19, 0) NOT NULL CHECK (balance_after >= 0),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS phase2_account_customer_idx
    ON phase2.account (customer_id, id);
CREATE INDEX IF NOT EXISTS phase2_history_account_occurred_idx
    ON phase2.transaction_history (account_id, occurred_at DESC, id DESC);

INSERT INTO bootstrap.schema_history (version, description)
VALUES (2, 'Phase 2 Servlet JSP JDBC schema')
ON CONFLICT (version) DO NOTHING;
COMMIT;
