package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.recon.BankTransactionQueryDTO;
import com.foodtraceability.dto.finance.recon.BankTransactionVO;
import com.foodtraceability.dto.finance.recon.ImportBatchVO;
import com.foodtraceability.dto.finance.recon.ImportCommitResultVO;
import com.foodtraceability.dto.finance.recon.ImportRowVO;
import com.foodtraceability.service.finance.recon.BankImportService;
import com.foodtraceability.service.finance.recon.BankTransactionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 银行/渠道流水导入控制器（P0-B · I1–I6，design §1.5）。
 *
 * <p>两段式：import 只做预检（不落 bank_transactions）→ commit 才落库。</p>
 * <p>P0 红线：本控制器<b>不产生任何记账动作</b>——不写 fund_flows、不改 bank_accounts.balance（TD-14 裁决：否）。</p>
 */
@Tag(name = "财务对账-流水导入", description = "CSV 导入（预检/提交/撤销）与外部流水只读查询")
@RestController
@RequestMapping("/v1/finance")
public class BankImportController {

    private final BankImportService bankImportService;
    private final BankTransactionQueryService bankTransactionQueryService;

    public BankImportController(BankImportService bankImportService,
                               BankTransactionQueryService bankTransactionQueryService) {
        this.bankImportService = bankImportService;
        this.bankTransactionQueryService = bankTransactionQueryService;
    }

    /** I1：上传 + 预检（不落事实表） */
    @Operation(summary = "上传并预检 CSV（不落事实表）")
    @PostMapping("/bank-transactions/import")
    @PreAuthorize("hasAuthority('finance:bank:import')")
    public Result<ImportBatchVO> importCsv(@RequestParam("file") MultipartFile file,
                                           @RequestParam("sourceType") String sourceType,
                                           @RequestParam("accountId") Long accountId) {
        return Result.success(bankImportService.importCsv(file, sourceType, accountId));
    }

    /** I2：批次详情 */
    @Operation(summary = "导入批次详情")
    @GetMapping("/imports/{batchId}")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<ImportBatchVO> getBatch(@PathVariable Long batchId) {
        return Result.success(bankImportService.getBatch(batchId));
    }

    /** I3：行级回执 */
    @Operation(summary = "导入批次行级回执（分页）")
    @GetMapping("/imports/{batchId}/rows")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<ImportRowVO>> listRows(@PathVariable Long batchId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "1") int current,
                                               @RequestParam(defaultValue = "50") int size) {
        return Result.success(bankImportService.listRows(batchId, status, current, size));
    }

    /** I4：确认落库 */
    @Operation(summary = "提交导入批次（落 bank_transactions）")
    @PostMapping("/imports/{batchId}/commit")
    @PreAuthorize("hasAuthority('finance:bank:import')")
    public Result<ImportCommitResultVO> commit(@PathVariable Long batchId) {
        return Result.success(bankImportService.commit(batchId));
    }

    /** I5：撤销未提交批次 */
    @Operation(summary = "撤销导入批次（未提交才允许）")
    @PostMapping("/imports/{batchId}/cancel")
    @PreAuthorize("hasAuthority('finance:bank:import')")
    public Result<ImportBatchVO> cancel(@PathVariable Long batchId,
                                        @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return Result.success(bankImportService.cancel(batchId, reason));
    }

    /** I6：外部流水只读查询 */
    @Operation(summary = "外部流水只读查询")
    @GetMapping("/bank-transactions")
    @PreAuthorize("hasAuthority('finance:bank:view')")
    public Result<IPage<BankTransactionVO>> page(BankTransactionQueryDTO query) {
        return Result.success(bankTransactionQueryService.page(query));
    }
}
