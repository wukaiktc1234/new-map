package com.foodtraceability.service;

import java.util.List;
import java.util.Map;

/**
 * 库存统计服务接口
 * 定义库存统计相关的业务方法
 */
public interface InventoryStatsService {

    /**
     * 获取库存统计概览
     * @return 库存统计概览数据
     */
    Map<String, Object> getStatsOverview();

    /**
     * 获取库存趋势统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param type 统计类型: day, week, month
     * @return 库存趋势数据
     */
    List<Map<String, Object>> getStatsTrend(String startTime, String endTime, String type);

    /**
     * 获取库存分类统计
     * @param warehouseId 仓库ID
     * @return 库存分类统计数据
     */
    List<Map<String, Object>> getStatsCategory(Long warehouseId);
    
    /**
     * 获取消耗趋势统计
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param type 统计类型: day, week, month
     * @return 消耗趋势数据
     */
    List<Map<String, Object>> getConsumptionTrend(String startTime, String endTime, String type);
    
    /**
     * 获取库存同比/环比数据
     * @return 同比/环比数据
     */
    Map<String, Object> getStatsComparison();
    
    /**
     * 获取预警历史趋势
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param type 统计类型: day, week, month
     * @return 预警历史趋势数据
     */
    List<Map<String, Object>> getWarningHistoryTrend(String startTime, String endTime, String type);
    
    /**
     * 获取预警级别分布
     * @return 预警级别分布数据
     */
    Map<String, Object> getWarningLevelDistribution();
    
    /**
     * 获取预警状态分布
     * @return 预警状态分布数据
     */
    Map<String, Object> getWarningStatusDistribution();
}