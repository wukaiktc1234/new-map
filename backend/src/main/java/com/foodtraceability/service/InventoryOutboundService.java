package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.InventoryOutboundCreateDTO;
import com.foodtraceability.entity.InventoryOutbound;

import java.util.List;
import java.util.Map;

/**
 * 库存出库单服务接口
 * 定义库存出库管理相关的业务方法
 */
public interface InventoryOutboundService {

    /**
     * 分页查询库存出库单列表
     *
     * @param page 分页对象
     * @param outboundType 出库类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    IPage<Map<String, Object>> getOutboundPage(Page<InventoryOutbound> page,
                                                String outboundType,
                                                String status,
                                                String warehouseId,
                                                String keyword,
                                                String startDate,
                                                String endDate);

    /**
     * 根据出库单号获取出库单详情（含明细）
     *
     * @param outboundCode 出库单号
     * @return 出库单详情（含明细列表）
     */
    Map<String, Object> getOutboundByCode(String outboundCode);

    /**
     * 创建库存出库单
     *
     * @param outbound 出库单实体
     * @param items 出库单明细列表
     * @return 创建后的出库单
     */
    InventoryOutbound createOutbound(InventoryOutbound outbound, List<Map<String, Object>> items);

    /**
     * 更新库存出库单（仅草稿状态可更新）
     * 替换明细列表并重算总数量
     *
     * @param outboundCode 出库单号
     * @param updateDTO 更新请求DTO
     * @return 更新后的出库单
     */
    InventoryOutbound updateOutbound(String outboundCode, InventoryOutboundCreateDTO updateDTO);

    /**
     * 删除库存出库单（仅草稿状态可删除，逻辑删除）
     *
     * @param outboundCode 出库单号
     */
    void deleteOutbound(String outboundCode);

    /**
     * 审批库存出库单
     *
     * @param outboundCode 出库单号
     * @param approved 是否通过
     * @param opinion 审批意见
     * @return 更新后的出库单
     */
    InventoryOutbound approveOutbound(String outboundCode, Boolean approved, String opinion);

    /**
     * 执行出库（确认出库）
     *
     * @param outboundCode 出库单号
     * @return 更新后的出库单
     */
    InventoryOutbound executeOutbound(String outboundCode);
}
