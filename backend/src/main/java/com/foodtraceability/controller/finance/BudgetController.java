package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 预算管理控制器
 * 提供预算的CRUD、执行情况对比、预警等功能
 */
@Tag(name = "预算管理", description = "企业年度/月度预算的制定与执行监控")
@RestController
@RequestMapping("/v1/finance/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @Operation(summary = "创建预算", description = "新增年度或月度预算项")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:budget:create')")
    public Result<BudgetVO> create(@Valid @RequestBody BudgetCreateDTO dto) {
        return Result.success(budgetService.create(dto));
    }

    @Operation(summary = "更新预算")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:budget:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody BudgetUpdateDTO dto) {
        dto.setBudgetId(id);
        return Result.success(budgetService.update(dto));
    }

    @Operation(summary = "获取预算详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:budget:query')")
    public Result<BudgetVO> getDetail(@PathVariable Long id) {
        return Result.success(budgetService.getDetail(id));
    }

    @Operation(summary = "分页查询预算", description = "支持按年份/类型/类别等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:budget:query')")
    public Result<IPage<BudgetVO>> getPage(BudgetQueryDTO query) {
        return Result.success(budgetService.getPage(query));
    }

    @Operation(summary = "更新实际金额", description = "录入实际发生金额用于对比分析")
    @PutMapping("/{id}/actual")
    @PreAuthorize("hasAuthority('finance:budget:update')")
    public Result<Boolean> updateActualAmount(@PathVariable Long id, @RequestParam Long actualAmount) {
        return Result.success(budgetService.updateActualAmount(id, actualAmount));
    }
}
