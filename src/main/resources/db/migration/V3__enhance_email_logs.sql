ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP;

ALTER TABLE email_logs
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_email_logs_status_retry
    ON email_logs (status, next_retry_at);