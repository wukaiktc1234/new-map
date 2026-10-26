package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.deposit.DepositSlipQueryDTO;
import com.foodtraceability.dto.finance.deposit.DepositSlipVO;
import com.foodtraceability.dto.finance.deposit.DepositToBankDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipDTO;
import com.foodtraceability.dto.finance.deposit.GenerateSlipResultVO;
import com.foodtraceability.dto.finance.deposit.ReceiveSlipDTO;
import com.foodtraceability.dto.finance.deposit.RejectSlipDTO;
import com.foodtraceability.dto.finance.deposit.ReverseSlipDTO;
import com.foodtraceability.service.finance.deposit.DepositSlipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 现金缴款单控制器（P2-A · SL1–SL11）。
 *
 * <p><b>红线</b>：RECEIVED 不产生任何 fund_flows；只有 DEPOSITED 生成恰好 2 条内部转账；
 * 冲正只增反向流水；余额只走乐观锁。</p>
 */
@Tag(name = "财务-现金缴款单", description = "生成/提交/收款/存行/复核/关闭/驳回/冲正/导出（SL1–SL11）")
@RestController
@RequestMapping("/v1/finance/deposit-slips")
public class DepositSlipController {

    private final DepositSlipService depositSlipService;

    public DepositSlipController(DepositSlipService depositSlipService) {
        this.depositSlipService = depositSlipService;
    }

    /** SL1：按班次生成（幂等，shift_id 唯一） */
    @Operation(summary = "按班次生成缴款单（幂等）")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('finance:deposit-slip:create')")
    public Result<GenerateSlipResultVO> generate(@Valid @RequestBody GenerateSlipDTO dto) {
        return Result.success(depositSlipService.generate(dto));
    }

    /** SL2：列表 */
    @Operation(summary = "缴款单列表（分页）")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    public Result<IPage<DepositSlipVO>> list(DepositSlipQueryDTO query) {
        return Result.success(depositSlipService.list(query));
    }

    /** SL3：详情 */
    @Operation(summary = "缴款单详情")
    @GetMapping("/{slipId}")
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    public Result<DepositSlipVO> detail(@PathVariable Long slipId) {
        return Result.success(depositSlipService.detail(slipId));
    }

    /** SL4：提交（CASHIER） */
    @Operation(summary = "提交缴款单（DRAFT → SUBMITTED）")
    @PostMapping("/{slipId}/submit")
    @PreAuthorize("hasAuthority('finance:deposit-slip:submit')")
    public Result<DepositSlipVO> submit(@PathVariable Long slipId) {
        return Result.success(depositSlipService.submit(slipId));
    }

    /** SL5：收款（TELLER），不生成流水 */
    @Operation(summary = "收款（SUBMITTED → RECEIVED，只记短款，不生成流水）")
    @PostMapping("/{slipId}/receive")
    @PreAuthorize("hasAuthority('finance:deposit-slip:receive')")
    public Result<DepositSlipVO> receive(@PathVariable Long slipId, @Valid @RequestBody ReceiveSlipDTO dto) {
        return Result.success(depositSlipService.receive(slipId, dto));
    }

    /** SL6：存行（TELLER），生成恰好 2 条内部转账 */
    @Operation(summary = "存行（RECEIVED → DEPOSITED，生成恰好 2 条内部转账）")
    @PostMapping("/{slipId}/deposit")
    @PreAuthorize("hasAuthority('finance:deposit-slip:deposit')")
    public Result<DepositSlipVO> deposit(@PathVariable Long slipId, @Valid @RequestBody DepositToBankDTO dto) {
        return Result.success(depositSlipService.deposit(slipId, dto));
    }

    /** SL7：复核（FINANCE_DIRECTOR，四眼） */
    @Operation(summary = "复核（DEPOSITED → VERIFIED，校验四眼：复核人 ≠ 收款人）")
    @PostMapping("/{slipId}/verify")
    @PreAuthorize("hasAuthority('finance:deposit-slip:verify')")
    public Result<DepositSlipVO> verify(@PathVariable Long slipId) {
        return Result.success(depositSlipService.verify(slipId));
    }

    /** SL8：关闭 */
    @Operation(summary = "关闭（VERIFIED → CLOSED）")
    @PostMapping("/{slipId}/close")
    @PreAuthorize("hasAuthority('finance:deposit-slip:close')")
    public Result<DepositSlipVO> close(@PathVariable Long slipId) {
        return Result.success(depositSlipService.close(slipId));
    }

    /** SL9：驳回（权限码 finance:deposit-slip:reject —— P3 前置已独立成码，2026-10-25） */
    @Operation(summary = "驳回（SUBMITTED/RECEIVED → REJECTED）")
    @PostMapping("/{slipId}/reject")
    @PreAuthorize("hasAuthority('finance:deposit-slip:reject')")
    public Result<DepositSlipVO> reject(@PathVariable Long slipId, @Valid @RequestBody RejectSlipDTO dto) {
        return Result.success(depositSlipService.reject(slipId, dto));
    }

    /** SL10：冲正（FINANCE_DIRECTOR），只增反向流水 */
    @Operation(summary = "冲正（只增反向流水，原流水置 status=3）")

    @PostMapping("/{slipId}/reverse")
    @PreAuthorize("hasAuthority('finance:deposit-slip:reverse')")
    public Result<DepositSlipVO> reverse(@PathVariable Long slipId, @Valid @RequestBody ReverseSlipDTO dto) {
        return Result.success(depositSlipService.reverse(slipId, dto));
    }
    /** 差异3：日汇总（人工汇总模式）—— 当天已存行缴款单 + 绑/未绑流水拆分 + 银行进账 */
    @GetMapping("/daily-summary")
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    @Operation(summary = "存行日汇总（差异3）", description = "unmatchedCount 未绑流水单数不得静默；bankInflowFen=当日该批账户银行进账合计")
    public Result<com.foodtraceability.dto.finance.deposit.DepositDailySummaryVO> dailySummary(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return Result.success(depositSlipService.dailySummary(date));
    }

    /** N19-3b：待确认归属的数据源（空时返回空数组，不返回 404） */
    @GetMapping("/{slipId}/pending-refunds")
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    @Operation(summary = "待确认归属退款 + 候选班次（N19-3b）",
            description = "pendingRefunds=本单班次边界带内且无归属记录的现金退款；candidateShifts=上一/本/下一班次；空则返回空数组")
    public Result<com.foodtraceability.dto.finance.deposit.DepositSlipPendingRefundsVO> pendingRefunds(
            @PathVariable Long slipId) {
        return Result.success(depositSlipService.pendingRefunds(slipId));
    }


    /** N19-2④：人工确认退款归属班次（幂等 + 覆盖 + 乐观锁），并重算退款构成与净差异 */
    @Operation(summary = "人工确认退款归属班次（N19-2④）", description = "写入 refund_shift_attribution 并重算该缴款单的 refund_component_fen / difference_net_fen")
    @PostMapping("/{slipId}/confirm-attribution")
    @PreAuthorize("hasAuthority('finance:deposit-slip:confirm-attribution')")
    public Result<DepositSlipVO> confirmAttribution(
            @PathVariable Long slipId,
            @RequestBody Map<String, Object> body) {
        Long refundRecordId = body.get("refundRecordId") == null ? null : Long.valueOf(String.valueOf(body.get("refundRecordId")));
        Long shiftId = body.get("shiftId") == null ? null : Long.valueOf(String.valueOf(body.get("shiftId")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return Result.success(depositSlipService.confirmAttribution(slipId, refundRecordId, shiftId, reason));
    }

    /** SL11：导出 CSV */
    @Operation(summary = "导出缴款单 CSV")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('finance:deposit-slip:view')")
    public ResponseEntity<byte[]> export(DepositSlipQueryDTO query) {
        String csv = depositSlipService.exportCsv(query);
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=deposit-slips.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
