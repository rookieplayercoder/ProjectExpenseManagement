-- =========================
-- FIX: user_balance_history.balance_id FK blocks deleting a fully-settled balance
-- =========================
-- BalanceRepository.applyDebt() deletes a user_balance row once its net_amount
-- reaches exactly zero (a debt is fully settled). It can't just update the row
-- to net_amount = 0 instead - chk_user_balance_amount_positive requires
-- net_amount > 0, so a zero-balance row can never legally exist. Deletion is
-- the only option once a debt is fully paid off.
--
-- fk_balance_history_balance was declared with the default ON DELETE NO ACTION,
-- so any earlier history row that still references that balance_id blocks the
-- delete outright:
--   ERROR: update or delete on table "user_balance" violates foreign key
--   constraint "fk_balance_history_balance"
--
-- Fix: ON DELETE SET NULL. balance_id is already nullable, and every other
-- audit column on a history row (group_id, debtor/creditor, currency,
-- previous/delta/new amounts) is captured independently of the live balance
-- row, so the audit trail is unaffected - balance_id on old rows simply
-- becomes NULL once the balance they pointed at is deleted, rather than
-- blocking the deletion.
ALTER TABLE user_balance_history
    DROP CONSTRAINT fk_balance_history_balance;

ALTER TABLE user_balance_history
    ADD CONSTRAINT fk_balance_history_balance
        FOREIGN KEY (balance_id) REFERENCES user_balance(id)
        ON DELETE SET NULL;
