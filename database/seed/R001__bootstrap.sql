BEGIN;
SELECT pg_advisory_xact_lock(7100000);
INSERT INTO bootstrap.app_metadata (environment, schema_version)
VALUES ('local-learning', 1)
ON CONFLICT (environment) DO NOTHING;
COMMIT;
