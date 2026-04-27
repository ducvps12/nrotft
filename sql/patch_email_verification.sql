-- Email OTP verification patch
-- Chạy 1 lần trên database NRO trước khi bật tính năng xác minh email.

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS email_verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_verify_code VARCHAR(10) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS email_verify_expire BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_verify_last_sent BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_account_email_verified ON account(email_verified);
CREATE INDEX IF NOT EXISTS idx_account_email_verify_code ON account(email_verify_code);
