package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票管理Controller
 *
 * <p>Sprint 3.1 P0 F-003/F-004：发票 CRUD + 状态流转管理。</p>
 *
 * <p>路径前缀：/v1/finance/invoices（原 /v1/invoices）</p>
 *
 * <p>状态机：draft(0) → issued(5) → void(6) / red-flushed(4)。</p>
 */
@Tag(name = "发票管理", description = "进项发票和销项发票的管理，含状态流转")
@RestController
@RequestMapping("/v1/finance/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * 分页查询发票列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询发票列表", description = "支持按发票号码/类型/类别/状态等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:invoice:view')")
    public Result<IPage<FinanceInvoiceVO>> getPage(FinanceInvoiceQueryDTO query) {
        return Result.success(invoiceService.getPage(query));
    }

    /**
     * 获取发票详情
     *
     * @param invoiceId 发票ID
     * @return 发票VO
     */
    @Operation(summary = "获取发票详情", description = "根据ID查询发票完整信息")
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('finance:invoice:view')")
    public Result<FinanceInvoiceVO> getDetail(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.getDetail(invoiceId));
    }

    /**
     * 创建发票记录
     *
     * @param dto 创建DTO
     * @return 创建后的发票VO
     */
    @Operation(summary = "创建发票记录", description = "创建新的发票记录，初始状态为草稿(0)")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:invoice:create')")
    public Result<FinanceInvoiceVO> create(@Valid @RequestBody FinanceInvoiceCreateDTO dto) {
        return Result.success(invoiceService.create(dto));
    }

    /**
     * 更新发票（仅 draft 状态可改）
     *
     * @param invoiceId 发票ID
     * @param dto       更新DTO
     * @return 更新后的发票VO
     */
    @Operation(summary = "更新发票", description = "仅草稿(0)状态的发票可以更新")
    @PutMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('finance:invoice:update')")
    public Result<FinanceInvoiceVO> update(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId,
            @Valid @RequestBody FinanceInvoiceUpdateDTO dto) {
        dto.setInvoiceId(invoiceId);
        return Result.success(invoiceService.update(dto));
    }

    /**
     * 删除发票（逻辑删除）
     *
     * @param invoiceId 发票ID
     * @return 操作结果
     */
    @Operation(summary = "删除发票", description = "逻辑删除发票记录")
    @DeleteMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('finance:invoice:delete')")
    public Result<Boolean> delete(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.delete(invoiceId));
    }

    /**
     * 开具发票（draft → issued）
     *
     * @param invoiceId 发票ID
     * @return 开具后的发票VO
     */
    @Operation(summary = "开具发票", description = "状态流转：草稿(0) → 已开具(5)")
    @PostMapping("/{invoiceId}/issue")
    @PreAuthorize("hasAuthority('finance:invoice:issue')")
    public Result<FinanceInvoiceVO> issue(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.issue(invoiceId));
    }

    /**
     * 作废发票（issued → void）
     *
     * @param invoiceId 发票ID
     * @return 作废后的发票VO
     */
    @Operation(summary = "作废发票", description = "状态流转：已开具(5) → 已作废(6)")
    @PutMapping("/{invoiceId}/void")
    @PreAuthorize("hasAuthority('finance:invoice:void')")
    public Result<FinanceInvoiceVO> voidInvoice(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.voidInvoice(invoiceId));
    }

    /**
     * 红冲发票（issued → red-flushed）
     *
     * @param invoiceId 发票ID
     * @return 红冲后的发票VO
     */
    @Operation(summary = "红冲发票", description = "状态流转：已开具(5) → 已红冲(4)")
    @PutMapping("/{invoiceId}/red-flush")
    @PreAuthorize("hasAuthority('finance:invoice:red-flush')")
    public Result<FinanceInvoiceVO> redFlush(
            @Parameter(description = "发票ID", required = true)
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.redFlush(invoiceId));
    }
}
