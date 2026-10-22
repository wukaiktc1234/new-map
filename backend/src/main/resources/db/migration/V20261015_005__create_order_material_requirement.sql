-- ============================================================
-- 现场修复（2026-10-23）：补齐缺失的 order_material_requirement 表
--
-- 背景：
--   实体 com.foodtraceability.entity.OrderMaterialRequirement 映射
--   @TableName("order_material_requirement")，OrderMaterialRequirementMapper 的多条
--   注解 SQL 以及 KitchensScanController 的料包核对接口都依赖它；
--   但**仓库内所有 Flyway 迁移从未创建过该表**（grep 全库 db/migration = 0 命中）。
--
-- 实测影响（E2E 链路验证）：
--   POST /api/v1/kitchen/orders/{id}/complete-make（完成制作/出餐扣料）
--   → PSQLException: 关系 "order_material_requirement" 不存在
--   → 同一事务后续 UPDATE 继续失败 → 前端收到 500「系统繁忙」
--   即「点单 -> 后厨 -> 出餐」在扣料这一步必然中断。
--
-- 列定义严格按实体字段生成（含 @TableLogic deleted）：
--   id/requirement_id/order_id/kitchen_order_id/order_number/dish_id/dish_name/
--   dish_quantity/is_combo/combo_id/material_id/material_name/required_quantity/
--   unit/locked_quantity/used_quantity/status/create_time/update_time/deleted
--
-- 幂等：CREATE TABLE/INDEX IF NOT EXISTS，可重复执行。
-- 回退：DROP TABLE order_material_requirement;
-- ============================================================

CREATE TABLE IF NOT EXISTS order_material_requirement (
    id                 BIGSERIAL PRIMARY KEY,
    requirement_id     VARCHAR(64),
    order_id           VARCHAR(64),
    kitchen_order_id   VARCHAR(64),
    order_number       VARCHAR(64),
    dish_id            VARCHAR(64),
    dish_name          VARCHAR(255),
    dish_quantity      INTEGER,
    is_combo           INTEGER        DEFAULT 0,
    combo_id           VARCHAR(64),
    material_id        VARCHAR(64),
    material_name      VARCHAR(255),
    required_quantity  NUMERIC(18, 4) DEFAULT 0,
    unit               VARCHAR(32),
    locked_quantity    NUMERIC(18, 4) DEFAULT 0,
    used_quantity      NUMERIC(18, 4) DEFAULT 0,
    status             VARCHAR(20)    DEFAULT 'pending',
    create_time        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        DEFAULT 0
);

COMMENT ON TABLE order_material_requirement IS '订单原料需求实体（扣料/出餐链路数据源）';

CREATE INDEX IF NOT EXISTS idx_omr_order_id         ON order_material_requirement (order_id);
CREATE INDEX IF NOT EXISTS idx_omr_kitchen_order_id ON order_material_requirement (kitchen_order_id);
CREATE INDEX IF NOT EXISTS idx_omr_material_name    ON order_material_requirement (material_name);
CREATE INDEX IF NOT EXISTS idx_omr_status           ON order_material_requirement (status);
