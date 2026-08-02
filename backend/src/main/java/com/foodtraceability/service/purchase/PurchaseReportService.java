package com.foodtraceability.service.purchase;

import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;

import java.util.List;

/**
 * 采购报表服务接口
 *
 * <p>提供采购数据的汇总、月度趋势、供应商维度、分类维度等聚合查询能力。
 * 所有金额字段以"分"为单位（Long），由前端 DataConverter 转换为"元"。</p>
 *
 * <p>数据来源：
 * <ul>
 *   <li>purchase_orders：订单汇总、月度趋势、供应商维度</li>
 *   <li>purchase_order_items + material_archives + material_categories：分类维度</li>
 *   <li>purchase_stockins：准时交付率、质检合格率</li>
 *   <li>suppliers：供应商名称查询</li>
 * </ul>
 * </p>
 */
public interface PurchaseReportService {

    /**
     * 获取报表汇总数据
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @param supplierId 供应商ID（可为空，表示全部供应商）
     * @param category 物资分类名称（可为空，表示全部分类）
     * @return 汇总数据
     */
    PurchaseReportSummaryVO getSummary(String startDate, String endDate,
                                        String supplierId, String category);

    /**
     * 获取月度趋势数据
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @param supplierId 供应商ID（可为空）
     * @param category 物资分类名称（可为空）
     * @return 月度趋势数据列表（按月份升序）
     */
    List<PurchaseReportMonthlyItemVO> getMonthlyTrend(String startDate, String endDate,
                                                       String supplierId, String category);

    /**
     * 获取供应商维度数据
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @param supplierId 供应商ID（可为空）
     * @param category 物资分类名称（可为空）
     * @return 供应商维度数据列表（按采购总额降序）
     */
    List<PurchaseReportSupplierItemVO> getSupplierReport(String startDate, String endDate,
                                                          String supplierId, String category);

    /**
     * 获取分类维度数据
     * @param startDate 开始日期（yyyy-MM-dd，可为空）
     * @param endDate 结束日期（yyyy-MM-dd，可为空）
     * @param supplierId 供应商ID（可为空）
     * @param category 物资分类名称（可为空）
     * @return 分类维度数据列表（按采购总额降序）
     */
    List<PurchaseReportCategoryItemVO> getCategoryReport(String startDate, String endDate,
                                                          String supplierId, String category);

    /**
     * 导出报表为 CSV 字符串
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param supplierId 供应商ID
     * @param category 物资分类名称
     * @return CSV 字符串内容（含 BOM 头）
     */
    String exportReportAsCsv(String startDate, String endDate,
                              String supplierId, String category);
}
