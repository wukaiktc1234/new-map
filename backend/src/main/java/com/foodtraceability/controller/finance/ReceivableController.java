package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.ReceivableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 应收账款控制器
 * 提供应收账款的CRUD、收款确认、坏账核销等功能
 */
@Tag(name = "应收账款管理", description = "客户欠款管理、账龄分析、催款提醒")
@RestController
@RequestMapping("/v1/finance/receivables")
public class ReceivableController {

    private final ReceivableService receivableService;

    public ReceivableController(ReceivableService receivableService) {
        this.receivableService = receivableService;
    }

    @Operation(summary = "创建应收账款", description = "新增客户应收账款记录")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:receivable:create')")
    public Result<ReceivableVO> create(@Valid @RequestBody ReceivableCreateDTO dto) {
        return Result.success(receivableService.create(dto));
    }

    @Operation(summary = "更新应收账款", description = "修改客户名称、原始金额、备注等信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:receivable:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody ReceivableUpdateDTO dto) {
        dto.setReceivableId(id);
        return Result.success(receivableService.update(dto));
    }

    @Operation(summary = "删除应收账款", description = "逻辑删除指定应收账款（已核销的不允许删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:receivable:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(receivableService.deleteReceivable(id));
    }

    @Operation(summary = "获取应收账款详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:receivable:query')")
    public Result<ReceivableVO> getDetail(@PathVariable Long id) {
        return Result.success(receivableService.getDetail(id));
    }

    @Operation(summary = "分页查询应收账款", description = "支持按客户/状态/到期日等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:receivable:query')")
    public Result<IPage<ReceivableVO>> getPage(ReceivableQueryDTO query) {
        return Result.success(receivableService.getPage(query));
    }

    @Operation(summary = "确认收款", description = "登记客户还款金额")
    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAuthority('finance:receivable:approve')")
    public Result<Boolean> confirmPayment(@PathVariable Long id, @RequestParam Long amount) {
        return Result.success(receivableService.confirmPayment(id, amount));
    }

    @Operation(summary = "核销坏账", description = "将无法收回的款项标记为坏账并核销")
    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasAuthority('finance:receivable:approve')")
    public Result<Boolean> writeOff(@PathVariable Long id) {
        return Result.success(receivableService.writeOff(id));
    }
}
