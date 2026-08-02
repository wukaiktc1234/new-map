package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.StoreInventory;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.StoreInventoryMapper;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.service.StoreInventoryService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 门店库存服务实现类
 * 实现门店库存维度的核心业务逻辑，包括库存增加、扣减、低库存查询等
 *
 * 并发控制说明（修复 P0-ERP-03，2026-07-17）：
 * - 通过 StoreInventory 实体的 @Version 注解启用 MyBatis-Plus 乐观锁
 * - 库存写操作（increase/decrease）使用乐观锁 + 自动重试
 * - 新建记录走 INSERT，无需乐观锁；已有记录走 UPDATE，必须乐观锁
 * - updateById 返回 0 表示版本冲突，重试时重新读取最新数据
 */
@Service
public class StoreInventoryServiceImpl extends ServiceImpl<StoreInventoryMapper, StoreInventory>
        implements StoreInventoryService {

    private static final Logger log = LoggerFactory.getLogger(StoreInventoryServiceImpl.class);

    /**
     * 乐观锁冲突时的最大重试次数（含首次执行）
     */
    private static final int OPTIMISTIC_LOCK_MAX_RETRY = 3;

    /**
     * 乐观锁重试间隔（毫秒）
     */
    private static final long OPTIMISTIC_LOCK_RETRY_DELAY_MS = 50L;

    private final StoreInventoryMapper storeInventoryMapper;

    private final StoreInventoryLogService storeInventoryLogService;

    public StoreInventoryServiceImpl(StoreInventoryMapper storeInventoryMapper,
                                     StoreInventoryLogService storeInventoryLogService) {
        this.storeInventoryMapper = storeInventoryMapper;
        this.storeInventoryLogService = storeInventoryLogService;
    }

    @Override
    public IPage<StoreInventory> getStoreInventoryPage(Page<StoreInventory> page,
                                                       String storeId,
                                                       Long materialId,
                                                       String materialName) {
        LambdaQueryWrapper<StoreInventory> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null && !storeId.isEmpty()) {
            wrapper.eq(StoreInventory::getStoreId, storeId);
        }
        if (materialId != null) {
            wrapper.eq(StoreInventory::getMaterialId, materialId);
        }
        if (materialName != null && !materialName.isEmpty()) {
            wrapper.like(StoreInventory::getMaterialName, materialName);
        }
        wrapper.orderByDesc(StoreInventory::getCreateTime);
        return storeInventoryMapper.selectPage(page, wrapper);
    }

    @Override
    public StoreInventory getByStoreAndMaterial(String storeId, Long materialId) {
        LambdaQueryWrapper<StoreInventory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreInventory::getStoreId, storeId)
                .eq(StoreInventory::getMaterialId, materialId);
        return storeInventoryMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(String storeId, Long materialId, String materialName,
                              BigDecimal quantity, String unit, Long unitCost) {
        increaseStock(storeId, materialId, materialName, quantity, unit, unitCost, 1, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(String storeId, Long materialId, String materialName,
                              BigDecimal quantity, String unit, Long unitCost,
                              Integer changeType, String sourceRef) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "增加数量必须大于0");
        }

        // 单位成本默认为0
        long incomingUnitCost = (unitCost != null) ? unitCost : 0L;
        // 本次入库总成本
        long incomingTotalCost = incomingUnitCost * quantity.longValue();

        StoreInventory inventory = getByStoreAndMaterial(storeId, materialId);
        if (inventory == null) {
            // 新建门店库存记录：INSERT 无需乐观锁
            inventory = new StoreInventory();
            inventory.setStoreId(storeId);
            inventory.setMaterialId(materialId);
            inventory.setMaterialName(materialName);
            inventory.setUnit(unit);
            inventory.setCurrentStock(quantity);
            inventory.setUnitCost(incomingUnitCost);
            inventory.setTotalCost(incomingTotalCost);
            inventory.setCreateTime(LocalDateTime.now());
            inventory.setUpdateTime(LocalDateTime.now());
            storeInventoryMapper.insert(inventory);
            log.debug("新建门店库存记录：storeId={}, materialId={}, 数量={}, 单位成本={}分, 总成本={}分",
                    storeId, materialId, quantity, incomingUnitCost, incomingTotalCost);

            // P2-1: 写入门店库存流水
            writeLog(inventory.getId(), storeId, materialId, materialName,
                    BigDecimal.ZERO, quantity, quantity, changeType, sourceRef);
            return;
        }

        // 已有记录：使用乐观锁 + 重试
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 每次重试都重新读取最新数据
            StoreInventory current = getByStoreAndMaterial(storeId, materialId);
            if (current == null) {
                // 极端情况：并发期间记录被删除
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "门店库存记录在更新期间消失：storeId=" + storeId + ", materialId=" + materialId);
            }

            // 最新入库价格法：单位成本直接覆盖为本次入库单价
            BigDecimal beforeStock = current.getCurrentStock() == null
                    ? BigDecimal.ZERO : current.getCurrentStock();
            long beforeTotalCost = (current.getTotalCost() != null) ? current.getTotalCost() : 0L;

            BigDecimal newStock = beforeStock.add(quantity);
            long newTotalCost = beforeTotalCost + incomingTotalCost;
            long newUnitCost = incomingUnitCost;

            current.setCurrentStock(newStock);
            current.setUnitCost(newUnitCost);
            current.setTotalCost(newTotalCost);

            // 更新物料名称和单位（防止历史数据缺失）
            if (materialName != null && !materialName.isEmpty()) {
                current.setMaterialName(materialName);
            }
            if (unit != null && !unit.isEmpty()) {
                current.setUnit(unit);
            }
            current.setUpdateTime(LocalDateTime.now());

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = storeInventoryMapper.updateById(current);
            if (rows > 0) {
                log.debug("累加门店库存：storeId={}, materialId={}, 变动前={}/{}分, 入库={}/{}分, 变动后={}/{}分, 新单位成本={}分",
                        storeId, materialId, beforeStock, beforeTotalCost,
                        quantity, incomingTotalCost,
                        newStock, newTotalCost, newUnitCost);

                // P2-1: 写入门店库存流水
                writeLog(current.getId(), storeId, materialId, materialName,
                        beforeStock, newStock, quantity, changeType, sourceRef);
                return;
            }

            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "门店库存增加并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long decreaseStock(String storeId, Long materialId, BigDecimal quantity) {
        return decreaseStock(storeId, materialId, quantity, 2, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long decreaseStock(String storeId, Long materialId, BigDecimal quantity,
                              Integer changeType, String sourceRef) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "扣减数量必须大于0");
        }

        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 每次重试都重新读取最新数据
            StoreInventory inventory = getByStoreAndMaterial(storeId, materialId);
            if (inventory == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND,
                        "门店库存记录不存在：storeId=" + storeId + ", materialId=" + materialId);
            }

            BigDecimal currentStock = inventory.getCurrentStock() == null
                    ? BigDecimal.ZERO : inventory.getCurrentStock();
            if (currentStock.compareTo(quantity) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                        "门店库存不足，当前库存：" + currentStock + "，需要：" + quantity);
            }

            // 按当前单位成本计算出库成本
            long currentUnitCost = (inventory.getUnitCost() != null) ? inventory.getUnitCost() : 0L;
            long outgoingTotalCost = currentUnitCost * quantity.longValue();

            BigDecimal beforeStock = currentStock;
            long beforeTotalCost = (inventory.getTotalCost() != null) ? inventory.getTotalCost() : 0L;

            BigDecimal afterStock = currentStock.subtract(quantity);
            inventory.setCurrentStock(afterStock);
            inventory.setTotalCost(beforeTotalCost - outgoingTotalCost);
            // 单位成本保持不变（按当前单位成本出库）
            inventory.setUpdateTime(LocalDateTime.now());

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = storeInventoryMapper.updateById(inventory);
            if (rows > 0) {
                log.debug("扣减门店库存：storeId={}, materialId={}, 变动前={}/{}分, 出库={}/{}分, 变动后={}/{}分, 单位成本={}分",
                        storeId, materialId, beforeStock, beforeTotalCost,
                        quantity, outgoingTotalCost,
                        afterStock, inventory.getTotalCost(),
                        currentUnitCost);

                // P2-1: 写入门店库存流水
                writeLog(inventory.getId(), storeId, materialId, inventory.getMaterialName(),
                        beforeStock, afterStock, quantity, changeType, sourceRef);

                // 返回出库成本，供成本结转使用
                return outgoingTotalCost;
            }

            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "门店库存扣减并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    /**
     * P2-1：写入门店库存流水。
     * 流水属于审计记录，写入失败不影响主库存操作（仅记录错误日志），
     * 避免审计链路异常阻断采购入库/销售出库等核心业务。
     */
    private void writeLog(Long inventoryId, String storeId, Long materialId, String materialName,
                          BigDecimal beforeStock, BigDecimal afterStock, BigDecimal changeQty,
                          Integer changeType, String sourceRef) {
        try {
            StoreInventoryLog logEntry = new StoreInventoryLog();
            logEntry.setInventoryId(inventoryId);
            logEntry.setStoreId(storeId);
            logEntry.setProductId(materialId);
            logEntry.setProductName(materialName);
            logEntry.setType(changeType != null ? changeType : 1);
            logEntry.setBeforeStock(beforeStock);
            logEntry.setAfterStock(afterStock);
            logEntry.setChangeQuantity(changeQty);
            logEntry.setOperatorId(SecurityUtils.getCurrentUserId());
            logEntry.setOperatorName(SecurityUtils.getCurrentUsername());
            logEntry.setRemark(sourceRef);
            storeInventoryLogService.createLog(logEntry);
        } catch (Exception e) {
            log.error("写入门店库存流水失败: storeId={}, materialId={}, 错误={}",
                    storeId, materialId, e.getMessage(), e);
        }
    }

    @Override
    public List<StoreInventory> getLowStockList(String storeId) {
        LambdaQueryWrapper<StoreInventory> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null && !storeId.isEmpty()) {
            wrapper.eq(StoreInventory::getStoreId, storeId);
        }
        // 查询存在安全库存预警线且当前库存低于预警线的记录
        wrapper.isNotNull(StoreInventory::getSafetyStock)
                .apply("current_stock <= safety_stock");
        return storeInventoryMapper.selectList(wrapper);
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
                    "门店库存操作重试被中断（第 " + (attempt + 1) + " 次）");
        }
    }
}
