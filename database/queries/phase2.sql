-- Phase 2 수동 관찰용 읽기 전용 SQL. 실제 실행 결과는 PROGRESS.md에 기록한다.
-- 이후 실습에서 행이 추가되므로 전체 행 수를 최초 seed 개수로 고정하지 않는다.
BEGIN READ ONLY;

SELECT version, description
FROM bootstrap.schema_history
WHERE version IN (1, 2)
ORDER BY version;

-- 앱의 readiness가 참조할 수 있는 전용 스키마 조회다.
SELECT count(*) AS customer_count FROM phase2.customer;

SELECT c.id AS customer_id, c.name, c.status,
       a.id AS account_id, a.balance
FROM phase2.customer c
LEFT JOIN phase2.account a ON a.customer_id = c.id
ORDER BY c.id, a.id;

SELECT id, account_id, type, amount, balance_after, occurred_at
FROM phase2.transaction_history
WHERE account_id = '20000000-0000-0000-0000-000000000101'
ORDER BY occurred_at, id;

-- FK/NOT NULL/CHECK 제약 확인. CHECK 제약은 행 사이의 잔액/이력 일치를 보장하지 않는다.
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = 'phase2'::regnamespace
ORDER BY conrelid::regclass::text, conname;

COMMIT;
