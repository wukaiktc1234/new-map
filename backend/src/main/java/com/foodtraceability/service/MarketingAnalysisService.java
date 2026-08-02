package com.foodtraceability.service;

import com.foodtraceability.dto.marketing.MarketingStatisticsVO;

import java.util.List;
import java.util.Map;

/**
 * 营销分析服务接口
 * 提供RFM客户分群、流失预警、营销效果分析等功能
 */
public interface MarketingAnalysisService {

    /**
     * 执行RFM模型计算（定时任务调用）
     * 为所有活跃会员计算R/F/M得分并更新客户分层
     *
     * @return 计算结果统计
     */
    Map<String, Object> calculateRFM();

    /**
     * 获取营销统计概览（仪表盘数据）
     */
    MarketingStatisticsVO getStatisticsOverview();

    /**
     * 获取流失风险会员列表
     * @param days 未消费天数阈值
     * @param limit 返回数量限制
     */
    List<Map<String, Object>> getChurnRiskMembers(int days, int limit);

    /**
     * 获取高价值会员列表
     * @param limit 返回数量限制
     */
    List<Map<String, Object>> getHighValueMembers(int limit);

    /**
     * 获取RFM分布统计
     */
    Map<String, Long> getRFMDistribution();

    /**
     * 获取客户生命周期价值(CLV)排名
     * @param limit 返回数量限制
     */
    List<Map<String, Object>> getCLVRanking(int limit);

    /**
     * 获取近N天趋势数据
     * @param days 天数
     */
    Map<String, Object> getTrendData(int days);

    /**
     * 流失预警处理（发送提醒/优惠券）
     * @param days 未消费天数阈值
     * @return 处理的会员数量
     */
    int processChurnWarning(int days);
}
