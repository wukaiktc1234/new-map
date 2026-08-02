package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.marketing.RechargeStatsOverviewVO;

/**
 * 储值统计服务接口
 * 提供储值仪表盘所需的聚合统计数据
 */
public interface RechargeStatsService {

    /**
     * 获取储值统计概览
     * 包含：余额统计、本月统计、今日统计、方案使用分布、近7天趋势、预警数据
     *
     * @return 统计概览VO
     */
    RechargeStatsOverviewVO getOverview();
}
