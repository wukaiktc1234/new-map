package com.foodtraceability.service.purchase;

import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;

import java.util.List;

/**
 * 采购数据分析服务接口
 *
 * <p>提供采购数据的趋势、供应商占比、品类分布、综合汇总等分析维度。
 * 与 {@link PurchaseReportService} 相比，本接口聚焦于"分析视角"，
 * 不接受 supplierId/category 过滤参数，仅按日期范围聚合。</p>
 *
 * <p>复用 PurchaseReport 系列的 VO，避免重复定义数据结构。
 * 所有金额字段以"分"为单位（Long），由前端 DataConverter 转换为"元"。</p>
 */
public interface PurchaseAnalysisService {

    /**
     * 采购趋势分析（按月份聚合）
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @return 月份/采购金额/订单数量趋势数据列表（按月份升序）
     */
    List<PurchaseReportMonthlyItemVO> getTrend(String startDate, String endDate);

    /**
     * 供应商采购占比分析（按供应商聚合）
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @return 供应商/采购金额占比数据列表（按金额降序）
     */
    List<PurchaseReportSupplierItemVO> getSupplier(String startDate, String endDate);

    /**
     * 品类采购分布分析（按物资分类聚合）
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @return 品类/采购金额分布数据列表（按金额降序）
     */
    List<PurchaseReportCategoryItemVO> getCategory(String startDate, String endDate);

    /**
     * 综合汇总指标
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @return 总采购金额/订单数/平均订单金额/供应商数/品类数等汇总数据
     */
    PurchaseReportSummaryVO getSummary(String startDate, String endDate);
}
