package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.finance.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 利润分析/财务报表Controller
 */
@Tag(name = "利润分析", description = "利润表生成和经营指标分析")
@RestController
@RequestMapping("/v1/finance/profits")
public class ProfitController {

    private final FinancialReportService financialReportService;

    public ProfitController(FinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    @Operation(summary = "获取月度利润表")
    @GetMapping("/monthly/{year}/{month}")
    public Result<Map<String, Object>> getMonthly(@PathVariable int year, @PathVariable int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        return Result.success(financialReportService.getProfitStatement(startDate, endDate));
    }

    @Operation(summary = "获取季度利润表")
    @GetMapping("/quarterly/{year}/{quarter}")
    public Result<Map<String, Object>> getQuarterly(@PathVariable int year, @PathVariable int quarter) {
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate startDate = LocalDate.of(year, startMonth, 1);
        LocalDate endDate = startDate.plusMonths(3).minusDays(1);
        return Result.success(financialReportService.getProfitStatement(startDate, endDate));
    }

    @Operation(summary = "手动触发生成利润表", description = "根据收支记录和成本数据重新计算并生成报表")
    @PostMapping("/generate/{year}/{month}")
    public Result<Map<String, Object>> generate(@PathVariable int year,
                                                   @RequestParam(defaultValue = "0") int month) {
        if (month > 0 && month <= 12) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            return Result.success(financialReportService.getProfitStatement(startDate, endDate));
        }
        // 年度
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return Result.success(financialReportService.getProfitStatement(startDate, endDate));
    }
}
