package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.CashSalesBackfillRequest;
import com.foodtraceability.dto.finance.recon.CashSalesBackfillVO;
import com.foodtraceability.dto.finance.recon.ReverseExecutionVO;
import com.foodtraceability.service.finance.backfill.CashSalesBackfillService;
import com.foodtraceability.service.finance.recon.ReconReverseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P2-B 两个真正写账的端点（差异冲正执行 / 现金销售补记）。
 *
 * <p><b>红线圈定</b>：只写 fund_flows + 通过乐观锁改 bank_accounts.balance（balance + delta, version + 1），
 * 不建表、不改结构、不物理删除、不改历史金额。</p>
 */
@Tag(name = "财务-P2 冲正与补记", description = "差异 REVERSE 执行；现金销售 SALE 补记")
@RestController
@RequestMapping("/v1/finance")
public class ReconReverseController {

    private final ReconReverseService reconReverseService;
    private final CashSalesBackfillService cashSalesBackfillService;

    public ReconReverseController(ReconReverseService reconReverseService,
                                  CashSalesBackfillService cashSalesBackfillService) {
        this.reconReverseService = reconReverseService;
        this.cashSalesBackfillService = cashSalesBackfillService;
    }

    /** 差异 REVERSE 执行（只增反向流水 + 原流水置 3 + 余额原子回滚） */
    @Operation(summary = "执行差异冲正（只增不删；幂等键 [reconReverseOf=原flow_no]）")
    @PostMapping("/recon-differences/{differenceId}/execute-reverse")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<ReverseExecutionVO> executeReverse(@PathVariable Long differenceId) {
        return Result.success(reconReverseService.executeReverse(differenceId));
    }

    /** 现金销售 SALE 补记（默认 dryRun） */
    @Operation(summary = "现金销售 SALE 流水补记（当天欠账；默认 dryRun 只返回清单）")
    @PostMapping("/backfill/cash-sales")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<CashSalesBackfillVO> backfill(@RequestBody CashSalesBackfillRequest req) {
        return Result.success(cashSalesBackfillService.backfill(req));
    }
}
