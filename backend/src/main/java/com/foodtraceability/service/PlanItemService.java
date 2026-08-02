package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.PlanItemRequest;
import com.foodtraceability.entity.PlanItem;

import java.util.List;
import java.util.Map;

/**
 * 计划项Service接口
 * 
 * @author demo
 * @since 1.0.0
 */
public interface PlanItemService extends IService<PlanItem> {

    /**
     * 创建计划项
     * 
     * @param request 计划项请求DTO
     * @return 创建的计划项
     */
    PlanItem createPlanItem(PlanItemRequest request);

    /**
     * 更新计划项
     * 
     * @param id      计划项ID
     * @param request 计划项请求DTO
     * @return 更新后的计划项
     */
    PlanItem updatePlanItem(Long id, PlanItemRequest request);

    /**
     * 删除计划项
     * 
     * @param id 计划项ID
     * @return 是否删除成功
     */
    boolean deletePlanItem(Long id);

    /**
     * 获取计划项详情
     * 
     * @param id 计划项ID
     * @return 计划项详情
     */
    PlanItem getPlanItemById(Long id);

    /**
     * 查询计划项列表
     * 
     * @param params 查询参数
     * @return 计划项列表
     */
    List<PlanItem> listPlanItems(Map<String, Object> params);

    /**
     * 统计计划项数量
     * 
     * @param params 统计参数
     * @return 统计结果
     */
    Map<String, Long> countPlanItems(Map<String, Object> params);

    /**
     * 批量更新计划项状态
     * 
     * @param ids    计划项ID列表
     * @param status 目标状态
     * @return 更新成功数量
     */
    int batchUpdateStatus(List<Long> ids, String status);
}
