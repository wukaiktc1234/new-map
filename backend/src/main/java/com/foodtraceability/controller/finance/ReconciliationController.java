package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.LedgerDailyVO;
import com.foodtraceability.dto.finance.recon.LedgerSummaryVO;
import com.foodtraceability.dto.finance.recon.ReconciliationStatementVO;
import com.foodtraceability.dto.finance.recon.StatementCreateRequest;
import com.foodtraceability.dto.finance.recon.StatementQueryDTO;
import com.foodtraceability.service.finance.recon.LedgerService;
import com.foodtraceability.service.finance.recon.ReconciliationStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 调节表（S3–S9）与资金日记账（S1/S2）。
 *
 * <p><b>硬规则一</b>：本控制器涉及的所有动作<b>不写 fund_flows、不改 bank_accounts.balance</b>（design §4.3）。</p>
 */
@Tag(name = "财务-调节表与日记账", description = "银行存款余额调节表 S3–S9；资金日记账 S1/S2")
@RestController
@RequestMapping("/v1/finance")
public class ReconciliationController {

    private final ReconciliationStatementService statementService;
    private final LedgerService ledgerService;

    public ReconciliationController(ReconciliationStatementService statementService, LedgerService ledgerService) {
        this.statementService = statementService;
        this.ledgerService = ledgerService;
    }

    /** S3 生成/重算 */
    @Operation(summary = "生成/重算银行存款余额调节表")
    @PostMapping("/recon/statements")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<ReconciliationStatementVO> generate(@RequestBody StatementCreateRequest req) {
        return Result.success(statementService.generate(req));
    }

    /** S4 列表 */
    @Operation(summary = "调节表列表")
    @GetMapping("/recon/statements")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<ReconciliationStatementVO>> page(StatementQueryDTO query) {
        return Result.success(statementService.page(query));
    }

    /** S5 详情 */
    @Operation(summary = "调节表详情（含四类未达账项明细）")
    @GetMapping("/recon/statements/{statementId}")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<ReconciliationStatementVO> detail(@PathVariable Long statementId) {
        return Result.success(statementService.detail(statementId));
    }

    /** S6 提交（V5 硬门禁） */
    @Operation(summary = "提交调节表（V5：调节后两侧必须相等）")
    @PostMapping("/recon/statements/{statementId}/submit")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<ReconciliationStatementVO> submit(@PathVariable Long statementId) {
        return Result.success(statementService.submit(statementId));
    }

    /** S7 审核（V8 四眼 + 角色门禁） */
    @Operation(summary = "审核调节表（V8：审核人≠提交人，且不得为出纳）")
    @PostMapping("/recon/statements/{statementId}/approve")
    @PreAuthorize("hasAuthority('finance:bank:reconcile')")
    public Result<ReconciliationStatementVO> approve(@PathVariable Long statementId) {
        return Result.success(statementService.approve(statementId));
    }

    /** BL-6 §3：人工处置同额歧义（不自动二选一） */
    @Operation(summary = "处置调节表同额歧义（decision=A|C / B|D + targetRef）")
    @PostMapping("/recon/statements/{statementId}/items/{itemId}/resolve-ambiguity")
    @PreAuthorize("hasAnyAuthority('ROLE_FINANCE_DIRECTOR','ROLE_FINANCE_MANAGER')")
    public Result<ReconciliationStatementVO> resolveAmbiguity(@PathVariable Long statementId,
                                                             @PathVariable Long itemId,
                                                             @RequestBody java.util.Map<String, String> body) {
        return Result.success(statementService.resolveAmbiguity(statementId, itemId,
                body == null ? null : body.get("decision"),
                body == null ? null : body.get("targetRef"),
                body == null ? null : body.get("reason")));
    }

    /** S9 导出 CSV */
    @Operation(summary = "导出调节表 CSV")
    @GetMapping("/recon/statements/{statementId}/export")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public ResponseEntity<byte[]> export(@PathVariable Long statementId) {
        String csv = statementService.exportCsv(statementId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=statement-" + statementId + ".csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** S1 日记账 */
    @Operation(summary = "资金日记账（按日，含 V1 校验）")
    @GetMapping("/ledger/daily")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<LedgerDailyVO> daily(@RequestParam Long accountId,
                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return Result.success(ledgerService.daily(accountId, from, to));
    }

    /** S2 月结汇总 */
    @Operation(summary = "资金月结汇总（含 V2 校验）")
    @GetMapping("/ledger/summary")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<LedgerSummaryVO> summary(@RequestParam Long accountId, @RequestParam String month) {
        return Result.success(ledgerService.summary(accountId, month));
    }
}
