package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PositionCodeRuleDTO;
import com.foodtraceability.service.PositionCodeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 职位编码规则管理控制器
 */
@Tag(name = "职位编码规则管理", description = "职位编码规则管理接口")
@RestController
@RequestMapping("/v1/position-code-rules")
public class PositionCodeRuleController {

    private static final Logger logger = LoggerFactory.getLogger(PositionCodeRuleController.class);
    
    private final PositionCodeRuleService positionCodeRuleService;

    public PositionCodeRuleController(PositionCodeRuleService positionCodeRuleService) {
        this.positionCodeRuleService = positionCodeRuleService;
        logger.info("=== PositionCodeRuleController 被实例化 ===");
    }

    /**
     * 获取所有规则
     */
    @Operation(summary = "获取所有职位编码规则")
    @GetMapping
    public Result<List<PositionCodeRuleDTO>> getAllRules() {
        List<PositionCodeRuleDTO> rules = positionCodeRuleService.getAllRules();
        return Result.success(rules);
    }

    /**
     * 添加规则
     */
    @Operation(summary = "添加职位编码规则")
    @PostMapping
    public Result<PositionCodeRuleDTO> addRule(@RequestBody PositionCodeRuleDTO rule) {
        PositionCodeRuleDTO savedRule = positionCodeRuleService.addRule(rule);
        return Result.success(savedRule);
    }

    /**
     * 删除规则
     */
    @Operation(summary = "删除职位编码规则")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        positionCodeRuleService.deleteRule(id);
        return Result.success();
    }

    /**
     * 批量更新规则
     */
    @Operation(summary = "批量更新职位编码规则")
    @PutMapping
    public Result<Void> updateRules(@RequestBody List<PositionCodeRuleDTO> rules) {
        positionCodeRuleService.updateRules(rules);
        return Result.success();
    }

    /**
     * 获取全局格式模板
     */
    @Operation(summary = "获取全局职位编码格式模板")
    @GetMapping("/global-format")
    public Result<String> getGlobalFormatTemplate() {
        String formatTemplate = positionCodeRuleService.getGlobalFormatTemplate();
        return Result.success(formatTemplate);
    }

    /**
     * 更新全局格式模板
     */
    @Operation(summary = "更新全局职位编码格式模板")
    @PutMapping("/global-format")
    public Result<Void> updateGlobalFormatTemplate(@RequestBody Map<String, String> request) {
        String formatTemplate = request.get("formatTemplate");
        positionCodeRuleService.updateGlobalFormatTemplate(formatTemplate);
        return Result.success();
    }

    /**
     * 恢复默认规则
     */
    @Operation(summary = "恢复默认职位编码规则")
    @PostMapping("/reset")
    public Result<Void> resetToDefault() {
        positionCodeRuleService.resetToDefault();
        return Result.success();
    }

    /**
     * 获取格式选项列表
     */
    @Operation(summary = "获取格式选项列表")
    @GetMapping("/format-options")
    public Result<List<Map<String, String>>> getFormatOptions() {
        List<Map<String, String>> options = positionCodeRuleService.getFormatOptions();
        return Result.success(options);
    }

    /**
     * 保存格式选项列表
     */
    @Operation(summary = "保存格式选项列表")
    @PutMapping("/format-options")
    public Result<Void> saveFormatOptions(@RequestBody List<Map<String, String>> options) {
        positionCodeRuleService.saveFormatOptions(options);
        return Result.success();
    }
}
