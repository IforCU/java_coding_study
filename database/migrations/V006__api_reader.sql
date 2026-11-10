BEGIN;
SELECT pg_advisory_xact_lock(7100000);

DO $migration$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM bootstrap.schema_history WHERE version = 4) THEN
        RAISE EXCEPTION 'Apply V004 before V006';
    END IF;
END;
$migration$;

-- psql 변수 api_password는 migrator가 API_DB_PASSWORD에서 전달한다.
-- SQL 리터럴 quoting은 :'api_password'와 format의 %L이 담당한다.
-- 기존 역할의 비밀번호·속성·사용자 실험 설정은 재실행으로 변경하지 않는다.
SELECT NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bank_learning_api') AS api_role_is_new
\gset

SELECT format(
    'CREATE ROLE bank_learning_api LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD %L',
    :'api_password'
)
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bank_learning_api')
\gexec

\if :api_role_is_new
ALTER ROLE bank_learning_api SET default_transaction_read_only = on;
\endif

DO $grant$
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO bank_learning_api', current_database());
END;
$grant$;

GRANT USAGE ON SCHEMA phase4 TO bank_learning_api;
GRANT SELECT ON TABLE phase4.customer, phase4.account, phase4.transfer_request,
    phase4.ledger, phase4.daily_usage TO bank_learning_api;

INSERT INTO bootstrap.schema_history (version, description)
VALUES (6, 'Phase 6 read-only API access to the Phase 4 owned banking schema')
ON CONFLICT (version) DO NOTHING;
COMMIT;
