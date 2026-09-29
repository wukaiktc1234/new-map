package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.StoreInventoryLogMapper;
import com.foodtraceability.service.InventoryMovementService;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 门店库存日志服务。
 *
 * M3-M4 S9a：写路径收编——原 BaseMapper insert 目标表 store_inventory_log 已更名
 * store_inventory_log_legacy；createLog 改为经统一流水 InventoryMovementService.recordMovement
 * 写入 inventory_movement（读路径 selectLogPage 亦已改读 inventory_movement，读写一致）。
 * 严格校验：store_id 解析失败 / product_id 缺失即拒绝（宪法 §III.4 禁止默认兜底）。
 */
@Service
public class StoreInventoryLogServiceImpl extends ServiceImpl<StoreInventoryLogMapper, StoreInventoryLog> implements StoreInventoryLogService {

    public StoreInventoryLogServiceImpl(StoreInventoryLogMapper storeInventoryLogMapper,
                                        LocationService locationService,
                                        InventoryMovementService inventoryMovementService) {
        this.storeInventoryLogMapper = storeInventoryLogMapper;
        this.locationService = locationService;
        this.inventoryMovementService = inventoryMovementService;
    }

    private final StoreInventoryLogMapper storeInventoryLogMapper;
    private final LocationService locationService;
    private final InventoryMovementService inventoryMovementService;

    @Override
    public StoreInventoryLog createLog(StoreInventoryLog log) {
        require(log != null, "日志记录为空");
        require(log.getProductId() != null, "日志缺少 product_id");
        require(log.getStoreId() != null && !log.getStoreId().trim().isEmpty(), "日志缺少 store_id");

        Long storeId;
        try {
            storeId = Long.valueOf(log.getStoreId().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日志 store_id 非数字：" + log.getStoreId());
        }
        Location location = locationService.resolveByStoreId(storeId);
        require(location != null, "日志 store_id 无法解析到 location：" + log.getStoreId());

        InventoryMovement movement = new InventoryMovement();
        movement.setLocationId(location.getLocationId());
        movement.setMaterialId(log.getProductId());
        movement.setChangeQty(log.getChangeQuantity());
        movement.setBalanceAfter(log.getAfterStock());
        movement.setOperatorId(log.getOperatorId());
        movement.setMovementType(toMovementType(log.getType()));
        movement.setSourceType("STORE_INVENTORY_LOG");
        movement.setSourceRef(log.getRemark() != null && !log.getRemark().trim().isEmpty()
                ? log.getRemark().trim()
                : "store-log-" + System.currentTimeMillis());
        movement.setRemark(log.getRemark());
        movement.setCreateTime(log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now());
        inventoryMovementService.recordMovement(movement);
        return log;
    }

    /** 门店日志 type 口径 → 统一流水 movement_type（1 入库 / 2 出库 / 3 调拨 / 4 盘点 / 5 损耗） */
    private static String toMovementType(Integer type) {
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日志缺少 type");
        }
        return switch (type) {
            case 1 -> "IN";
            case 2, 5 -> "OUT";
            case 3 -> "TRANSFER_OUT";
            case 4 -> "ADJUST";
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "日志 type 未知：" + type);
        };
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    @Override
    public IPage<StoreInventoryLog> getLogPage(Page<StoreInventoryLog> page, String storeId, Long productId, Integer type) {
        String actualStoreId = storeId;

        if (!SecurityUtils.isAdmin() && SecurityUtils.hasStorePermission()) {
            String currentUserStoreId = SecurityUtils.getCurrentUserStoreId();
            if (currentUserStoreId != null && !currentUserStoreId.isEmpty()) {
                actualStoreId = currentUserStoreId;
            }
        }

        return storeInventoryLogMapper.selectLogPage(page, actualStoreId, productId, type);
    }
}
