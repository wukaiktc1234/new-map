package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.entity.finance.AccountingPeriod;
import com.foodtraceability.service.finance.AccountingPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 会计期间控制器
 * 提供会计期间的创建、结账、反结账、试算平衡、损益结转等操作
 */
@Tag(name = "会计期间管理", description = "会计期间的生命周期管理，包括结账、反结账、试算平衡、损益结转")
@RestController
@RequestMapping("/v1/finance/accounting-periods")
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    public AccountingPeriodController(AccountingPeriodService accountingPeriodService) {
        this.accountingPeriodService = accountingPeriodService;
    }

    @Operation(summary = "创建会计期间", description = "新增月度/季度/年度会计期间")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:period:create')")
    public Result<AccountingPeriodVO> create(@Valid @RequestBody AccountingPeriodCreateDTO dto) {
        return Result.success(accountingPeriodService.create(dto));
    }

    @Operation(summary = "获取期间详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:period:query')")
    public Result<AccountingPeriodVO> getDetail(@PathVariable Long id) {
        return Result.success(accountingPeriodService.getDetail(id));
    }

    @Operation(summary = "分页查询会计期间", description = "支持按期间类型和状态筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:period:query')")
    public Result<IPage<AccountingPeriodVO>> getPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer periodType,
            @RequestParam(required = false) Integer status) {
        return Result.success(accountingPeriodService.getPage(current, size, periodType, status));
    }

    @Operation(summary = "结账", description = "对指定会计期间执行结账操作")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('finance:period:update')")
    public Result<Boolean> closePeriod(@PathVariable Long id, @RequestParam Long operatorId) {
        return Result.success(accountingPeriodService.closePeriod(id, operatorId));
    }

    @Operation(summary = "反结账", description = "重新开放已结账的会计期间")
    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('finance:period:update')")
    public Result<Boolean> reopenPeriod(@PathVariable Long id, @RequestParam Long operatorId) {
        return Result.success(accountingPeriodService.reopenPeriod(id, operatorId));
    }

    @Operation(summary = "试算平衡", description = "对指定期间进行试算平衡校验")
    @GetMapping("/{id}/trial-balance")
    @PreAuthorize("hasAuthority('finance:period:query')")
    public Result<TrialBalanceResultVO> trialBalance(@PathVariable Long id) {
        return Result.success(accountingPeriodService.trialBalance(id));
    }

    @Operation(summary = "获取结账检查清单", description = "获取结账前的检查项清单")
    @GetMapping("/{id}/closing-checklist")
    @PreAuthorize("hasAuthority('finance:period:query')")
    public Result<ClosingChecklistVO> getClosingChecklist(@PathVariable Long id) {
        return Result.success(accountingPeriodService.getClosingChecklist(id));
    }

    @Operation(summary = "损益结转", description = "将损益类科目余额结转至未分配利润")
    @PostMapping("/{id}/profit-transfer")
    @PreAuthorize("hasAuthority('finance:period:approve')")
    public Result<Boolean> profitTransfer(@PathVariable Long id, @RequestParam Long operatorId) {
        return Result.success(accountingPeriodService.profitTransfer(id, operatorId));
    }

    @Operation(summary = "获取当前会计期间", description = "获取系统当前所处的会计期间")
    @GetMapping("/current")
    @PreAuthorize("hasAuthority('finance:period:query')")
    public Result<AccountingPeriod> getCurrentPeriod() {
        return Result.success(accountingPeriodService.getCurrentPeriod());
    }
}
