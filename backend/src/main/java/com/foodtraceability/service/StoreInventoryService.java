package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.StoreInventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 门店库存服务接口
 * 定义门店库存维度的核心业务方法，独立于仓库库存
 */
public interface StoreInventoryService extends IService<StoreInventory> {

    /**
     * 分页查询门店库存列表
     *
     * @param page 分页对象
     * @param storeId 门店ID（可选）
     * @param materialId 物料ID（可选）
     * @param materialName 物料名称（可选，模糊查询）
     * @return 分页结果
     */
    IPage<StoreInventory> getStoreInventoryPage(Page<StoreInventory> page,
                                                String storeId,
                                                Long materialId,
                                                String materialName);

    /**
     * 根据门店ID和物料ID查询门店库存
     *
     * @param storeId 门店ID
     * @param materialId 物料ID
     * @return 门店库存实体，不存在返回null
     */
    StoreInventory getByStoreAndMaterial(String storeId, Long materialId);

    /**
     * 增加门店库存（采购入库、调拨入库等场景）
     * 若记录存在则累加，不存在则新建
     * 使用加权平均法计算单位成本
     *
     * @param storeId 门店ID
     * @param materialId 物料ID
     * @param materialName 物料名称
     * @param quantity 增加数量
     * @param unit 单位
     * @param unitCost 本次入库单位成本（分）
     */
    void increaseStock(String storeId, Long materialId, String materialName,
                       BigDecimal quantity, String unit, Long unitCost);

    /**
     * 增加门店库存（携带变动类型与来源备注，写入库存流水）
     *
     * @param changeType 变动类型（1:入库,2:出库,3:调拨,4:盘点,5:损耗）
     * @param sourceRef  来源备注（如 采购入库单号 / 到货确认单号 / 调拨单号）
     */
    void increaseStock(String storeId, Long materialId, String materialName,
                       BigDecimal quantity, String unit, Long unitCost,
                       Integer changeType, String sourceRef);

    /**
     * 扣减门店库存（销售出库、领料出库等场景）
     * 库存不足抛出BusinessException
     * 按当前单位成本结转出库成本
     *
     * @param storeId 门店ID
     * @param materialId 物料ID
     * @param quantity 扣减数量
     * @return 出库总成本（分）
     */
    Long decreaseStock(String storeId, Long materialId, BigDecimal quantity);

    /**
     * 扣减门店库存（携带变动类型与来源备注，写入库存流水）
     *
     * @param changeType 变动类型（1:入库,2:出库,3:调拨,4:盘点,5:损耗）
     * @param sourceRef  来源备注（如 订单号 / 调拨单号 / 退货单号）
     * @return 出库总成本（分）
     */
    Long decreaseStock(String storeId, Long materialId, BigDecimal quantity,
                       Integer changeType, String sourceRef);

    /**
     * 获取低库存列表（当前库存低于安全库存预警线）
     *
     * @param storeId 门店ID（可选，为空则查所有门店）
     * @return 低库存列表
     */
    List<StoreInventory> getLowStockList(String storeId);
}
