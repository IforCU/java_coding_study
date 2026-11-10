BEGIN;
SELECT pg_advisory_xact_lock(7100000);
INSERT INTO phase5.profile(username, display_name, email, account_hint, note)
VALUES ('learner', '학습사용자05', 'learner@example.test', '1234-5678-9001', ''),
       ('admin', '학습관리자05', 'admin@example.test', '1234-5678-9002', '')
ON CONFLICT(username) DO NOTHING;
COMMIT;
