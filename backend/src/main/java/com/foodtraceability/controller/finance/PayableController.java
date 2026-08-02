package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.PayableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 应付账款控制器
 * 提供应付账款的CRUD、付款确认等功能
 */
@Tag(name = "应付账款管理", description = "供应商欠款管理、账期跟踪、付款计划")
@RestController
@RequestMapping("/v1/finance/payables")
public class PayableController {

    private final PayableService payableService;

    public PayableController(PayableService payableService) {
        this.payableService = payableService;
    }

    @Operation(summary = "创建应付账款", description = "新增供应商应付账款记录")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:payable:create')")
    public Result<PayableVO> create(@Valid @RequestBody PayableCreateDTO dto) {
        return Result.success(payableService.create(dto));
    }

    @Operation(summary = "更新应付账款", description = "修改供应商名称、到期日、账期等信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:payable:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody PayableUpdateDTO dto) {
        dto.setPayableId(id);
        return Result.success(payableService.update(dto));
    }

    @Operation(summary = "删除应付账款", description = "逻辑删除指定应付账款（已付清的不允许删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:payable:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(payableService.deletePayable(id));
    }

    @Operation(summary = "获取应付账款详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:payable:query')")
    public Result<PayableVO> getDetail(@PathVariable Long id) {
        return Result.success(payableService.getDetail(id));
    }

    @Operation(summary = "分页查询应付账款", description = "支持按供应商/状态/到期日等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:payable:query')")
    public Result<IPage<PayableVO>> getPage(PayableQueryDTO query) {
        return Result.success(payableService.getPage(query));
    }

    @Operation(summary = "确认付款", description = "登记向供应商付款金额")
    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAuthority('finance:payable:approve')")
    public Result<Boolean> confirmPayment(@PathVariable Long id, @RequestParam Long amount) {
        return Result.success(payableService.confirmPayment(id, amount));
    }
}
