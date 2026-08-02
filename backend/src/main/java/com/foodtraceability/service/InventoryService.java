package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryDeductDTO;
import com.foodtraceability.dto.IncreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.dto.InventoryLockDTO;
import com.foodtraceability.dto.InventoryQueryDTO;
import com.foodtraceability.entity.Inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存服务接口
 * 定义库存管理相关的核心业务方法
 */
public interface InventoryService extends IService<Inventory> {

    /**
     * 分页查询库存列表
     *
     * @param page 分页对象
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    IPage<Inventory> getInventoryPage(Page<Inventory> page, InventoryQueryDTO queryDTO);

    /**
     * 锁定库存（用于订单预留）
     *
     * @param lockDTO 锁定请求DTO
     */
    void lockInventory(InventoryLockDTO lockDTO);

    /**
     * 解锁库存（取消订单预留）
     *
     * @param inventoryId 库存ID
     * @param quantity 解锁数量
     */
    void unlockInventory(Long inventoryId, BigDecimal quantity);

    /**
     * 扣减库存（销售出库等场景）
     *
     * @param deductDTO 扣减请求DTO
     */
    void deductInventory(InventoryDeductDTO deductDTO);

    /**
     * 增加库存（采购入库、调拨入库、盘盈等场景）
     *
     * @param increaseDTO 增加请求DTO
     */
    void increaseInventory(InventoryIncreaseDTO increaseDTO);

    /**
     * 减少库存（销售出库、领料出库、盘亏等场景）
     * 简化版扣减接口，根据物料ID和仓库ID自动查找库存记录
     *
     * @param decreaseDTO 减少请求DTO
     */
    void decreaseInventory(InventoryDecreaseDTO decreaseDTO);

    /**
     * 查询可用库存数量（当前数量 - 锁定数量）
     *
     * @param inventoryId 库存ID
     * @return 可用数量
     */
    BigDecimal getAvailableQuantity(Long inventoryId);

    /**
     * 根据物料ID和仓库ID查询库存
     *
     * @param materialId 物料ID
     * @param warehouseId 仓库ID
     * @return 库存实体
     */
    Inventory getByMaterialAndWarehouse(Long materialId, Long warehouseId);

    /**
     * 获取低库存列表
     *
     * @param warehouseId 仓库ID（可选，为空则查所有仓库）
     * @return 低库存列表
     */
    List<Inventory> getLowStockList(Long warehouseId);

    /**
     * 获取即将过期的库存列表
     *
     * @param days 天数阈值（默认30天）
     * @return 即将过期列表
     */
    List<Inventory> getExpiringSoonList(Integer days);
}
