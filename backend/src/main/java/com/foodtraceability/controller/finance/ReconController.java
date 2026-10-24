package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.ReconDifferenceQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconDifferenceVO;
import com.foodtraceability.dto.finance.recon.ReconItemQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconItemVO;
import com.foodtraceability.dto.finance.recon.ReconMatchDTO;
import com.foodtraceability.dto.finance.recon.ReconResolveDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateResultVO;
import com.foodtraceability.dto.finance.recon.ReconRunQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconRunVO;
import com.foodtraceability.service.finance.recon.ReconDifferenceService;
import com.foodtraceability.service.finance.recon.ReconRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务对账控制器（P1-B：R1 / R2 / R3 / R5 / R6 / R7 / R8 / R9 / R10）。
 *
 * <p><b>红线</b>：本控制器及其服务对 fund_flows / bank_accounts <b>只读</b>——
 * 不写流水、不改余额、不冲正；差异处理只改变 recon_differences 的状态与决议字段。</p>
 */
@Tag(name = "财务对账-勾稽与差异池", description = "跑批（R1）/批次列表详情（R2/R3）/主体列表（R5）/人工勾稽（R6/R7）/差异池（R8/R9/R10）")
@RestController
@RequestMapping("/v1/finance")
public class ReconController {

    private final ReconRunService reconRunService;
    private final ReconDifferenceService reconDifferenceService;

    public ReconController(ReconRunService reconRunService,
                           ReconDifferenceService reconDifferenceService) {
        this.reconRunService = reconRunService;
        this.reconDifferenceService = reconDifferenceService;
    }

    /** R1：发起跑批（幂等；force=true 重建） */
    @Operation(summary = "发起对账跑批（幂等，force=true 强制重建）")
    @PostMapping("/recon-runs")
    @PreAuthorize("hasAuthority('finance:bank:recon')")
    public Result<ReconRunCreateResultVO> run(@Valid @RequestBody ReconRunCreateDTO dto) {
        return Result.success(reconRunService.run(dto));
    }

    /** R2：跑批列表 */
    @Operation(summary = "对账批次列表（分页）")
    @GetMapping("/recon-runs")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<ReconRunVO>> listRuns(ReconRunQueryDTO query) {
        return Result.success(reconRunService.listRuns(query));
    }

    /** R3：跑批详情 */
    @Operation(summary = "对账批次详情（含勾稽率）")
    @GetMapping("/recon-runs/{runId}")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<ReconRunVO> getRun(@PathVariable Long runId) {
        return Result.success(reconRunService.getRun(runId));
    }

    /** R5：勾稽主体列表 */
    @Operation(summary = "勾稽主体列表（runId / side / subjectStatus / keyword）")
    @GetMapping("/recon-items")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<ReconItemVO>> listItems(ReconItemQueryDTO query) {
        return Result.success(reconRunService.listItems(query));
    }

    /** R6：人工勾稽（1:1 / 1:N / N:1 由 groupKey 表达） */
    @Operation(summary = "人工勾稽（需两侧主体均存在且状态合法）")
    @PostMapping("/recon-items/{itemId}/match")
    @PreAuthorize("hasAuthority('finance:bank:recon')")
    public Result<ReconItemVO> match(@PathVariable Long itemId, @Valid @RequestBody ReconMatchDTO dto) {
        return Result.success(reconRunService.matchItem(itemId, dto));
    }

    /** R7：解绑（仅 MANUAL_MATCHED → UNMATCHED） */
    @Operation(summary = "解绑人工勾稽（回到 UNMATCHED 并清 match_group）")
    @PostMapping("/recon-items/{itemId}/unmatch")
    @PreAuthorize("hasAuthority('finance:bank:recon')")
    public Result<ReconItemVO> unmatch(@PathVariable Long itemId) {
        return Result.success(reconRunService.unmatchItem(itemId));
    }

    /** R8：差异池列表 */
    @Operation(summary = "差异池列表（status / type / runId / assigneeId）")
    @GetMapping("/recon-differences")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<ReconDifferenceVO>> listDifferences(ReconDifferenceQueryDTO query) {
        return Result.success(reconDifferenceService.listDifferences(query));
    }

    /** R9：认领差异 */
    @Operation(summary = "认领差异（OPEN → ASSIGNED）")
    @PostMapping("/recon-differences/{id}/claim")
    @PreAuthorize("hasAuthority('finance:bank:recon')")
    public Result<ReconDifferenceVO> claim(@PathVariable Long id) {
        return Result.success(reconDifferenceService.claim(id));
    }

    /**
     * R10：处理差异。
     * WRITE_OFF → CLOSED（只记决议）；EXEMPT → EXEMPT；REVERSE → PENDING_P2_REVERSE（P2 执行冲正）。
     */
    @Operation(summary = "处理差异（WRITE_OFF / EXEMPT / REVERSE，remark 必填，不生成任何流水）")
    @PostMapping("/recon-differences/{id}/resolve")
    @PreAuthorize("hasAuthority('finance:bank:recon')")
    public Result<ReconDifferenceVO> resolve(@PathVariable Long id, @Valid @RequestBody ReconResolveDTO dto) {
        return Result.success(reconDifferenceService.resolve(id, dto));
    }
}
