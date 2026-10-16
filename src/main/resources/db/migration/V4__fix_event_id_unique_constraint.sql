-- ============================================================
-- V4: Thay partial unique index bằng unique constraint trên event_id
--     để hỗ trợ ON CONFLICT (event_id) DO NOTHING trong claimEvent query
-- ============================================================

-- Xóa partial unique index cũ
DROP INDEX IF EXISTS idx_email_logs_event_id;

-- Thêm unique constraint thật sự (không partial)
ALTER TABLE email_logs
    ADD CONSTRAINT uq_email_logs_event_id UNIQUE (event_id);
