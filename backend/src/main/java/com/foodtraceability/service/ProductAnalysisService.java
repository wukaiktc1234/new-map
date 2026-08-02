package com.foodtraceability.service;

import com.foodtraceability.dto.ProductAnalysisVO;
import com.foodtraceability.dto.ProductAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 产品分析服务接口
 * 提供产品（菜品/套餐）数据的分析和报表生成功能
 */
public interface ProductAnalysisService {

    /**
     * 获取菜品盈利能力分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 盈利能力分析数据（毛利=售价-成本）
     */
    Map<String, Object> getProductProfitabilityAnalysis(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 生成产品分析报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 产品分析报表
     */
    ProductAnalysisVO generateProductReport(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取菜单工程数据
     * @return 菜单工程数据（明星/金牛/问题/瘦狗分类）
     */
    Map<String, Object> getMenuEngineeringData();

    /**
     * 获取套餐表现分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 套餐表现数据
     */
    List<Map<String, Object>> getComboPerformance(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取价格弹性分析
     * @param foodId 菜品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格弹性数据（调价对销量的影响）
     */
    Map<String, Object> getPriceElasticity(Long foodId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取客户口味偏好
     * @return 客户口味偏好数据（辣度/口味/菜系）
     */
    Map<String, Object> getCustomerPreferenceAnalysis();

    /**
     * 分页查询产品报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<ProductAnalysisVO> queryProductReports(ProductAnalysisQueryDTO queryDTO);

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    ProductAnalysisVO getReportById(Long reportId);
}
