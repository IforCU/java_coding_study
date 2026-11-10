BEGIN;
SELECT pg_advisory_xact_lock(7100000);

DO $migration$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM bootstrap.schema_history WHERE version = 1) THEN
        RAISE EXCEPTION 'Apply V001 before V004';
    END IF;
END;
$migration$;

CREATE SCHEMA IF NOT EXISTS phase4;

CREATE TABLE IF NOT EXISTS phase4.customer (
    id uuid PRIMARY KEY,
    name varchar(80) NOT NULL CHECK (char_length(btrim(name)) BETWEEN 1 AND 80),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS phase4.account (
    id uuid PRIMARY KEY,
    customer_id uuid NOT NULL REFERENCES phase4.customer (id),
    balance numeric(19, 0) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    daily_limit numeric(19, 0) NOT NULL DEFAULT 1000000 CHECK (daily_limit > 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE IF NOT EXISTS phase4.transfer_request (
    request_id uuid PRIMARY KEY,
    source_id uuid NOT NULL REFERENCES phase4.account (id),
    destination_id uuid NOT NULL REFERENCES phase4.account (id),
    amount numeric(19, 0) NOT NULL CHECK (amount > 0),
    status varchar(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMPLETED', 'CANCEL_REQUESTED')),
    business_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (source_id <> destination_id)
);

CREATE TABLE IF NOT EXISTS phase4.ledger (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL REFERENCES phase4.transfer_request (request_id),
    account_id uuid NOT NULL REFERENCES phase4.account (id),
    type varchar(20) NOT NULL CHECK (type IN ('WITHDRAWAL', 'DEPOSIT')),
    amount numeric(19, 0) NOT NULL CHECK (amount > 0),
    balance_after numeric(19, 0) NOT NULL CHECK (balance_after >= 0),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (request_id, account_id)
);

CREATE TABLE IF NOT EXISTS phase4.daily_usage (
    account_id uuid NOT NULL REFERENCES phase4.account (id),
    business_date date NOT NULL,
    used numeric(19, 0) NOT NULL DEFAULT 0 CHECK (used >= 0),
    PRIMARY KEY (account_id, business_date)
);

CREATE INDEX IF NOT EXISTS phase4_account_customer_idx ON phase4.account (customer_id, id);
CREATE INDEX IF NOT EXISTS phase4_request_source_day_idx
    ON phase4.transfer_request (source_id, business_date, created_at, request_id);
CREATE INDEX IF NOT EXISTS phase4_ledger_account_time_idx
    ON phase4.ledger (account_id, occurred_at, id);

INSERT INTO bootstrap.schema_history (version, description)
VALUES (4, 'Phase 4 local transfer transaction and ledger schema')
ON CONFLICT (version) DO NOTHING;
COMMIT;
