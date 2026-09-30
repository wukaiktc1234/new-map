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

    /**
     * 门店库存日志写路径的来源标注（收口遗留 3）：替代词表外 source_type，
     * 随 remark 写入统一流水，保证"来源可辨"且不新增词表外枚举值。
     */
    private static final String SOURCE_ORIGIN_NOTE = "门店库存日志写路径（原 source_type=STORE_INVENTORY_LOG，现按词表取 OTHER）";

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
        // 收口遗留 3（§24.3j/§24.3k）：S9a-1 自造的 source_type='STORE_INVENTORY_LOG' 不在
        // -001 §1.4 词表内。改用词表内 OTHER，来源语义由 remark 标注承载——不取"补词表"方案，
        // 因补词需改动 Owner 拍板的 -001 设计（项目惯例：补充设计只落 -002，-001 不动）。
        movement.setSourceType("OTHER");
        String userRemark = log.getRemark() != null ? log.getRemark().trim() : "";
        String mergedRemark = userRemark.isEmpty()
                ? SOURCE_ORIGIN_NOTE
                : SOURCE_ORIGIN_NOTE + " - " + userRemark;
        movement.setRemark(mergedRemark.length() > 500 ? mergedRemark.substring(0, 500) : mergedRemark);
        movement.setSourceRef(userRemark.isEmpty() ? "store-log-" + System.currentTimeMillis() : userRemark);
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
