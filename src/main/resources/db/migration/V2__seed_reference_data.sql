-- user lookups
CREATE INDEX idx_app_user_active ON app_user(is_active);

-- group membership traversal
CREATE INDEX idx_group_member_user ON expense_group_member(user_id, is_active);
CREATE INDEX idx_group_member_group_active ON expense_group_member(group_id, is_active);

-- expense lookup patterns
CREATE INDEX idx_expense_group_date ON expense(group_id, expense_date DESC, created_at DESC);
CREATE INDEX idx_expense_paid_by_date ON expense(paid_by_user_id, expense_date DESC, created_at DESC);
CREATE INDEX idx_expense_created_at ON expense(created_at DESC);

-- participant lookup
CREATE INDEX idx_expense_participant_user ON expense_participant(user_id, created_at DESC);
CREATE INDEX idx_expense_participant_expense ON expense_participant(expense_id);

-- settlement lookup
CREATE INDEX idx_settlement_paid_by_date ON settlement(paid_by_user_id, settlement_date DESC, created_at DESC);
CREATE INDEX idx_settlement_paid_to_date ON settlement(paid_to_user_id, settlement_date DESC, created_at DESC);
CREATE INDEX idx_settlement_group_date ON settlement(group_id, settlement_date DESC, created_at DESC);

-- fast balance lookups
CREATE INDEX idx_user_balance_debtor ON user_balance(debtor_user_id, currency_code, updated_at DESC);
CREATE INDEX idx_user_balance_creditor ON user_balance(creditor_user_id, currency_code, updated_at DESC);
CREATE INDEX idx_user_balance_group ON user_balance(group_id, currency_code, updated_at DESC);

-- history lookup for balance timelines
CREATE INDEX idx_balance_history_debtor_creditor_time
ON user_balance_history(debtor_user_id, creditor_user_id, event_occurred_at DESC);

CREATE INDEX idx_balance_history_group_time
ON user_balance_history(group_id, event_occurred_at DESC);

CREATE INDEX idx_balance_history_event
ON user_balance_history(event_type, event_id);

CREATE INDEX idx_balance_history_user_time
ON user_balance_history(debtor_user_id, event_occurred_at DESC);

CREATE INDEX idx_balance_history_creditor_time
ON user_balance_history(creditor_user_id, event_occurred_at DESC);
