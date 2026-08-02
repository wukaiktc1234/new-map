package com.foodtraceability.service;

import com.foodtraceability.dto.DashboardConfigVO;
import com.foodtraceability.dto.DashboardConfigQueryDTO;
import com.foodtraceability.dto.DashboardConfigCreateDTO;
import com.foodtraceability.dto.PageResult;
import java.util.Map;

/**
 * 看板服务接口
 * 提供经营看板的数据聚合和配置管理功能
 */
public interface DashboardService {

    /**
     * 获取看板概览数据
     * @param dashboardType 看板类型
     * @return 看板概览数据
     */
    Map<String, Object> getDashboardOverview(Integer dashboardType);

    /**
     * 获取今日销售概览
     * @return 今日销售数据
     */
    Map<String, Object> getTodaySalesOverview();

    /**
     * 获取库存预警信息
     * @return 库存预警列表
     */
    Map<String, Object> getInventoryAlerts();

    /**
     * 获取会员增长数据
     * @param days 最近天数
     * @return 会员增长趋势
     */
    Map<String, Object> getMemberGrowthTrend(Integer days);

    /**
     * 获取财务概况
     * @return 财务关键指标
     */
    Map<String, Object> getFinanceSummary();

    /**
     * 保存看板配置
     * @param dto 配置信息
     * @return 配置ID
     */
    Long saveDashboardConfig(DashboardConfigCreateDTO dto);

    /**
     * 获取用户看板配置
     * @param userId 用户ID
     * @param dashboardType 看板类型
     * @return 配置信息
     */
    DashboardConfigVO getUserDashboardConfig(Long userId, Integer dashboardType);

    /**
     * 分页查询看板配置
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<DashboardConfigVO> queryDashboardConfigs(DashboardConfigQueryDTO queryDTO);

    /**
     * 删除看板配置
     * @param configId 配置ID
     * @param userId 用户ID（权限校验）
     * @return 是否成功
     */
    boolean deleteDashboardConfig(Long configId, Long userId);
}
