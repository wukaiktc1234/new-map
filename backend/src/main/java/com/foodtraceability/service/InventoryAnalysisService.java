package com.foodtraceability.service;

import com.foodtraceability.dto.InventoryAnalysisVO;
import com.foodtraceability.dto.InventoryAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import java.util.List;
import java.util.Map;

/**
 * 库存分析服务接口
 * 提供库存数据的分析和报表生成功能
 */
public interface InventoryAnalysisService {

    /**
     * 获取库存概览
     * @return 库存概览数据（总量/总值/缺货数/预警数）
     */
    Map<String, Object> getInventoryOverview();

    /**
     * 生成库存分析报表
     * @param date 日期
     * @return 库存分析报表
     */
    InventoryAnalysisVO generateInventoryReport(java.time.LocalDate date);

    /**
     * 获取周转率分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 周转率分析数据
     */
    Map<String, Object> getInventoryTurnoverAnalysis(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取ABC分类
     * @return ABC分类数据（A高周转/B中等/C滞销）
     */
    List<Map<String, Object>> getABCClassification();

    /**
     * 获取报损分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报损分析数据（按原因/品类/时间）
     */
    Map<String, Object> getWasteAnalysis(java.time.LocalDate startDate, java.time.LocalDate endDate);

    /**
     * 获取缺货风险列表
     * @return 缺货风险物料列表（低于安全库存的）
     */
    List<Map<String, Object>> getStockoutRiskList();

    /**
     * 获取各仓库库存价值分布
     * @return 各仓库库存价值
     */
    List<Map<String, Object>> getInventoryValueByWarehouse();

    /**
     * 分页查询库存报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<InventoryAnalysisVO> queryInventoryReports(InventoryAnalysisQueryDTO queryDTO);

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    InventoryAnalysisVO getReportById(Long reportId);
}
