package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryAdjustCreateDTO;
import com.foodtraceability.entity.InventoryAdjust;

import java.util.List;
import java.util.Map;

/**
 * 库存调整单服务接口
 * 定义库存调整管理相关的业务方法
 */
public interface InventoryAdjustService {

    /**
     * 分页查询库存调整单列表
     *
     * @param page 分页对象
     * @param adjustType 调整类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    IPage<Map<String, Object>> getAdjustPage(Page<InventoryAdjust> page,
                                              String adjustType,
                                              String status,
                                              String warehouseId,
                                              String keyword,
                                              String startDate,
                                              String endDate);

    /**
     * 根据调整单号获取调整单详情（含明细）
     *
     * @param adjustCode 调整单号
     * @return 调整单详情（含明细列表）
     */
    Map<String, Object> getAdjustByCode(String adjustCode);

    /**
     * 创建库存调整单
     *
     * @param adjust 调整单实体
     * @param items 调整单明细列表
     * @return 创建后的调整单
     */
    InventoryAdjust createAdjust(InventoryAdjust adjust, List<Map<String, Object>> items);

    /**
     * 更新库存调整单（仅草稿状态可更新）
     * 替换明细列表并重算总数量和总金额
     *
     * @param adjustCode 调整单号
     * @param updateDTO 更新请求DTO
     * @return 更新后的调整单
     */
    InventoryAdjust updateAdjust(String adjustCode, InventoryAdjustCreateDTO updateDTO);

    /**
     * 删除库存调整单（仅草稿状态可删除，逻辑删除）
     *
     * @param adjustCode 调整单号
     */
    void deleteAdjust(String adjustCode);

    /**
     * 审批库存调整单
     *
     * @param adjustCode 调整单号
     * @param approved 是否通过
     * @param opinion 审批意见
     * @return 更新后的调整单
     */
    InventoryAdjust approveAdjust(String adjustCode, Boolean approved, String opinion);

    /**
     * 执行库存调整（完成调整）
     *
     * @param adjustCode 调整单号
     * @return 更新后的调整单
     */
    InventoryAdjust executeAdjust(String adjustCode);
}
