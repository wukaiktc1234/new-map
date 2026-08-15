-- ============================================================
-- 收货确认增强：明细问题类型 + 双方签名存证
-- ============================================================

ALTER TABLE receipt_confirmation_items ADD COLUMN IF NOT EXISTS issue_type VARCHAR(20);
COMMENT ON COLUMN receipt_confirmation_items.issue_type IS '问题类型：damaged破损/short短少/expiry效期不符/quality品质异常/other其他';

-- 收货确认签名存证表
CREATE TABLE IF NOT EXISTS receipt_confirmation_signatures (
    signature_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    confirmation_id BIGINT       NOT NULL,
    signer_type     VARCHAR(20)  NOT NULL,
    signer_name     VARCHAR(50),
    signature_image TEXT         NOT NULL,
    sign_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  receipt_confirmation_signatures IS '收货确认单双方签名存证';
COMMENT ON COLUMN receipt_confirmation_signatures.signer_type IS '签名方：RECEIVER收货方 / SUPPLIER供应商（司机）';
COMMENT ON COLUMN receipt_confirmation_signatures.signature_image IS '签名图片（base64 PNG）';
COMMENT ON COLUMN receipt_confirmation_signatures.sign_time IS '签名时间';

CREATE INDEX IF NOT EXISTS idx_receipt_signatures_confirmation_id ON receipt_confirmation_signatures (confirmation_id);
