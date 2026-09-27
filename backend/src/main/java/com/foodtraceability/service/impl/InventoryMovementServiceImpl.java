package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.mapper.InventoryMovementMapper;
import com.foodtraceability.service.InventoryMovementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 统一库存流水服务实现（M3-M4 S5 流水统一）。
 *
 * 严格校验（宪法 §IV.4 DDL 红线 + §III.4 禁止默认兜底）：
 * locationId / materialId / changeQty / movementType / sourceType / sourceRef
 * 缺一即拒绝（BusinessException）；不填默认值、不做降级填充。
 * 追加型流水：不提供 update/delete API（§III.7）。
 */
@Service
public class InventoryMovementServiceImpl implements InventoryMovementService {

    private final InventoryMovementMapper inventoryMovementMapper;

    public InventoryMovementServiceImpl(InventoryMovementMapper inventoryMovementMapper) {
        this.inventoryMovementMapper = inventoryMovementMapper;
    }

    @Override
    public InventoryMovement recordMovement(InventoryMovement movement) {
        require(movement != null, "流水记录为空");
        require(movement.getLocationId() != null, "流水缺少 location_id");
        require(movement.getMaterialId() != null, "流水缺少 material_id");
        require(movement.getChangeQty() != null, "流水缺少 change_qty");
        require(notBlank(movement.getMovementType()), "流水缺少 movement_type");
        require(notBlank(movement.getSourceType()),
                "流水缺少 source_type，禁止写入无来源流水（宪法 §IV.4）");
        require(notBlank(movement.getSourceRef()),
                "流水缺少 source_ref，禁止写入无来源流水（宪法 §IV.4）");
        if (movement.getCreateTime() == null) {
            movement.setCreateTime(LocalDateTime.now());
        }
        inventoryMovementMapper.insert(movement);
        return movement;
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
