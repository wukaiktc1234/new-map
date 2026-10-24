package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.FixtureReverseRequest;
import com.foodtraceability.dto.finance.recon.FixtureReverseVO;
import com.foodtraceability.service.finance.ops.FinanceOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务运维端点（**仅供测试夹具的余额抹平使用，业务上不存在此场景**）。
 *
 * <p>为什么需要它：P2 自测补记过 2 条夹具流水（FF202610250010/011，各 100 分），
 * 事后把流水置 status=3 作废，但当时余额已经加上去了 —— 于是「流水侧少 200 分、账户余额多 200 分」
 * 两边同时错，V3 反而假 PASS。运维需要用一条反向流水把这 200 分真正抹平。</p>
 *
 * <p>安全边界：只接受 status=3 且 remark 含 [testFixture 的流水；普通业务流水一律 400；
 * 余额只走 AtomicBalanceUpdater（乐观锁）；写 finance_audit_logs 留痕。</p>
 */
@Tag(name = "财务-运维", description = "测试夹具余额抹平（非业务功能）")
@RestController
@RequestMapping("/v1/finance/ops")
public class FinanceOpsController {

    private final FinanceOpsService financeOpsService;

    public FinanceOpsController(FinanceOpsService financeOpsService) {
        this.financeOpsService = financeOpsService;
    }

    @Operation(summary = "抹平测试夹具余额（仅 testFixture 流水，重复调用 400）")
    @PostMapping("/fixture-reverse")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<FixtureReverseVO> fixtureReverse(@RequestBody FixtureReverseRequest req) {
        return Result.success(financeOpsService.reverseFixtures(req));
    }
}
