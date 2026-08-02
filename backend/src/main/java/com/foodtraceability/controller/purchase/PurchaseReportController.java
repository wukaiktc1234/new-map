package com.foodtraceability.controller.purchase;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;
import com.foodtraceability.service.purchase.PurchaseReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 采购报表管理控制器
 *
 * <p>提供 5 个 RESTful 端点：
 * <ul>
 *   <li>GET /v1/purchase/reports/summary         报表汇总数据</li>
 *   <li>GET /v1/purchase/reports/monthly-trend    月度趋势数据</li>
 *   <li>GET /v1/purchase/reports/supplier          供应商维度数据</li>
 *   <li>GET /v1/purchase/reports/category          分类维度数据</li>
 *   <li>GET /v1/purchase/reports/export            导出 CSV 报表</li>
 * </ul>
 * </p>
 *
 * <p>所有金额字段以"分"为单位返回（Long），由前端 DataConverter 转换为"元"。</p>
 */
@Tag(name = "采购报表", description = "采购报表汇总、趋势、供应商、分类及导出接口")
@RestController
@RequestMapping("/v1/purchase/reports")
public class PurchaseReportController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseReportController.class);

    /** CSV 文件 UTF-8 BOM 头，确保 Excel 正确识别中文 */
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private final PurchaseReportService reportService;

    public PurchaseReportController(PurchaseReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 获取报表汇总数据
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @param supplierId 供应商ID（可选）
     * @param category 物资分类名称（可选）
     * @return 汇总数据
     */
    @Operation(summary = "获取报表汇总数据", description = "返回采购总金额、订单总数、已结算金额、供应商数、平均订单金额、准时交付率、合格率等汇总指标")
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseReportSummaryVO> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String category) {
        try {
            PurchaseReportSummaryVO summary = reportService.getSummary(startDate, endDate, supplierId, category);
            return Result.success(summary, "获取报表汇总成功");
        } catch (Exception e) {
            log.error("获取报表汇总数据失败: startDate={}, endDate={}, supplierId={}, category={}",
                    startDate, endDate, supplierId, category, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取报表汇总失败");
        }
    }

    /**
     * 获取月度趋势数据
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param supplierId 供应商ID（可选）
     * @param category 物资分类名称（可选）
     * @return 月度趋势数据列表
     */
    @Operation(summary = "获取月度趋势数据", description = "按月份返回采购金额、订单数、已结算金额、供应商数等趋势数据（按月份升序）")
    @GetMapping("/monthly-trend")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportMonthlyItemVO>> getMonthlyTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String category) {
        try {
            List<PurchaseReportMonthlyItemVO> data = reportService.getMonthlyTrend(startDate, endDate, supplierId, category);
            return Result.success(data, "获取月度趋势数据成功");
        } catch (Exception e) {
            log.error("获取月度趋势数据失败: startDate={}, endDate={}, supplierId={}, category={}",
                    startDate, endDate, supplierId, category, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取月度趋势数据失败");
        }
    }

    /**
     * 获取供应商维度数据
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param supplierId 供应商ID（可选）
     * @param category 物资分类名称（可选）
     * @return 供应商维度数据列表
     */
    @Operation(summary = "获取供应商维度数据", description = "按供应商维度返回采购金额、订单数、准时交付率、合格率等数据（按金额降序）")
    @GetMapping("/supplier")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportSupplierItemVO>> getSupplierReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String category) {
        try {
            List<PurchaseReportSupplierItemVO> data = reportService.getSupplierReport(startDate, endDate, supplierId, category);
            return Result.success(data, "获取供应商维度数据成功");
        } catch (Exception e) {
            log.error("获取供应商维度数据失败: startDate={}, endDate={}, supplierId={}, category={}",
                    startDate, endDate, supplierId, category, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取供应商维度数据失败");
        }
    }

    /**
     * 获取分类维度数据
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param supplierId 供应商ID（可选）
     * @param category 物资分类名称（可选）
     * @return 分类维度数据列表
     */
    @Operation(summary = "获取分类维度数据", description = "按物资分类维度返回采购金额、数量、订单数等数据（按金额降序）")
    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportCategoryItemVO>> getCategoryReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String category) {
        try {
            List<PurchaseReportCategoryItemVO> data = reportService.getCategoryReport(startDate, endDate, supplierId, category);
            return Result.success(data, "获取分类维度数据成功");
        } catch (Exception e) {
            log.error("获取分类维度数据失败: startDate={}, endDate={}, supplierId={}, category={}",
                    startDate, endDate, supplierId, category, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取分类维度数据失败");
        }
    }

    /**
     * 导出报表
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param supplierId 供应商ID（可选）
     * @param category 物资分类名称（可选）
     * @param format 导出格式（默认 csv，目前仅支持 csv）
     * @return CSV 文件字节数组（UTF-8 BOM 头，Excel 兼容）
     */
    @Operation(summary = "导出采购报表", description = "按指定格式导出采购报表，返回 CSV 文件字节数组（含 UTF-8 BOM 头，Excel 兼容）")
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public Result<byte[]> exportReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            String csv = reportService.exportReportAsCsv(startDate, endDate, supplierId, category);
            // 拼接 UTF-8 BOM 头，确保 Excel 正确识别中文
            byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
            byte[] result = new byte[UTF8_BOM.length + csvBytes.length];
            System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
            System.arraycopy(csvBytes, 0, result, UTF8_BOM.length, csvBytes.length);
            return Result.success(result, "导出成功");
        } catch (Exception e) {
            log.error("导出报表失败: startDate={}, endDate={}, supplierId={}, category={}, format={}",
                    startDate, endDate, supplierId, category, format, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "导出失败");
        }
    }
}
