package com.foodtraceability.service;

import com.foodtraceability.entity.InventoryMovement;

/**
 * 统一库存流水服务（P1-LOCATION-MODEL-001 M3-M4 S5 流水统一）。
 * inventory_movement 表（T3+T4a+T4b 合并，-001 §1.4）的唯一写入口。
 *
 * 红线（implementation-constitution-001）：
 * - source_type + source_ref NOT NULL（§IV.4）：缺来源的记录以异常拒绝——
 *   无来源流水严格禁止（R-04 教训），本服务不做任何默认兜底填充（§III.4 禁止默认兜底）；
 * - 追加型流水：仅提供 recordMovement，无 update/delete API（§III.7 流水禁 update/delete）。
 *
 * 写入失败是否回滚由调用方决定（§6-3 现状迁移：现有业务调用方降级——仅记日志、
 * 不回滚库存账变更）。
 */
public interface InventoryMovementService {

    /**
     * 写入一条统一流水（严格校验 + insert）。
     *
     * @param movement 流水实体：locationId / materialId / changeQty / movementType /
     *                 sourceType / sourceRef 必填（缺一拒绝）；createTime 为空时补当前时间
     * @return 主键回填后的流水实体
     * @throws com.foodtraceability.common.exception.BusinessException 必填字段缺失
     */
    InventoryMovement recordMovement(InventoryMovement movement);
}
