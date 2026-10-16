-- ============================================================
-- V1: Initial schema for email-service
-- ============================================================

CREATE TABLE IF NOT EXISTS email_logs
(
    id          BIGSERIAL PRIMARY KEY,
    recipient   VARCHAR(255) NOT NULL,
    subject     VARCHAR(500) NOT NULL,
    body        TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'SENT',  -- SENT | FAILED
    event_type  VARCHAR(100),                           -- e.g. USER_REGISTERED
    event_id    VARCHAR(100),                           -- idempotency key từ event gốc
    error_msg   TEXT,                                   -- lý do thất bại nếu có

    -- audit fields (BaseEntity)
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

-- index để query lịch sử mail theo recipient
CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs (recipient);

-- index để tránh gửi mail trùng cùng event_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_email_logs_event_id ON email_logs (event_id)
    WHERE event_id IS NOT NULL;
