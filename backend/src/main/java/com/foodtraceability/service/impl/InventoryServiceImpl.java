package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryDeductDTO;
import com.foodtraceability.dto.IncreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.InventoryLockDTO;
import com.foodtraceability.dto.InventoryQueryDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryMovement;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.service.InventoryMovementService;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存服务实现类
 * 实现库存管理的核心业务逻辑，包括库存锁定、扣减、增加等操作
 *
 * 重构说明：
 * - 已移除 RabbitMQ 依赖（RabbitTemplate）
 * - 库存变动通知改为日志记录，由其他系统通过数据库查询获取变动信息
 * - 保留所有业务逻辑和数据库操作
 *
 * 并发控制说明（修复 P0-ERP-03/P0-ERP-04，2026-07-17）：
 * - 通过 Inventory 实体的 @Version 注解启用 MyBatis-Plus 乐观锁
 * - 所有库存写操作（lock/unlock/deduct/increase/decrease）使用乐观锁 + 自动重试
 * - updateById 返回 0 表示版本冲突，重试时重新读取最新数据
 * - 最大重试次数 OPTIMISTIC_LOCK_MAX_RETRY，重试间隔 OPTIMISTIC_LOCK_RETRY_DELAY_MS
 */
@Service
public class InventoryServiceImpl extends ServiceImpl<InventoryMapper, Inventory> implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    /**
     * 乐观锁冲突时的最大重试次数（含首次执行）
     */
    private static final int OPTIMISTIC_LOCK_MAX_RETRY = 3;

    /**
     * 乐观锁重试间隔（毫秒），使用固定间隔避免退避算法引入额外延迟
     */
    private static final long OPTIMISTIC_LOCK_RETRY_DELAY_MS = 50L;

    private final InventoryMapper inventoryMapper;
    private final InventoryMovementService inventoryMovementService;
    private final LocationService locationService;

    public InventoryServiceImpl(InventoryMapper inventoryMapper,
                               InventoryMovementService inventoryMovementService,
                               LocationService locationService) {
        this.inventoryMapper = inventoryMapper;
        this.inventoryMovementService = inventoryMovementService;
        this.locationService = locationService;
    }

    @Override
    public IPage<Inventory> getInventoryPage(Page<Inventory> page, InventoryQueryDTO queryDTO) {
        return inventoryMapper.selectInventoryPage(page,
                queryDTO.getWarehouseId(),
                queryDTO.getMaterialId(),
                queryDTO.getMaterialName(),
                queryDTO.getBatchNo(),
                queryDTO.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockInventory(InventoryLockDTO lockDTO) {
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 每次重试都重新读取最新数据（PostgreSQL READ COMMITTED 下可读到已提交的最新版本）
            Inventory inventory = this.getById(lockDTO.getInventoryId());
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录不存在：" + lockDTO.getInventoryId());
            }

            // 检查可用数量是否足够
            BigDecimal availableQty = inventory.getQuantity().subtract(inventory.getLockedQuantity());
            if (availableQty.compareTo(lockDTO.getQuantity()) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_LOCK_INSUFFICIENT,
                        "可用库存不足，当前可用：" + availableQty + "，需要：" + lockDTO.getQuantity());
            }

            // 更新锁定数量
            inventory.setLockedQuantity(inventory.getLockedQuantity().add(lockDTO.getQuantity()));
            inventory.setUpdateTime(LocalDateTime.now());

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = inventoryMapper.updateById(inventory);
            if (rows > 0) {
                return;
            }

            // 版本冲突，准备重试
            lastException = new BusinessException(ErrorCode.INVENTORY_LOCK_CONFLICT,
                    "库存锁定并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockInventory(Long inventoryId, BigDecimal quantity) {
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            Inventory inventory = this.getById(inventoryId);
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录不存在：" + inventoryId);
            }

            // 更新锁定数量（减少）
            BigDecimal newLockedQty = inventory.getLockedQuantity().subtract(quantity);
            if (newLockedQty.compareTo(BigDecimal.ZERO) < 0) {
                newLockedQty = BigDecimal.ZERO;
            }
            inventory.setLockedQuantity(newLockedQty);
            inventory.setUpdateTime(LocalDateTime.now());

            int rows = inventoryMapper.updateById(inventory);
            if (rows > 0) {
                return;
            }

            lastException = new BusinessException(ErrorCode.INVENTORY_LOCK_CONFLICT,
                    "库存解锁并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    /**
     * 扣减库存（按 inventoryId 定位；P0-A 收编：统一账为唯一写路径）。
     * <p>收编前写 legacy `inventory_transactions`（表已更名 → 500）；现改为 location 维度委派
     * {@link #decreaseStockAtLocation}，并保留 legacy 语义：可用量 = 数量 − 锁定数量（现状迁移裁定日志）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInventory(InventoryDeductDTO deductDTO) {
        requireSourceType(deductDTO.getSourceType(), "扣减库存");
        String sourceRef = buildSourceRef(deductDTO.getSourceType(), deductDTO.getReferenceType(),
                deductDTO.getReferenceNo(), "扣减库存");

        Inventory inventory = this.getById(deductDTO.getInventoryId());
        if (inventory == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                    "库存记录不存在：" + deductDTO.getInventoryId());
        }
        if (inventory.getLocationId() == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                    "库存记录缺少 location_id，无法按统一账扣减：" + deductDTO.getInventoryId());
        }

        // 现状迁移：可用量 = 数量 − 锁定数量（legacy deductInventory 口径，保留）
        BigDecimal lockedQty = inventory.getLockedQuantity() != null ? inventory.getLockedQuantity() : BigDecimal.ZERO;
        BigDecimal availableQty = inventory.getQuantity().subtract(lockedQty);
        if (availableQty.compareTo(deductDTO.getQuantity()) < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                    "可扣减库存不足，当前可用：" + availableQty + "，需要：" + deductDTO.getQuantity());
        }

        decreaseStockAtLocation(inventory.getLocationId(), inventory.getMaterialId(),
                deductDTO.getQuantity(), deductDTO.getSourceType(), sourceRef);
        refreshStatusAfterLegacyWrite(inventory.getLocationId(), inventory.getMaterialId());
    }

    /**
     * 增加库存（仓库维度入口；P0-A 收编：统一账 location 维度为唯一写路径）。
     * <p>收编前写 legacy `inventory_transactions`（表已更名 → 500）；现改为
     * warehouseId → locationId 经 map 解析（未映射显式拒绝，宪法 §三.4）→ 委派
     * {@link #increaseStockAtLocation}（带批次号重载，保留 legacy 批次号语义）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseInventory(InventoryIncreaseDTO increaseDTO) {
        requireSourceType(increaseDTO.getSourceType(), "增加库存");
        String sourceRef = buildSourceRef(increaseDTO.getSourceType(), increaseDTO.getReferenceType(),
                increaseDTO.getReferenceNo(), "增加库存");

        Long locationId = resolveLocationIdForLegacyWrite(increaseDTO.getWarehouseId(), "增加库存");
        increaseStockAtLocation(locationId, increaseDTO.getMaterialId(), null, increaseDTO.getQuantity(),
                null, increaseDTO.getUnitCost(), increaseDTO.getBatchNo(),
                increaseDTO.getSourceType(), sourceRef);
        refreshStatusAfterLegacyWrite(locationId, increaseDTO.getMaterialId());
    }

    /**
     * 减少库存（仓库维度入口；P0-A 收编：统一账 location 维度为唯一写路径）。
     * <p>保留 legacy 语义：可用量 = 数量 − 锁定数量（现状迁移裁定日志）。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseInventory(InventoryDecreaseDTO decreaseDTO) {
        requireSourceType(decreaseDTO.getSourceType(), "减少库存");
        String sourceRef = buildSourceRef(decreaseDTO.getSourceType(), decreaseDTO.getReferenceType(),
                decreaseDTO.getReferenceNo(), "减少库存");

        Long locationId = resolveLocationIdForLegacyWrite(decreaseDTO.getWarehouseId(), "减少库存");
        Inventory inventory = getByLocationAndMaterial(locationId, decreaseDTO.getMaterialId());
        if (inventory == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                    "库存记录不存在：materialId=" + decreaseDTO.getMaterialId()
                            + ", warehouseId=" + decreaseDTO.getWarehouseId());
        }

        // 现状迁移：可用量 = 数量 − 锁定数量（legacy decreaseInventory 口径，保留）
        BigDecimal lockedQty = inventory.getLockedQuantity() != null ? inventory.getLockedQuantity() : BigDecimal.ZERO;
        BigDecimal availableQty = inventory.getQuantity().subtract(lockedQty);
        if (availableQty.compareTo(decreaseDTO.getQuantity()) < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                    "可扣减库存不足，当前可用：" + availableQty + "，需要：" + decreaseDTO.getQuantity());
        }

        decreaseStockAtLocation(locationId, decreaseDTO.getMaterialId(), decreaseDTO.getQuantity(),
                decreaseDTO.getSourceType(), sourceRef);
        refreshStatusAfterLegacyWrite(locationId, decreaseDTO.getMaterialId());
    }

    @Override
    public BigDecimal getAvailableQuantity(Long inventoryId) {
        Inventory inventory = this.getById(inventoryId);
        if (inventory == null) {
            return BigDecimal.ZERO;
        }
        return inventory.getQuantity().subtract(inventory.getLockedQuantity());
    }

    @Override
    public Inventory getByMaterialAndWarehouse(Long materialId, Long warehouseId) {
        // M3-M4 S6b：原 raw SQL selectByMaterialAndWarehouse 删除；
        // 改为 warehouse→location 解析 + 统一账 LambdaQueryWrapper 查询（兼容 legacy 调用方）
        com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
        if (location == null) {
            return null;
        }
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Inventory::getLocationId, location.getLocationId())
               .eq(Inventory::getMaterialId, materialId)
               .last("LIMIT 1");
        return inventoryMapper.selectOne(wrapper);
    }

    @Override
    public List<Inventory> getLowStockList(Long warehouseId) {
        // M3-M4 S6b：原 raw SQL current_stock 列名改为新表 quantity；warehouseId→locationId 解析
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Inventory::getMinSafeQty)
               .apply("quantity <= min_safe_qty");
        if (warehouseId != null) {
            com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
            if (location == null) {
                return java.util.Collections.emptyList();
            }
            wrapper.eq(Inventory::getLocationId, location.getLocationId());
        }
        return inventoryMapper.selectList(wrapper);
    }

    @Override
    public List<Inventory> getExpiringSoonList(Integer days) {
        if (days == null || days <= 0) {
            days = 30; // 默认30天
        }
        // 查询 expiry_date 在今日起 days 天内的记录（含今日已过期）
        return this.lambdaQuery()
                .isNotNull(Inventory::getExpiryDate)
                .le(Inventory::getExpiryDate, LocalDate.now().plusDays(days))
                .list();
    }

    /**
     * 计算总成本（P0-A：legacy 全量重估公式的调用方 recordTransaction 已删除，
     * 本方法暂留供后续口径对齐；当前无调用方）
     */
    private Long calculateTotalCost(Inventory inventory) {
        if (inventory.getUnitCost() == null || inventory.getQuantity() == null) {
            return 0L;
        }
        return inventory.getUnitCost() * inventory.getQuantity().longValue();
    }

    /**
     * 更新库存状态
     */
    private void updateInventoryStatus(Inventory inventory) {
        // 如果库存为0或负数，标记为冻结
        if (inventory.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            inventory.setStatus(4); // 冻结
        }
        // 如果低于安全库存，标记为预警
        else if (inventory.getMinSafeQty() != null &&
                inventory.getQuantity().compareTo(inventory.getMinSafeQty()) < 0) {
            inventory.setStatus(2); // 预警
        }
        else {
            inventory.setStatus(1); // 正常
        }
    }

    /**
     * P0-A 收编辅助：统一流水 source_type 必填校验。
     * 缺省即显式拒绝——禁止默认兜底（宪法 §III.4）、禁止无来源流水（宪法 §IV.4）。
     */
    private void requireSourceType(String sourceType, String method) {
        if (sourceType == null || sourceType.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    method + " 缺少业务来源 sourceType（统一流水禁无来源记录，宪法 §IV.4）");
        }
    }

    /**
     * P0-A 收编辅助：统一流水 source_ref 组装（`referenceType:referenceNo`）。
     * 关联单号缺失即显式拒绝——不伪造、不兜底（宪法 §III.4 / §IV.4）。
     */
    private String buildSourceRef(String sourceType, String referenceType, String referenceNo, String method) {
        if (referenceNo == null || referenceNo.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    method + " 缺少关联单据号 referenceNo，无法形成可追溯流水（宪法 §IV.4）");
        }
        String prefix = (referenceType != null && !referenceType.trim().isEmpty())
                ? referenceType.trim() : sourceType;
        return prefix + ":" + referenceNo.trim();
    }

    /**
     * P0-A 收编辅助：legacy 仓库维度入口 → location_id 解析（规则 4 唯一入口）。
     * 未映射仓库显式拒绝，与同族 6 处文案口径一致（宪法 §三.4）。
     */
    private Long resolveLocationIdForLegacyWrite(Long warehouseId, String method) {
        if (warehouseId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, method + " 缺少仓库ID，无法解析入账位置");
        }
        com.foodtraceability.entity.Location location = locationService.resolveByWarehouseId(warehouseId);
        if (location == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    method + " 仓库未映射到位置，禁止入账：warehouseId=" + warehouseId);
        }
        return location.getLocationId();
    }

    /**
     * P0-A 收编辅助：legacy 写路径的"库存状态重算"现状语义保留（1正常/2预警/4冻结）。
     * 仅刷新状态元数据，不改数量与流水。
     */
    private void refreshStatusAfterLegacyWrite(Long locationId, Long materialId) {
        Inventory row = getByLocationAndMaterial(locationId, materialId);
        if (row == null) {
            return;
        }
        updateInventoryStatus(row);
        row.setUpdateTime(LocalDateTime.now());
        inventoryMapper.updateById(row);
    }

    /**
     * 乐观锁冲突时等待重试
     * 注意：保持线程的中断状态，避免吞没中断信号
     *
     * @param attempt 当前是第几次重试（0-based）
     */
    private void sleepForRetry(int attempt) {
        try {
            Thread.sleep(OPTIMISTIC_LOCK_RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "库存操作重试被中断（第 " + (attempt + 1) + " 次）");
        }
    }

    // ==================== M3-M4 位置维度方法（S4b：统一账直算） ====================
    // 行为敏感点现状迁移签字（矩阵 §6）：
    //   §6-1 入建出抛不对称 —— 保留：increase 不存在则 INSERT，decrease 不存在抛 NOT_FOUND
    //   §6-3 流水尽力而为 —— 保留：writeMovement 失败仅记日志，不回滚库存变更
    //   §6-6 调拨成本清零 —— 保留：unitCost=null 时单位成本按 0 处理（调用方 S4c/S6 传入口径不变，Q3 批复前不改）
    //   §6-8 事务边界 —— 不在本方法层：调用方事务策略（DF-001 等）S4c/S6 收编时逐个核对

    @Override
    public Inventory getByLocationAndMaterial(Long locationId, Long materialId) {
        if (locationId == null || materialId == null) {
            return null;
        }
        return inventoryMapper.selectByMaterialAndLocation(locationId, materialId);
    }

    /** 8 参重载（无批次号）：委派 9 参重载，既有调用方零改动。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStockAtLocation(Long locationId, Long materialId, String materialName,
                                        BigDecimal quantity, String unit, Long unitCost,
                                        String sourceType, String sourceRef) {
        increaseStockAtLocation(locationId, materialId, materialName, quantity, unit, unitCost,
                null, sourceType, sourceRef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStockAtLocation(Long locationId, Long materialId, String materialName,
                                        BigDecimal quantity, String unit, Long unitCost,
                                        String batchNo, String sourceType, String sourceRef) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "增加数量必须大于0");
        }
        // 单位成本默认为0（§6-6 现状迁移：unitCost=null 按 0）
        long incomingUnitCost = (unitCost != null) ? unitCost : 0L;
        long incomingTotalCost = incomingUnitCost * quantity.longValue();

        Inventory current = getByLocationAndMaterial(locationId, materialId);
        if (current == null) {
            // §6-1 入建：INSERT 无需乐观锁
            Inventory inv = new Inventory();
            inv.setLocationId(locationId);
            inv.setMaterialId(materialId);
            inv.setMaterialName(materialName);
            inv.setUnit(unit);
            inv.setQuantity(quantity);
            inv.setUnitCost(incomingUnitCost);
            inv.setTotalCost(incomingTotalCost);
            // P0-A：批次号现状语义保留（原 legacy increaseInventory 会写入 batch_no）
            if (batchNo != null && !batchNo.isEmpty()) {
                inv.setBatchNo(batchNo);
            }
            inv.setCreateTime(LocalDateTime.now());
            inv.setUpdateTime(LocalDateTime.now());
            inventoryMapper.insert(inv);
            log.debug("新建统一库存行：locationId={}, materialId={}, 数量={}, 单位成本={}分",
                    locationId, materialId, quantity, incomingUnitCost);
            writeMovement(locationId, materialId, materialName,
                    BigDecimal.ZERO, quantity, quantity, "IN", sourceType, sourceRef,
                    incomingUnitCost, incomingTotalCost);
            return;
        }

        // 已有行：乐观锁 + 3 次重试；最新入库价格法（现状迁移，§6-6 口径不变）
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            Inventory fresh = getByLocationAndMaterial(locationId, materialId);
            if (fresh == null) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录在更新期间消失：locationId=" + locationId + ", materialId=" + materialId);
            }
            BigDecimal beforeStock = fresh.getQuantity() == null ? BigDecimal.ZERO : fresh.getQuantity();
            long beforeTotalCost = (fresh.getTotalCost() != null) ? fresh.getTotalCost() : 0L;
            BigDecimal newStock = beforeStock.add(quantity);
            long newTotalCost = beforeTotalCost + incomingTotalCost;
            long newUnitCost = incomingUnitCost;

            fresh.setQuantity(newStock);
            fresh.setUnitCost(newUnitCost);
            fresh.setTotalCost(newTotalCost);
            if (materialName != null && !materialName.isEmpty()) {
                fresh.setMaterialName(materialName);
            }
            if (unit != null && !unit.isEmpty()) {
                fresh.setUnit(unit);
            }
            // P0-A：批次号现状语义保留（legacy increaseInventory 覆盖批次号）
            if (batchNo != null && !batchNo.isEmpty()) {
                fresh.setBatchNo(batchNo);
            }
            fresh.setUpdateTime(LocalDateTime.now());

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = inventoryMapper.updateById(fresh);
            if (rows > 0) {
                writeMovement(locationId, materialId, materialName,
                        beforeStock, newStock, quantity, "IN", sourceType, sourceRef,
                        incomingUnitCost, incomingTotalCost);
                return;
            }
            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "库存增加并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long decreaseStockAtLocation(Long locationId, Long materialId,
                                        BigDecimal quantity,
                                        String sourceType, String sourceRef) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "扣减数量必须大于0");
        }
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            Inventory current = getByLocationAndMaterial(locationId, materialId);
            if (current == null) {
                // §6-1 出抛：库存行不存在直接失败，禁止凭空建行
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "库存记录不存在：locationId=" + locationId + ", materialId=" + materialId);
            }
            BigDecimal currentStock = current.getQuantity() == null ? BigDecimal.ZERO : current.getQuantity();
            if (currentStock.compareTo(quantity) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                        "库存不足，当前库存：" + currentStock + "，需要：" + quantity);
            }
            // 按当前单位成本结转出库成本（现状迁移）
            long currentUnitCost = (current.getUnitCost() != null) ? current.getUnitCost() : 0L;
            long outgoingTotalCost = currentUnitCost * quantity.longValue();
            BigDecimal beforeStock = currentStock;
            long beforeTotalCost = (current.getTotalCost() != null) ? current.getTotalCost() : 0L;
            BigDecimal afterStock = currentStock.subtract(quantity);

            current.setQuantity(afterStock);
            current.setTotalCost(beforeTotalCost - outgoingTotalCost);
            current.setUpdateTime(LocalDateTime.now());

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = inventoryMapper.updateById(current);
            if (rows > 0) {
                writeMovement(locationId, materialId, current.getMaterialName(),
                        beforeStock, afterStock, quantity.negate(), "OUT", sourceType, sourceRef,
                        currentUnitCost, outgoingTotalCost);
                // 返回出库成本，供成本结转使用
                return outgoingTotalCost;
            }
            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "库存扣减并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    @Override
    public IPage<Inventory> getStockPageAtLocation(Page<Inventory> page, Long locationId,
                                                   Long materialId, String materialName) {
        return inventoryMapper.selectInventoryPage(page, locationId, materialId, materialName, null, null);
    }

    @Override
    public List<Inventory> getLowStockAtLocation(Long locationId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (locationId != null) {
            wrapper.eq(Inventory::getLocationId, locationId);
        }
        // 门店域安全线口径现状迁移（原 StoreInventoryServiceImpl:288：current_stock <= safety_stock）
        wrapper.isNotNull(Inventory::getSafetyStock)
                .apply("quantity <= safety_stock");
        return inventoryMapper.selectList(wrapper);
    }

    @Override
    public Long resolveLocationIdByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        com.foodtraceability.entity.Location location = locationService.resolveByStoreId(storeId);
        return location == null ? null : location.getLocationId();
    }

    /**
     * 统一流水写入（M3-M4 S5 流水统一；§6-3 现状迁移：尽力而为，失败仅记日志不回滚）。
     * source_type/source_ref 为 NOT NULL（宪法 §IV.4 DDL 红线）：缺来源时由
     * InventoryMovementService 以异常拒绝；此处捕获后降级（仅日志）——不写无来源流水、
     * 不伪造兜底值（§III.4 禁止默认兜底）。
     * change_qty 符号约定按 -001 §1.4：正=入、负=出。
     */
    private void writeMovement(Long locationId, Long materialId, String materialName,
                               BigDecimal beforeStock, BigDecimal afterStock, BigDecimal changeQty,
                               String movementType, String sourceType, String sourceRef,
                               Long unitCost, Long totalCost) {
        try {
            InventoryMovement movement = new InventoryMovement();
            movement.setLocationId(locationId);
            movement.setMaterialId(materialId);
            movement.setChangeQty(changeQty);
            movement.setBalanceAfter(afterStock);
            movement.setMovementType(movementType);
            movement.setSourceType(sourceType);
            movement.setSourceRef(sourceRef);
            movement.setOperatorId(SecurityUtils.getCurrentUserId());
            movement.setUnitCost(unitCost);
            movement.setTotalCost(totalCost);
            movement.setRemark(materialName);
            inventoryMovementService.recordMovement(movement);
        } catch (Exception e) {
            log.error("写入统一库存流水失败（降级，库存变更不回滚）: locationId={}, materialId={}, sourceType={}, sourceRef={}, 错误={}",
                    locationId, materialId, sourceType, sourceRef, e.getMessage(), e);
        }
    }
}
