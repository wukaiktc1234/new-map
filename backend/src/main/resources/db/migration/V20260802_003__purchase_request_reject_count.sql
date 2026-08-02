-- 采购申请驳回次数限制（A1）
-- reject_count: 被驳回累计次数，达到 3 次后禁止重新提交，需管理员重置
ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS reject_count INT NOT NULL DEFAULT 0;
