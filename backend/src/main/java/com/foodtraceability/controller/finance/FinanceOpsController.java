package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.FixtureReverseRequest;
import com.foodtraceability.dto.finance.recon.FixtureReverseVO;
import com.foodtraceability.service.finance.ops.FinanceOpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
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
 *
 * <p><b>处置结论（P3 收口，Owner 选 (b)：保留 + 双锁）</b>：该端点业务上不存在，但
 * 测试/迁移期还可能再遇到夹具流水；直接删掉会迫使运维手写反向流水（易错、且会绕过乐观锁）。
 * 因此<b>保留但加双锁</b>：① {@code @Profile("ops")} —— 生产 profile 下该 Bean 根本不存在，
 * 路由为 404，误用风险为 0；② 权限收紧为**仅 FINANCE_DIRECTOR**（hasAnyAuthority('ROLE_FINANCE_DIRECTOR')，
 * 不再用 finance:bank:reconcile —— 该权限码出纳/财务经理都有）。</p>
 *
 * <p><b>启用方式</b>：{@code --spring.profiles.active=pg,ops}（或环境变量 SPRING_PROFILES_ACTIVE=pg,ops）。
 * <b>生产不启用</b>：prod profile 不包含 ops。</p>
 */
@Tag(name = "财务-运维", description = "测试夹具余额抹平（非业务功能；仅 ops profile 暴露）")
@Profile("ops")
@RestController
@RequestMapping("/v1/finance/ops")
public class FinanceOpsController {

    private final FinanceOpsService financeOpsService;

    public FinanceOpsController(FinanceOpsService financeOpsService) {
        this.financeOpsService = financeOpsService;
    }

    @Operation(summary = "抹平测试夹具余额（仅 ops profile；仅 FINANCE_DIRECTOR；重复调用 400）")
    @PostMapping("/fixture-reverse")
    @PreAuthorize("hasAnyAuthority('ROLE_FINANCE_DIRECTOR')")
    public Result<FixtureReverseVO> fixtureReverse(@RequestBody FixtureReverseRequest req) {
        return Result.success(financeOpsService.reverseFixtures(req));
    }
}
