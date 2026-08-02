package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.FinanceStatisticsQueryDTO;
import com.foodtraceability.dto.finance.FinanceStatisticsVO;
import com.foodtraceability.service.finance.FinanceStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务统计Controller
 *
 * <p>Sprint 3.1 P0 F-002/F-017：财务中心首页统计概览。</p>
 *
 * <p>路径前缀：/v1/finance/statistics（F-003 不变更此路径）</p>
 *
 * <p>真实化：移除 BigDecimal("0.00") mock 数据，对接 FinanceStatisticsService
 * 返回真实统计数据，金额字段全部 Long（分）。</p>
 */
@Tag(name = "财务统计", description = "财务中心首页统计概览，含当月收入/支出/利润及环比变化率")
@RestController
@RequestMapping("/v1/finance/statistics")
public class FinanceStatisticsController {

    private final FinanceStatisticsService financeStatisticsService;

    public FinanceStatisticsController(FinanceStatisticsService financeStatisticsService) {
        this.financeStatisticsService = financeStatisticsService;
    }

    /**
     * 获取财务统计概览数据
     *
     * <p>返回当月收入/支出/利润（Long，分）+ 环比变化率（Double）+ 上月数据 + period。</p>
     *
     * @param query 查询条件（含 startDate/endDate/period）
     * @return 财务统计VO
     */
    @Operation(summary = "获取财务统计概览",
            description = "聚合 Receivable/Payable/FinanceVoucher/FundFlow 数据，计算当月收入/支出/利润及环比变化率")
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('finance:statistics:view')")
    public Result<FinanceStatisticsVO> getStatisticsOverview(FinanceStatisticsQueryDTO query) {
        return Result.success(financeStatisticsService.getOverview(query));
    }
}
