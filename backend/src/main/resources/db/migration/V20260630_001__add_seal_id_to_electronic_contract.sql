-- ============================================================
-- 为 electronic_contract 表添加 seal_id 字段
-- 关联电子签章模块（seals 表）的印章ID
--
-- 背景：
--   ElectronicContractController.signElectronicContract 端点已集成 SealService，
--   签署电子合同时可选择关联印章，签署成功后记录印章使用日志。
--   seal_id 字段用于持久化签署该合同所使用的印章ID，便于审计追溯。
--
-- 兼容性：
--   - 使用 ADD COLUMN IF NOT EXISTS 保证幂等（H2 与 PostgreSQL 均支持）
--   - 字段允许为 NULL，兼容历史数据（旧合同无印章关联）
--
-- 对应实体: ElectronicContract.sealId
-- 对应 H2 初始化: PurchaseDatabaseInitializer.createElectronicContractTable
-- ============================================================

-- 添加 seal_id 字段
ALTER TABLE electronic_contract ADD COLUMN IF NOT EXISTS seal_id VARCHAR(32);

COMMENT ON COLUMN electronic_contract.seal_id IS '签署使用的印章ID（关联 seals.seal_id），可为空';

-- 印章ID索引（便于按印章反查合同）
CREATE INDEX IF NOT EXISTS idx_electronic_contract_seal_id ON electronic_contract (seal_id);
