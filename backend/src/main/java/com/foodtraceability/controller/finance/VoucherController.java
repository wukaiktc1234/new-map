package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 记账凭证控制器
 * 提供凭证的CRUD、审核、过账、作废等操作
 */
@Tag(name = "记账凭证管理", description = "记账凭证的增删改查及状态流转")
@RestController
@RequestMapping("/v1/finance/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @Operation(summary = "创建记账凭证", description = "创建新的记账凭证，自动校验借贷平衡")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:voucher:create')")
    public Result<FinanceVoucherVO> create(@Valid @RequestBody FinanceVoucherCreateDTO dto) {
        return Result.success(voucherService.create(dto));
    }

    @Operation(summary = "更新记账凭证", description = "仅暂存状态的凭证可修改")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody FinanceVoucherUpdateDTO dto) {
        dto.setVoucherId(id);
        return Result.success(voucherService.update(dto));
    }

    @Operation(summary = "获取凭证详情", description = "获取凭证头信息和分录明细")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:voucher:query')")
    public Result<FinanceVoucherVO> getDetail(@PathVariable Long id) {
        return Result.success(voucherService.getDetail(id));
    }

    @Operation(summary = "分页查询凭证列表", description = "支持按期间/类型/状态等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:voucher:query')")
    public Result<IPage<FinanceVoucherVO>> getPage(FinanceVoucherQueryDTO query) {
        return Result.success(voucherService.getPage(query));
    }

    @Operation(summary = "审核凭证", description = "将凭证从暂存状态变更为已审核状态")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:voucher:approve')")
    public Result<Boolean> approve(@PathVariable Long id) {
        return Result.success(voucherService.approve(id));
    }

    @Operation(summary = "过账凭证", description = "将凭证从已审核状态变更为已过账状态")
    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('finance:voucher:approve')")
    public Result<Boolean> post(@PathVariable Long id) {
        return Result.success(voucherService.post(id));
    }

    @Operation(summary = "反审核凭证", description = "将凭证从已审核状态退回暂存状态")
    @PostMapping("/{id}/unapprove")
    @PreAuthorize("hasAuthority('finance:voucher:approve')")
    public Result<Boolean> unapprove(@PathVariable Long id) {
        return Result.success(voucherService.unapprove(id));
    }

    @Operation(summary = "反过账凭证", description = "将凭证从已过账状态退回已审核状态")
    @PostMapping("/{id}/unpost")
    @PreAuthorize("hasAuthority('finance:voucher:approve')")
    public Result<Boolean> unpost(@PathVariable Long id) {
        return Result.success(voucherService.unpost(id));
    }

    @Operation(summary = "作废凭证", description = "将暂存或已审核状态的凭证作废（已过账需红字冲销）")
    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('finance:voucher:approve')")
    public Result<Boolean> voidVoucher(@PathVariable Long id) {
        return Result.success(voucherService.voidVoucher(id));
    }
}
