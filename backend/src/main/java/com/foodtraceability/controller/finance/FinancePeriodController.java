package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.period.FinancePeriodVO;
import com.foodtraceability.service.finance.FinancePeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 期末结账 / 反结账（P4-4）。
 *
 * <p><b>权限</b>：close / list / summary 复用既有 {@code finance:bank:view}（**不新增权限码**）；
 * reopen 额外要求财务总监角色（四眼原则：结账谁都能按流程走，解封只给总监）。</p>
 */
@Tag(name = "财务-期末结账", description = "结账 / 反结账 / 期间列表与快照（P4-4）")
@RestController
@RequestMapping("/v1/finance/periods")
public class FinancePeriodController {

    private final FinancePeriodService financePeriodService;

    public FinancePeriodController(FinancePeriodService financePeriodService) {
        this.financePeriodService = financePeriodService;
    }

    @Operation(summary = "会计期间列表（含快照合计）")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<List<FinancePeriodVO>> list() {
        return Result.success(financePeriodService.list());
    }

    @Operation(summary = "期间详情（逐账户快照 + dirty 标记）")
    @GetMapping("/{periodCode}")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<FinancePeriodVO> summary(@PathVariable String periodCode) {
        return Result.success(financePeriodService.summary(periodCode));
    }

    @Operation(summary = "结账（RK-8 门禁 + V1 试算校验）")
    @PostMapping("/{periodCode}/close")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<FinancePeriodVO> close(@PathVariable String periodCode,
                                         @RequestBody(required = false) Map<String, String> body) {
        return Result.success(financePeriodService.close(periodCode, body == null ? null : body.get("remark")));
    }

    @Operation(summary = "反结账（仅财务总监；只允许最后一个已结账期间）")
    @PostMapping("/{periodCode}/reopen")
    @PreAuthorize("hasAuthority('finance:bank:view') and hasAnyAuthority('ROLE_FINANCE_DIRECTOR')  /* A46：原写法含 'ROLE_finance_director' 大小写错误码，已删（roles 表只有 ROLE_FINANCE_DIRECTOR） */")
    public Result<FinancePeriodVO> reopen(@PathVariable String periodCode,
                                          @RequestBody(required = false) Map<String, String> body) {
        return Result.success(financePeriodService.reopen(periodCode, body == null ? null : body.get("reason")));
    }
}
