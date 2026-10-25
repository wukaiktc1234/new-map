package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.FinanceOverviewDailyVO;
import com.foodtraceability.service.finance.FinanceOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 资金总览（日）控制器 —— P3-4，**只读**。
 *
 * <p>范围由服务端按 data_scope 强制（all/company 全量；store/department 仅本店；
 * 无归属门店 403），前端不传任何范围参数。</p>
 */
@Tag(name = "财务-资金总览", description = "资金总览（日）三段只读聚合：门店现金户 / 渠道 / 调节表")
@RestController
@RequestMapping("/v1/finance/overview")
public class FinanceOverviewController {

    private final FinanceOverviewService financeOverviewService;

    public FinanceOverviewController(FinanceOverviewService financeOverviewService) {
        this.financeOverviewService = financeOverviewService;
    }

    @Operation(summary = "资金总览（date 缺省=今天；范围由 data_scope 决定）")
    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<FinanceOverviewDailyVO> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(financeOverviewService.daily(date));
    }
}
