-- PERF-001: SavingsGoal create endpoint'inde network-retry idempotency desteği.
-- Kolon nullable — mevcut mobil app sürümleri key göndermeden çalışmaya devam eder.
-- Partial UNIQUE index (WHERE idempotency_key IS NOT NULL): NULL değerler birbirleriyle
-- çakışmaz (Postgres'in varsayılan UNIQUE davranışı da bu, ama partial index niyeti daha açık
-- ifade eder ve index boyutunu key göndermeyen eski kayıtları dışlayarak küçük tutar).

ALTER TABLE savings_goals ADD COLUMN idempotency_key varchar(255);

CREATE UNIQUE INDEX uk_savings_goals_user_idempotency_key
    ON savings_goals (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
