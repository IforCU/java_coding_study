BEGIN;
SELECT pg_advisory_xact_lock(7100000);

-- 모두 가상 학습 데이터다. 기존 이름/상태/잔액을 덮어쓰지 않는다.
INSERT INTO phase3.customer (id, name, status)
VALUES ('30000000-0000-0000-0000-000000000001', '학습고객02', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

DO $seed$
DECLARE
    new_account_id uuid;
BEGIN
    INSERT INTO phase3.account (id, customer_id, balance)
    VALUES ('30000000-0000-0000-0000-000000000101',
            '30000000-0000-0000-0000-000000000001', 100000)
    ON CONFLICT (id) DO NOTHING
    RETURNING id INTO new_account_id;

    -- 계좌를 이번 실행에서 새로 만든 경우에만 최초 입금 이력을 넣는다.
    -- 기존 계좌에 실습 거래가 있으면 잔액도 이력도 그대로 보존한다.
    IF new_account_id IS NOT NULL THEN
        INSERT INTO phase3.transaction_history
            (id, account_id, type, amount, balance_after, occurred_at)
        VALUES ('30000000-0000-0000-0000-000000000201',
                new_account_id, 'DEPOSIT', 100000, 100000,
                TIMESTAMPTZ '2026-10-25 09:00:00+09')
        ON CONFLICT (id) DO NOTHING;

        -- 예약된 seed UUID가 다른 데이터에 쓰였으면 부분 seed를 남기지 않는다.
        IF NOT EXISTS (
            SELECT 1 FROM phase3.transaction_history
            WHERE id = '30000000-0000-0000-0000-000000000201'
              AND account_id = new_account_id
              AND type = 'DEPOSIT' AND amount = 100000 AND balance_after = 100000
        ) THEN
            RAISE EXCEPTION 'Phase 3 seed identifier conflict';
        END IF;
    END IF;
END;
$seed$;
COMMIT;
