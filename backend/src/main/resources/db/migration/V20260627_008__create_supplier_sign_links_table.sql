-- ============================================================
-- 供应商签署门户模块建表脚本
-- 创建供应商签署链接表 supplier_sign_links
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- ============================================================

-- ------------------------------------------------------------
-- 供应商签署链接表
-- 存储管理端生成的供应商合同签署链接及其状态流转
-- 状态流转: pending -> sent -> viewed -> signed/rejected -> expired
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supplier_sign_links (
    link_id             VARCHAR(32) NOT NULL,
    e_contract_id       VARCHAR(32) NOT NULL,
    e_contract_no       VARCHAR(50),
    contract_name       VARCHAR(200),
    supplier_id         VARCHAR(32),
    supplier_name       VARCHAR(100),
    contact_phone       VARCHAR(20),
    contact_email       VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'pending',
    token               VARCHAR(64) NOT NULL,
    expire_time         TIMESTAMP,
    send_time           TIMESTAMP,
    view_time           TIMESTAMP,
    sign_time           TIMESTAMP,
    reject_time         TIMESTAMP,
    reject_reason       VARCHAR(500),
    verification        TEXT,
    create_by           VARCHAR(50),
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT supplier_sign_links_pkey PRIMARY KEY (link_id)
);

-- 唯一索引：token 用于 H5 访问鉴权，必须唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_sign_links_token ON supplier_sign_links (token);

-- 普通索引：用于列表查询与状态筛选
CREATE INDEX IF NOT EXISTS idx_supplier_sign_links_status ON supplier_sign_links (status);
CREATE INDEX IF NOT EXISTS idx_supplier_sign_links_e_contract ON supplier_sign_links (e_contract_id);
CREATE INDEX IF NOT EXISTS idx_supplier_sign_links_supplier ON supplier_sign_links (supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_sign_links_expire ON supplier_sign_links (expire_time);
CREATE INDEX IF NOT EXISTS idx_supplier_sign_links_deleted ON supplier_sign_links (deleted);

COMMENT ON TABLE supplier_sign_links IS '供应商签署链接表';
COMMENT ON COLUMN supplier_sign_links.e_contract_id IS '关联电子合同ID';
COMMENT ON COLUMN supplier_sign_links.e_contract_no IS '电子合同编号(冗余)';
COMMENT ON COLUMN supplier_sign_links.contract_name IS '合同名称(冗余)';
COMMENT ON COLUMN supplier_sign_links.supplier_id IS '供应商ID(冗余)';
COMMENT ON COLUMN supplier_sign_links.supplier_name IS '供应商名称(冗余)';
COMMENT ON COLUMN supplier_sign_links.contact_phone IS '供应商联系人手机号';
COMMENT ON COLUMN supplier_sign_links.contact_email IS '供应商联系人邮箱';
COMMENT ON COLUMN supplier_sign_links.status IS '链接状态: pending-待发送 sent-已发送 viewed-已查看 signed-已签署 rejected-已拒绝 expired-已过期';
COMMENT ON COLUMN supplier_sign_links.token IS 'H5访问token(唯一, 用于供应商端鉴权)';
COMMENT ON COLUMN supplier_sign_links.expire_time IS '链接过期时间';
COMMENT ON COLUMN supplier_sign_links.send_time IS '链接发送时间';
COMMENT ON COLUMN supplier_sign_links.view_time IS '供应商首次查看时间';
COMMENT ON COLUMN supplier_sign_links.sign_time IS '供应商签署时间';
COMMENT ON COLUMN supplier_sign_links.reject_time IS '供应商拒绝时间';
COMMENT ON COLUMN supplier_sign_links.reject_reason IS '拒绝原因';
COMMENT ON COLUMN supplier_sign_links.verification IS '实名认证信息(JSON字符串)';
COMMENT ON COLUMN supplier_sign_links.create_by IS '创建人(冗余)';
COMMENT ON COLUMN supplier_sign_links.deleted IS '逻辑删除标记: 0-未删除 1-已删除';

-- 状态约束
ALTER TABLE supplier_sign_links ADD CONSTRAINT chk_sign_link_status
    CHECK (status IN ('pending', 'sent', 'viewed', 'signed', 'rejected', 'expired'));
