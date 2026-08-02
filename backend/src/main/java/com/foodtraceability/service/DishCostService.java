package com.foodtraceability.service;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.product.CostAlertRuleVO;
import com.foodtraceability.dto.product.CostSummaryVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import com.foodtraceability.dto.product.DishCostQueryDTO;
import com.foodtraceability.dto.product.DishCostSnapshotVO;

import java.util.List;
import java.util.Map;

/**
 * 菜品成本服务接口
 * 提供菜品成本快照、成本趋势、BOM 明细、预警规则、汇总统计等功能
 */
public interface DishCostService {

    /**
     * 获取菜品成本快照
     * @param dishId 菜品ID
     * @return 成本快照
     */
    DishCostSnapshotVO getDishCost(String dishId);

    /**
     * 分页查询菜品成本列表
     * @param queryDto 查询条件
     * @return 分页结果
     */
    PageResult<DishCostSnapshotVO> getDishCostList(DishCostQueryDTO queryDto);

    /**
     * 重新计算所有菜品成本
     * @return 重算任务信息：taskId、totalDishes、estimatedSeconds
     */
    Map<String, Object> recalculateAll();

    /**
     * 获取菜品成本趋势
     * @param dishId 菜品ID
     * @param days 天数
     * @return 趋势数据列表
     */
    List<CostTrendDataVO> getCostTrend(String dishId, Integer days);

    /**
     * 获取菜品 BOM 明细成本
     * @param dishId 菜品ID
     * @return 含 BOM 明细的成本快照
     */
    DishCostSnapshotVO getBomDetail(String dishId);

    /**
     * 更新 BOM 项数量
     * @param dishId 菜品ID
     * @param materialId 物料ID
     * @param quantity 数量
     */
    void updateBomQuantity(String dishId, String materialId, Double quantity);

    /**
     * 获取成本预警规则列表
     * @return 预警规则列表
     */
    List<CostAlertRuleVO> getAlertRules();

    /**
     * 保存成本预警规则
     * @param rule 预警规则
     * @return 保存后的规则
     */
    CostAlertRuleVO saveAlertRule(CostAlertRuleVO rule);

    /**
     * 切换预警规则启用状态
     * @param id 规则ID
     * @param enabled 是否启用
     */
    void toggleAlertRule(String id, Boolean enabled);

    /**
     * 获取成本汇总统计
     * @param startDate 开始日期（可空，格式 yyyy-MM-dd）
     * @param endDate 结束日期（可空，格式 yyyy-MM-dd）
     * @return 汇总统计
     */
    CostSummaryVO getCostSummary(String startDate, String endDate);

    /**
     * 导出成本报表
     * @param category 分类筛选
     * @param status 状态筛选
     * @return 报表字节数组
     */
    byte[] exportCostReport(String category, String status);
}
