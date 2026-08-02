package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Map;

/**
 * 运营数据中心服务接口
 * 提供公司级数据分析与监控功能
 * 支持多店监控统计、门店绩效分析等运营决策支持
 */
public interface OperationsDashboardService {

    /**
     * 获取多店监控统计数据卡片
     * 权限限制：仅operations_director和regional_manager可访问
     * 区域经理只能查看辖区内的门店数据
     *
     * @param principal 当前用户身份信息
     * @return 统计数据Map（activeStores/todayRevenue/todayOrderCount/onDutyStaff等）
     */
    Map<String, Object> getDashboardStats(java.security.Principal principal);

    /**
     * 获取门店绩效列表
     * 支持按区域、日期范围筛选
     *
     * @param page      页码
     * @param size      每页大小
     * @param principal 当前用户身份信息（用于权限过滤）
     * @return 门店绩效分页数据
     */
    IPage<Map<String, Object>> getStorePerformanceList(Integer page, Integer size,
                                                         java.security.Principal principal);
}
