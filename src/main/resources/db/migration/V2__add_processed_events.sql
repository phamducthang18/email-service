-- ============================================================
-- V2: Thêm bảng idempotency để đảm bảo mỗi event chỉ xử lý một lần
-- ============================================================

CREATE TABLE IF NOT EXISTS processed_events
(
    event_id     VARCHAR(100) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP    NOT NULL DEFAULT now()
);
