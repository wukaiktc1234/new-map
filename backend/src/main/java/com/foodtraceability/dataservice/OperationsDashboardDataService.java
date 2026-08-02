package com.foodtraceability.dataservice;

import java.util.List;
import java.util.Map;

/**
 * 运营数据中心数据服务接口
 * 提供门店运营统计数据的缓存和聚合查询功能
 * 用于运营Dashboard的多店监控、绩效分析等场景
 * 缓存键格式: operations_dashboard:stats:{storeId}:{date}
 */
public interface OperationsDashboardDataService {

    /**
     * 获取多店监控统计数据
     * 包含活跃门店数、今日营收、订单数、在岗人数等核心指标
     *
     * @param storeIds 门店ID列表（区域经理传入辖区门店，总部传null表示全部）
     * @param date     统计日期（yyyy-MM-dd格式），传null表示今日
     * @return 统计指标Map
     */
    Map<String, Object> getDashboardStats(List<String> storeIds, String date);

    /**
     * 获取单店绩效数据
     *
     * @param storeId 门店ID
     * @param date    统计日期
     * @return 单店绩效数据Map
     */
    Map<String, Object> getStorePerformance(String storeId, String date);

    /**
     * 清除指定门店的运营数据缓存
     *
     * @param storeId 门店ID
     */
    void clearStoreOperationsCache(String storeId);
}
