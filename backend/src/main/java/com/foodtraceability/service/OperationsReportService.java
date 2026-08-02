package com.foodtraceability.service;

import com.foodtraceability.dto.operations.KpiSummaryDTO;
import com.foodtraceability.dto.operations.OperationsReportQueryDTO;

import java.util.Map;

/**
 * 经营报表服务接口（管理层视角 - 精简版）
 * 提供统一报表查询、KPI汇总、导出等功能
 */
public interface OperationsReportService {

    /**
     * 统一报表数据查询
     * @param reportType 报表类型: daily/weekly/monthly/quarterly/yearly/profit
     * @param queryDTO 查询参数
     * @param userId 用户ID
     * @return 报表数据
     */
    Map<String, Object> getReportData(String reportType, OperationsReportQueryDTO queryDTO, Long userId);

    /**
     * 获取KPI汇总数据（首屏快速加载）
     * @param queryDTO 查询参数
     * @param userId 用户ID
     * @return KPI汇总数据
     */
    KpiSummaryDTO getKpiSummary(OperationsReportQueryDTO queryDTO, Long userId);

    /**
     * 创建导出任务
     * @param taskType 任务类型：1-Excel 2-PDF
     * @param reportType 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
     * @param queryDTO 查询参数
     * @param userId 用户ID
     * @return 导出任务信息
     */
    Map<String, Object> createExportTask(Integer taskType, Integer reportType,
                                         OperationsReportQueryDTO queryDTO, Long userId);

    /**
     * 获取导出任务状态
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务状态
     */
    Map<String, Object> getExportTaskStatus(Long taskId, Long userId);
}
