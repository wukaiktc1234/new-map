package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.finance.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 财务报表控制器
 * 提供利润表、收支明细、应收应付统计等报表数据
 */
@Tag(name = "财务报表", description = "利润表、收支明细、账龄分析、成本结构等财务报表")
@RestController
@RequestMapping("/v1/finance/reports")
public class ReportController {

    private final FinancialReportService reportService;

    public ReportController(FinancialReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "利润表数据", description = "获取指定期间的利润表数据")
    @GetMapping("/profit-statement")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getProfitStatement(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportService.getProfitStatement(startDate, endDate));
    }

    @Operation(summary = "收支明细汇总", description = "获取指定期间的收入支出汇总数据")
    @GetMapping("/income-expense-summary")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getIncomeExpenseSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportService.getIncomeExpenseSummary(startDate, endDate));
    }

    @Operation(summary = "应收账款统计", description = "获取应收账款总额、已收金额、余额等统计数据")
    @GetMapping("/receivable-statistics")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getReceivableStatistics() {
        return Result.success(reportService.getReceivableStatistics());
    }

    @Operation(summary = "应付账款统计", description = "获取应付账款总额、已付金额、余额等统计数据")
    @GetMapping("/payable-statistics")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getPayableStatistics() {
        return Result.success(reportService.getPayableStatistics());
    }

    @Operation(summary = "账龄分析", description = "获取各账龄段的应收账款分布情况")
    @GetMapping("/aging-analysis")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getAgingAnalysis() {
        return Result.success(reportService.getAgingAnalysis());
    }

    @Operation(summary = "成本结构分析", description = "获取指定期间各成本类型的占比情况")
    @GetMapping("/cost-structure")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getCostStructure(@RequestParam String period) {
        return Result.success(reportService.getCostStructure(period));
    }

    @Operation(summary = "预算执行情况", description = "获取指定年份和类型的预算vs实际对比数据")
    @GetMapping("/budget-execution")
    @PreAuthorize("hasAuthority('finance:report:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getBudgetExecution(
            @RequestParam Integer year,
            @RequestParam(required = false) Integer type) {
        return Result.success(reportService.getBudgetExecution(year, type));
    }
}
