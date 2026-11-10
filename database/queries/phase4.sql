-- 관찰용 읽기 전용 SQL. 실행 여부/결과는 PROGRESS.md에서 별도로 기록한다.
BEGIN READ ONLY;

SELECT version, description FROM bootstrap.schema_history WHERE version = 4;

SELECT a.id, c.name, c.status, a.balance, a.daily_limit, a.version
FROM phase4.account a JOIN phase4.customer c ON c.id = a.customer_id
ORDER BY a.id;

SELECT request_id, source_id, destination_id, amount, status, business_date, created_at
FROM phase4.transfer_request
ORDER BY created_at, request_id;

SELECT account_id, business_date, used
FROM phase4.daily_usage
ORDER BY business_date, account_id;

SELECT request_id, account_id, type, amount, balance_after, occurred_at
FROM phase4.ledger
ORDER BY occurred_at, request_id, account_id;

-- 완료/취소 요청 거래에 이체 원장 두 건이 연결되는지 관찰한다.
-- 취소 요청은 이미 완료된 자금 이동을 자동으로 되돌리는 상태가 아니다.
SELECT r.request_id, r.status, r.amount, count(l.id) AS ledger_count,
       count(*) FILTER (WHERE l.type = 'WITHDRAWAL' AND l.account_id = r.source_id
                         AND l.amount = r.amount) AS source_entries,
       count(*) FILTER (WHERE l.type = 'DEPOSIT' AND l.account_id = r.destination_id
                         AND l.amount = r.amount) AS destination_entries
FROM phase4.transfer_request r
LEFT JOIN phase4.ledger l ON l.request_id = r.request_id
GROUP BY r.request_id, r.status, r.amount
ORDER BY r.request_id;

-- daily_usage는 출금 기준이다. 한도 내 사용인지 관찰하고 원장과 함께 대조한다.
SELECT u.account_id, u.business_date, u.used, a.daily_limit,
       u.used <= a.daily_limit AS within_limit
FROM phase4.daily_usage u JOIN phase4.account a ON a.id = u.account_id
ORDER BY u.account_id, u.business_date;

COMMIT;
