package com.foodtraceability.service;

import com.foodtraceability.dto.CustomerAnalysisVO;
import com.foodtraceability.dto.CustomerAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 客户分析服务接口
 * 提供客户（会员）数据的分析和报表生成功能
 */
public interface CustomerAnalysisService {

    /**
     * 获取会员增长趋势
     * @param days 最近天数
     * @return 会员增长趋势数据
     */
    Map<String, Object> getMemberGrowthTrend(Integer days);

    /**
     * 生成客户分析报表
     * @param period 统计周期
     * @return 客户分析报表
     */
    CustomerAnalysisVO generateCustomerReport(String period);

    /**
     * 获取留存队列分析
     * @return 留存队列数据（按注册月份分组看留存）
     */
    Map<String, Object> getRetentionCohortAnalysis();

    /**
     * 获取RFM分布快照
     * @return RFM分布数据
     */
    Map<String, Object> getRFMDistributionSnapshot();

    /**
     * 获取分群洞察
     * @param segmentType 分群类型（如"高价值流失风险"）
     * @return 分群洞察数据
     */
    Map<String, Object> getMemberSegmentInsight(String segmentType);

    /**
     * 获取CLV预测
     * @param months 未来月数
     * @return CLV预测数据（未来N个月的客户价值预测）
     */
    Map<String, Object> getCLVForecast(Integer months);

    /**
     * 分页查询客户报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<CustomerAnalysisVO> queryCustomerReports(CustomerAnalysisQueryDTO queryDTO);

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    CustomerAnalysisVO getReportById(Long reportId);
}
