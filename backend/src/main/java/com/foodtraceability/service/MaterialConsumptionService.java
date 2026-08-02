package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.MaterialConsumption;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 原料消耗服务接口
 */
public interface MaterialConsumptionService extends IService<MaterialConsumption> {

    /**
     * 记录原料消耗
     */
    MaterialConsumption record(MaterialScanConsumeDTO dto);

    /**
     * 根据后厨订单ID查询
     */
    List<MaterialConsumption> getByKitchenOrderId(String kitchenOrderId);

    /**
     * 根据订单ID查询
     */
    List<MaterialConsumption> getByOrderId(String orderId);

    /**
     * 计算订单原料成本
     */
    BigDecimal calculateOrderCost(String orderId);

    /**
     * 计算门店时间范围原料成本
     */
    BigDecimal calculateStoreCost(Long storeId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 扣减库存
     */
    boolean deductInventory(String consumptionId);

    /**
     * 批量扣减库存
     */
    boolean batchDeductInventory(List<String> consumptionIds);

    /**
     * 获取未扣减库存的记录
     */
    List<MaterialConsumption> getUndeductedRecords();
}
