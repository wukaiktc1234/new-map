package com.foodtraceability.service.impl;

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
import com.foodtraceability.entity.InventoryTransaction;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryTransactionMapper;
import com.foodtraceability.service.InventoryService;
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
    private final InventoryTransactionMapper transactionMapper;

    public InventoryServiceImpl(InventoryMapper inventoryMapper,
                               InventoryTransactionMapper transactionMapper) {
        this.inventoryMapper = inventoryMapper;
        this.transactionMapper = transactionMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductInventory(InventoryDeductDTO deductDTO) {
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 每次重试都重新读取最新数据
            Inventory inventory = this.getById(deductDTO.getInventoryId());
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录不存在：" + deductDTO.getInventoryId());
            }

            // 检查可用数量是否足够
            BigDecimal availableQty = inventory.getQuantity().subtract(inventory.getLockedQuantity());
            if (availableQty.compareTo(deductDTO.getQuantity()) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                        "可扣减库存不足，当前可用：" + availableQty + "，需要：" + deductDTO.getQuantity());
            }

            // 记录变动前数量
            BigDecimal beforeQty = inventory.getQuantity();

            // 扣减库存
            inventory.setQuantity(inventory.getQuantity().subtract(deductDTO.getQuantity()));
            inventory.setTotalCost(calculateTotalCost(inventory));
            inventory.setUpdateTime(LocalDateTime.now());

            // 检查是否低于安全库存并更新状态
            updateInventoryStatus(inventory);

            // 乐观锁更新：@Version 自动添加 WHERE version = ? 条件
            int rows = inventoryMapper.updateById(inventory);
            if (rows > 0) {
                // 更新成功，记录库存变动（仅成功后记录，避免重试时重复插入）
                recordTransaction(inventory, deductDTO.getTransactionType() != null ? deductDTO.getTransactionType() : 2,
                        deductDTO.getQuantity().negate(), beforeQty, inventory.getQuantity(),
                        deductDTO.getReferenceNo(), deductDTO.getReferenceType());
                return;
            }

            // 版本冲突，准备重试
            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "库存扣减并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseInventory(InventoryIncreaseDTO increaseDTO) {
        // 查询或创建库存记录
        Inventory inventory = getByMaterialAndWarehouse(increaseDTO.getMaterialId(), increaseDTO.getWarehouseId());

        BigDecimal beforeQty;
        if (inventory == null) {
            // 创建新的库存记录：无需乐观锁（INSERT）
            inventory = new Inventory();
            inventory.setMaterialId(increaseDTO.getMaterialId());
            inventory.setWarehouseId(increaseDTO.getWarehouseId());
            inventory.setLocationId(increaseDTO.getLocationId());
            inventory.setQuantity(BigDecimal.ZERO);
            inventory.setLockedQuantity(BigDecimal.ZERO);
            inventory.setStatus(1);
            inventory.setCreateTime(LocalDateTime.now());
            beforeQty = BigDecimal.ZERO;

            // 更新库存信息
            inventory.setBatchNo(increaseDTO.getBatchNo());
            if (increaseDTO.getUnitCost() != null) {
                inventory.setUnitCost(increaseDTO.getUnitCost());
            }
            inventory.setQuantity(inventory.getQuantity().add(increaseDTO.getQuantity()));
            inventory.setTotalCost(calculateTotalCost(inventory));
            inventory.setUpdateTime(LocalDateTime.now());
            updateInventoryStatus(inventory);

            // INSERT：无需乐观锁
            this.saveOrUpdate(inventory);

            // 记录库存变动
            recordTransaction(inventory, increaseDTO.getTransactionType(),
                    increaseDTO.getQuantity(), beforeQty, inventory.getQuantity(),
                    increaseDTO.getReferenceNo(), increaseDTO.getReferenceType());
            return;
        }

        // 已有记录：使用乐观锁 + 重试
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 每次重试都重新读取最新数据
            Inventory current = getByMaterialAndWarehouse(increaseDTO.getMaterialId(), increaseDTO.getWarehouseId());
            if (current == null) {
                // 极端情况：并发期间记录被删除，按新建处理
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录在更新期间消失：materialId=" + increaseDTO.getMaterialId()
                                + ", warehouseId=" + increaseDTO.getWarehouseId());
            }

            beforeQty = current.getQuantity();

            // 更新库存信息
            current.setBatchNo(increaseDTO.getBatchNo());
            if (increaseDTO.getUnitCost() != null) {
                current.setUnitCost(increaseDTO.getUnitCost());
            }
            current.setQuantity(current.getQuantity().add(increaseDTO.getQuantity()));
            current.setTotalCost(calculateTotalCost(current));
            current.setUpdateTime(LocalDateTime.now());
            updateInventoryStatus(current);

            // 乐观锁更新
            int rows = inventoryMapper.updateById(current);
            if (rows > 0) {
                recordTransaction(current, increaseDTO.getTransactionType(),
                        increaseDTO.getQuantity(), beforeQty, current.getQuantity(),
                        increaseDTO.getReferenceNo(), increaseDTO.getReferenceType());
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
    public void decreaseInventory(InventoryDecreaseDTO decreaseDTO) {
        BusinessException lastException = null;
        for (int attempt = 0; attempt < OPTIMISTIC_LOCK_MAX_RETRY; attempt++) {
            // 根据物料ID和仓库ID查询库存记录（每次重试都重新读取）
            Inventory inventory = getByMaterialAndWarehouse(decreaseDTO.getMaterialId(), decreaseDTO.getWarehouseId());
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND,
                        "库存记录不存在：materialId=" + decreaseDTO.getMaterialId()
                                + ", warehouseId=" + decreaseDTO.getWarehouseId());
            }

            // 检查可用数量是否足够
            BigDecimal availableQty = inventory.getQuantity().subtract(inventory.getLockedQuantity());
            if (availableQty.compareTo(decreaseDTO.getQuantity()) < 0) {
                throw new BusinessException(ErrorCode.INVENTORY_INSUFFICIENT,
                        "可扣减库存不足，当前可用：" + availableQty + "，需要：" + decreaseDTO.getQuantity());
            }

            // 记录变动前数量
            BigDecimal beforeQty = inventory.getQuantity();

            // 扣减库存
            inventory.setQuantity(inventory.getQuantity().subtract(decreaseDTO.getQuantity()));
            inventory.setTotalCost(calculateTotalCost(inventory));
            inventory.setUpdateTime(LocalDateTime.now());
            updateInventoryStatus(inventory);

            // 乐观锁更新
            int rows = inventoryMapper.updateById(inventory);
            if (rows > 0) {
                recordTransaction(inventory, decreaseDTO.getTransactionType() != null ? decreaseDTO.getTransactionType() : 1,
                        decreaseDTO.getQuantity().negate(), beforeQty, inventory.getQuantity(),
                        decreaseDTO.getReferenceNo(), decreaseDTO.getReferenceType());
                return;
            }

            lastException = new BusinessException(ErrorCode.INVENTORY_CONFLICT,
                    "库存减少并发冲突，已重试 " + (attempt + 1) + " 次");
            sleepForRetry(attempt);
        }
        throw lastException;
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
        return inventoryMapper.selectByMaterialAndWarehouse(materialId, warehouseId);
    }

    @Override
    public List<Inventory> getLowStockList(Long warehouseId) {
        // 注意：inventory 表的 quantity 字段已重命名为 current_stock，
        // 此处 raw SQL 必须使用真实数据库列名 current_stock
        if (warehouseId != null) {
            return this.lambdaQuery()
                    .eq(Inventory::getWarehouseId, warehouseId)
                    .isNotNull(Inventory::getMinSafeQty)
                    .apply("current_stock <= min_safe_qty")
                    .list();
        } else {
            return this.lambdaQuery()
                    .isNotNull(Inventory::getMinSafeQty)
                    .apply("current_stock <= min_safe_qty")
                    .list();
        }
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
     * 计算总成本
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
     * 记录库存变动
     */
    private void recordTransaction(Inventory inventory, Integer transactionType,
                                   BigDecimal quantityChange, BigDecimal beforeQty,
                                   BigDecimal afterQty, String referenceNo, String referenceType) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionType(transactionType);
        transaction.setInventoryId(inventory.getInventoryId());
        transaction.setMaterialId(inventory.getMaterialId());
        transaction.setWarehouseId(inventory.getWarehouseId());
        transaction.setQuantityChange(quantityChange);
        transaction.setBeforeQty(beforeQty);
        transaction.setAfterQty(afterQty);
        transaction.setUnitCost(inventory.getUnitCost());
        transaction.setTotalCost(inventory.getTotalCost());
        transaction.setReferenceNo(referenceNo);
        transaction.setReferenceType(referenceType);
        transaction.setCreateTime(LocalDateTime.now());

        transactionMapper.insert(transaction);

        // 发送库存变动通知消息（仓储→采购等其他系统）
        publishStockChangedEvent(inventory, transactionType, quantityChange, referenceNo, referenceType);
    }

    /**
     * 发布库存变动事件
     * 重构说明：已移除 RabbitMQ，改为日志记录。其他系统可通过数据库查询获取库存变动信息。
     */
    private void publishStockChangedEvent(Inventory inventory, Integer transactionType,
                                          BigDecimal quantityChange, String referenceNo,
                                          String referenceType) {
        log.info("库存变动：inventoryId={}, materialId={}, warehouseId={}, transactionType={}, quantityChange={}, currentQuantity={}, status={}, referenceNo={}, referenceType={}",
                inventory.getInventoryId(), inventory.getMaterialId(), inventory.getWarehouseId(),
                transactionType, quantityChange, inventory.getQuantity(), inventory.getStatus(),
                referenceNo, referenceType);
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
}
