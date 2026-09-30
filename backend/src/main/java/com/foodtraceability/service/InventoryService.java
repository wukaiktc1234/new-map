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

    // ==================== M3-M4 位置维度方法（S4a 新增；S4b 前委托 StoreInventoryService，运行时行为不变） ====================

    /**
     * 按位置ID+物料ID查库存（统一账唯一键定位）
     * @param locationId 位置ID（STORE/CENTRAL/DEPOT 统一维度）
     * @return 不存在返回 null
     */
    Inventory getByLocationAndMaterial(Long locationId, Long materialId);

    /**
     * 按位置增加库存（入建出抛不对称的"入"侧：不存在则新建行）
     * 加权平均法计算单位成本
     *
     * @param sourceType 来源业务类型（S5 必填，词表见 -001 §1.4；宪法 §IV.4 强制非空）
     * @param sourceRef  来源单据号（S5 必填）
     */
    void increaseStockAtLocation(Long locationId, Long materialId, String materialName,
                                 java.math.BigDecimal quantity, String unit, Long unitCost,
                                 String sourceType, String sourceRef);

    /**
     * 按位置增加库存（带批次号重载，P0-A 卡新增）。
     * <p>legacy `increaseInventory` 现状行为含"写入批次号"（`current.setBatchNo(...)`），
     * 收编至统一账后需保留该元数据语义；无批次号的调用方走 8 参重载（batchNo=null）。</p>
     *
     * @param batchNo 批次号（可空；非空时覆盖库存行批次号，不参与数量计算）
     */
    void increaseStockAtLocation(Long locationId, Long materialId, String materialName,
                                 java.math.BigDecimal quantity, String unit, Long unitCost,
                                 String batchNo, String sourceType, String sourceRef);

    /**
     * 按位置扣减库存（"出"侧：不存在或不足抛 BusinessException）
     *
     * @param sourceType 来源业务类型（S5 必填，词表见 -001 §1.4；宪法 §IV.4 强制非空）
     * @param sourceRef  来源单据号（S5 必填）
     * @return 出库总成本（分）
     */
    Long decreaseStockAtLocation(Long locationId, Long materialId,
                                 java.math.BigDecimal quantity,
                                 String sourceType, String sourceRef);

    /**
     * 按位置分页查库存（统一账维度）
     */
    IPage<Inventory> getStockPageAtLocation(Page<Inventory> page, Long locationId,
                                            Long materialId, String materialName);

    /**
     * 低库存列表（location 维度；阈值列按位置类型语义：安全线/预警阈值双列并存，宪法裁定日志）
     */
    java.util.List<Inventory> getLowStockAtLocation(Long locationId);

    /**
     * store_id（STORE 型 location 别名）→ location_id 解析（规则 3/4 标准入口的便捷方法）。
     * 解析失败返回 null，调用方决定拒绝语义。
     */
    Long resolveLocationIdByStoreId(Long storeId);

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
