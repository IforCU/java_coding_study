BEGIN;
SELECT pg_advisory_xact_lock(7100000);

INSERT INTO phase4.customer (id, name, status)
VALUES ('40000000-0000-0000-0000-000000000001', '학습이체고객04', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

-- 이 금액은 가상 실습의 opening balance다. 이체 완료나 원장 기록을 가장하지 않는다.
-- 기존 실습 잔액·한도·version은 seed 재실행으로 덮어쓰지 않는다.
INSERT INTO phase4.account (id, customer_id, balance, daily_limit, version)
VALUES ('40000000-0000-0000-0000-000000000101',
        '40000000-0000-0000-0000-000000000001', 1000000, 1000000, 0),
       ('40000000-0000-0000-0000-000000000102',
        '40000000-0000-0000-0000-000000000001', 100000, 1000000, 0)
ON CONFLICT (id) DO NOTHING;

-- transfer_request/ledger/daily_usage는 실제 실습 이체가 만들며 seed하지 않는다.
COMMIT;
