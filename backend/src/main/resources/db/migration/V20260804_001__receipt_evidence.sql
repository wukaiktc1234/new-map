-- ============================================================
-- 收货留证：照片证据表（人工关联 / 拒收/质检不合格 现场拍照）
-- 照片通过 /v1/upload/image 上传，表内仅存 URL 与元数据
-- ============================================================

CREATE TABLE IF NOT EXISTS receipt_evidence (
    evidence_id BIGSERIAL PRIMARY KEY,
    confirmation_id BIGINT NOT NULL,
    confirmation_item_id BIGINT,
    arrival_item_id BIGINT,
    evidence_type VARCHAR(20) NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    photo_taken_at VARCHAR(32),
    device_model VARCHAR(100),
    gps VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

COMMENT ON TABLE receipt_evidence IS '收货留证照片表（现场拍摄证据链）';
COMMENT ON COLUMN receipt_evidence.evidence_type IS '证据类型：MANUAL_MATCH-人工关联 / QC_REJECT-拒收或不合格';
COMMENT ON COLUMN receipt_evidence.photo_url IS '照片访问地址';
COMMENT ON COLUMN receipt_evidence.photo_taken_at IS '照片拍摄时间（Exif，可缺失）';
COMMENT ON COLUMN receipt_evidence.device_model IS '拍摄设备型号（Exif）';
COMMENT ON COLUMN receipt_evidence.gps IS '定位信息（浏览器授权或Exif GPS，可缺失）';

CREATE INDEX IF NOT EXISTS idx_receipt_evidence_confirmation ON receipt_evidence (confirmation_id, deleted);
CREATE INDEX IF NOT EXISTS idx_receipt_evidence_item ON receipt_evidence (confirmation_item_id, deleted);
