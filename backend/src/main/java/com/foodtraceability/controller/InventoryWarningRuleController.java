package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.InventoryWarningRuleCreateDTO;
import com.foodtraceability.dto.InventoryWarningRuleUpdateDTO;
import com.foodtraceability.entity.InventoryWarningRule;
import com.foodtraceability.service.InventoryWarningRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 库存预警规则控制器
 * 处理库存预警规则相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/warning-rules")
@Tag(name = "库存预警规则管理")
public class InventoryWarningRuleController {
    

    public InventoryWarningRuleController(InventoryWarningRuleService warningRuleService) {
        this.warningRuleService = warningRuleService;
    }

    private final InventoryWarningRuleService warningRuleService;
    
    /**
     * 创建库存预警规则
     */
    @PostMapping
    @Operation(summary = "创建库存预警规则")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('inventory:create')")
    public Result<InventoryWarningRule> createWarningRule(@Valid @RequestBody InventoryWarningRuleCreateDTO createDTO) {
        try {
            InventoryWarningRule rule = new InventoryWarningRule();
            rule.setRuleName(createDTO.getRuleName());
            rule.setWarehouseId(createDTO.getWarehouseId());
            rule.setWarningType(createDTO.getWarningType());
            rule.setConditionField(createDTO.getConditionField());
            rule.setConditionOperator(createDTO.getConditionOperator());
            rule.setThresholdValue(createDTO.getThresholdValue());
            rule.setNotifyMethod(createDTO.getNotifyMethod());
            rule.setIsEnabled(createDTO.getIsEnabled());
            InventoryWarningRule createdRule = warningRuleService.createWarningRule(rule);
            return Result.success(createdRule, "创建库存预警规则成功");
        } catch (Exception e) {
            return Result.error(500, "创建库存预警规则失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取单个库存预警规则详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取单个库存预警规则详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryWarningRule> getWarningRuleDetail(
            @Parameter(description = "规则ID") @PathVariable("id") Long id) {
        try {
            InventoryWarningRule rule = warningRuleService.getWarningRuleById(id);
            return Result.success(rule, "获取库存预警规则详情成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存预警规则详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新库存预警规则
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新库存预警规则")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryWarningRule> updateWarningRule(
            @Parameter(description = "规则ID") @PathVariable("id") Long id,
            @Valid @RequestBody InventoryWarningRuleUpdateDTO updateDTO) {
        try {
            InventoryWarningRule rule = new InventoryWarningRule();
            rule.setRuleName(updateDTO.getRuleName());
            rule.setWarehouseId(updateDTO.getWarehouseId());
            rule.setWarningType(updateDTO.getWarningType());
            rule.setConditionField(updateDTO.getConditionField());
            rule.setConditionOperator(updateDTO.getConditionOperator());
            rule.setThresholdValue(updateDTO.getThresholdValue());
            rule.setNotifyMethod(updateDTO.getNotifyMethod());
            rule.setIsEnabled(updateDTO.getIsEnabled());
            InventoryWarningRule updatedRule = warningRuleService.updateWarningRule(id, rule);
            return Result.success(updatedRule, "更新库存预警规则成功");
        } catch (Exception e) {
            return Result.error(500, "更新库存预警规则失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除库存预警规则
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除库存预警规则")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PreAuthorize("hasAuthority('inventory:delete')")
    public Result<Void> deleteWarningRule(
            @Parameter(description = "规则ID") @PathVariable("id") Long id) {
        try {
            warningRuleService.deleteWarningRule(id);
            return Result.success(null, "删除库存预警规则成功");
        } catch (Exception e) {
            return Result.error(500, "删除库存预警规则失败：" + e.getMessage());
        }
    }
    
    /**
     * 分页查询库存预警规则列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询库存预警规则列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<InventoryWarningRule>> getWarningRulePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "规则名称") @RequestParam(required = false) String ruleName,
            @Parameter(description = "产品类别") @RequestParam(required = false) String category,
            @Parameter(description = "预警级别") @RequestParam(required = false) Integer warningLevel,
            @Parameter(description = "是否启用") @RequestParam(required = false) Integer enabled) {
        try {
            Page<InventoryWarningRule> pageParam = new Page<>(page, pageSize);
            com.baomidou.mybatisplus.core.metadata.IPage<InventoryWarningRule> rulePage = 
                    warningRuleService.getWarningRulePage(pageParam, ruleName, category, warningLevel, enabled);
            return Result.success(rulePage, "查询库存预警规则列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询库存预警规则列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 启用/禁用库存预警规则
     */
    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用库存预警规则")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @PreAuthorize("hasAuthority('inventory:update')")
    public Result<InventoryWarningRule> toggleRuleStatus(
            @Parameter(description = "规则ID") @PathVariable("id") Long id,
            @Parameter(description = "是否启用（0：禁用，1：启用）") @RequestParam Integer enabled) {
        try {
            InventoryWarningRule updatedRule = warningRuleService.toggleRuleStatus(id, enabled);
            return Result.success(updatedRule, "操作成功");
        } catch (Exception e) {
            return Result.error(500, "操作失败：" + e.getMessage());
        }
    }
}
