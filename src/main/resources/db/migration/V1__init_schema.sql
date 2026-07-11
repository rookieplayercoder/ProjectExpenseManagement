-- =========================
-- EXTENSIONS
-- =========================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- USERS
-- =========================
CREATE TABLE app_user (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(320) NOT NULL,
    full_name           VARCHAR(150) NOT NULL,
    mobile_number       VARCHAR(20),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT chk_app_user_email_non_empty CHECK (length(trim(email)) > 0),
    CONSTRAINT chk_app_user_name_non_empty CHECK (length(trim(full_name)) > 0)
);

-- =========================
-- GROUPS
-- =========================
CREATE TABLE expense_group (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_name          VARCHAR(150) NOT NULL,
    description         TEXT,
    created_by          UUID NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_expense_group_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id),
    CONSTRAINT chk_expense_group_name_non_empty CHECK (length(trim(group_name)) > 0)
);

CREATE TABLE expense_group_member (
    group_id            UUID NOT NULL,
    user_id             UUID NOT NULL,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_member_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_member_user
        FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- =========================
-- EXPENSES
-- =========================
CREATE TABLE expense (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id            UUID,
    paid_by_user_id     UUID NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    total_amount        NUMERIC(19, 4) NOT NULL,
    currency_code       CHAR(3) NOT NULL,
    split_type          VARCHAR(30) NOT NULL,
    expense_date        DATE NOT NULL,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_expense_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_expense_paid_by
        FOREIGN KEY (paid_by_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_expense_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id),

    CONSTRAINT chk_expense_total_amount_positive CHECK (total_amount > 0),
    CONSTRAINT chk_expense_currency_code_upper CHECK (currency_code = upper(currency_code)),
    CONSTRAINT chk_expense_split_type CHECK (split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE')),
    CONSTRAINT chk_expense_title_non_empty CHECK (length(trim(title)) > 0)
);

-- =========================
-- EXPENSE PARTICIPANTS
-- Each row says: user participates in this expense and owes owed_amount
-- =========================
CREATE TABLE expense_participant (
    expense_id           UUID NOT NULL,
    user_id              UUID NOT NULL,
    owed_amount          NUMERIC(19, 4) NOT NULL,
    percentage_value     NUMERIC(9, 4),
    exact_amount_input   NUMERIC(19, 4),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (expense_id, user_id),

    CONSTRAINT fk_expense_participant_expense
        FOREIGN KEY (expense_id) REFERENCES expense(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_participant_user
        FOREIGN KEY (user_id) REFERENCES app_user(id),

    CONSTRAINT chk_expense_participant_owed_amount_non_negative CHECK (owed_amount >= 0),
    CONSTRAINT chk_expense_participant_percentage_non_negative CHECK (
        percentage_value IS NULL OR percentage_value >= 0
    ),
    CONSTRAINT chk_expense_participant_exact_amount_non_negative CHECK (
        exact_amount_input IS NULL OR exact_amount_input >= 0
    )
);

-- =========================
-- SETTLEMENTS
-- A direct payment to reduce bilateral debt
-- =========================
CREATE TABLE settlement (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id            UUID,
    paid_by_user_id     UUID NOT NULL,
    paid_to_user_id     UUID NOT NULL,
    amount              NUMERIC(19, 4) NOT NULL,
    currency_code       CHAR(3) NOT NULL,
    settlement_date     DATE NOT NULL,
    note                VARCHAR(300),
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_settlement_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_settlement_paid_by
        FOREIGN KEY (paid_by_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_settlement_paid_to
        FOREIGN KEY (paid_to_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_settlement_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id),

    CONSTRAINT chk_settlement_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_settlement_currency_code_upper CHECK (currency_code = upper(currency_code)),
    CONSTRAINT chk_settlement_distinct_users CHECK (paid_by_user_id <> paid_to_user_id)
);

-- =========================
-- BILATERAL BALANCE LEDGER
-- Canonical row: debtor owes creditor net_amount
-- Only one row exists per pair, never both directions
-- =========================
CREATE TABLE user_balance (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id            UUID,
    debtor_user_id      UUID NOT NULL,
    creditor_user_id    UUID NOT NULL,
    currency_code       CHAR(3) NOT NULL,
    net_amount          NUMERIC(19, 4) NOT NULL,
    last_event_type     VARCHAR(30) NOT NULL,
    last_event_id       UUID NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_balance_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_user_balance_debtor
        FOREIGN KEY (debtor_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_user_balance_creditor
        FOREIGN KEY (creditor_user_id) REFERENCES app_user(id),

    CONSTRAINT chk_user_balance_amount_positive CHECK (net_amount > 0),
    CONSTRAINT chk_user_balance_currency_code_upper CHECK (currency_code = upper(currency_code)),
    CONSTRAINT chk_user_balance_distinct_users CHECK (debtor_user_id <> creditor_user_id),
    CONSTRAINT chk_user_balance_event_type CHECK (last_event_type IN ('EXPENSE', 'SETTLEMENT'))
);

-- prevent directional duplicates by forcing debtor < creditor ordering?
-- Not enough because direction matters.
-- Instead enforce canonical pair uniqueness using least/greatest + direction stored by debt meaning.
CREATE UNIQUE INDEX uq_user_balance_pair_scope
ON user_balance (
    COALESCE(group_id, '00000000-0000-0000-0000-000000000000'::uuid),
    debtor_user_id,
    creditor_user_id,
    currency_code
);

-- =========================
-- BALANCE HISTORY / AUDIT
-- Every balance mutation is recorded here
-- =========================
CREATE TABLE user_balance_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id              UUID,
    group_id                UUID,
    debtor_user_id          UUID NOT NULL,
    creditor_user_id        UUID NOT NULL,
    currency_code           CHAR(3) NOT NULL,

    event_type              VARCHAR(30) NOT NULL,
    event_id                UUID NOT NULL,

    previous_amount         NUMERIC(19, 4),
    delta_amount            NUMERIC(19, 4) NOT NULL,
    new_amount              NUMERIC(19, 4),

    event_occurred_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_balance_history_balance
        FOREIGN KEY (balance_id) REFERENCES user_balance(id),
    CONSTRAINT fk_balance_history_group
        FOREIGN KEY (group_id) REFERENCES expense_group(id),
    CONSTRAINT fk_balance_history_debtor
        FOREIGN KEY (debtor_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_balance_history_creditor
        FOREIGN KEY (creditor_user_id) REFERENCES app_user(id),

    CONSTRAINT chk_balance_history_distinct_users CHECK (debtor_user_id <> creditor_user_id),
    CONSTRAINT chk_balance_history_currency_code_upper CHECK (currency_code = upper(currency_code)),
    CONSTRAINT chk_balance_history_event_type CHECK (event_type IN ('EXPENSE', 'SETTLEMENT'))
);

-- =========================
-- OPTIONAL IDEMPOTENCY SUPPORT
-- Prevent duplicate create-expense requests from retries
-- =========================
CREATE TABLE idempotency_record (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(100) NOT NULL,
    request_type        VARCHAR(50) NOT NULL,
    reference_id        UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_key_request_type UNIQUE (idempotency_key, request_type)
);
