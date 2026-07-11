-- =========================
-- AUTHENTICATION
-- =========================
ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_role CHECK (role IN ('USER', 'ADMIN'));

-- index to speed up login lookups by email (case is already normalized to lower-case on write)
CREATE INDEX idx_app_user_email ON app_user(email);
