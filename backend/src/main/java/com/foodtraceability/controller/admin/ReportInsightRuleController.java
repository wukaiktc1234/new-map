package com.foodtraceability.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.report.ReportInsightRule;
import com.foodtraceability.mapper.ReportInsightRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能洞察规则管理控制器
 * 提供洞察规则的增删改查接口（管理员专用）
 */
@RestController
@RequestMapping("/v1/admin/report-insight-rules")
@Tag(name = "智能洞察规则管理", description = "报表智能洞察规则的管理接口（管理员专用）")
@PreAuthorize("hasAuthority('finance:report:config')")
public class ReportInsightRuleController {

    private static final Logger logger = LoggerFactory.getLogger(ReportInsightRuleController.class);

    private final ReportInsightRuleMapper reportInsightRuleMapper;

    public ReportInsightRuleController(ReportInsightRuleMapper reportInsightRuleMapper) {
        this.reportInsightRuleMapper = reportInsightRuleMapper;
    }

    /**
     * 分页查询规则列表
     * @param page 页码
     * @param size 每页大小
     * @param reportScope 报表范围筛选
     * @param isEnabled 启用状态筛选
     * @return 规则分页列表
     */
    @GetMapping
    @Operation(summary = "分页查询规则列表", description = "获取智能洞察规则的分页列表")
    public Result<IPage<ReportInsightRule>> listRules(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "报表范围：0全部 1日报 2周报 3月报 4季报 5年报 6利润") @RequestParam(required = false) Integer reportScope,
            @Parameter(description = "启用状态") @RequestParam(required = false) Boolean isEnabled) {
        logger.info("查询智能洞察规则列表，page={}, size={}, reportScope={}, isEnabled={}", page, size, reportScope, isEnabled);

        Page<ReportInsightRule> pageParam = new Page<>(page, size);
        QueryWrapper<ReportInsightRule> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("priority");

        if (reportScope != null) {
            queryWrapper.and(wrapper -> wrapper.eq("report_scope", 0).or().eq("report_scope", reportScope));
        }
        if (isEnabled != null) {
            queryWrapper.eq("is_enabled", isEnabled);
        }

        IPage<ReportInsightRule> result = reportInsightRuleMapper.selectPage(pageParam, queryWrapper);
        return Result.success(result);
    }

    /**
     * 根据ID获取规则详情
     * @param ruleId 规则ID
     * @return 规则详情
     */
    @GetMapping("/{ruleId}")
    @Operation(summary = "获取规则详情", description = "根据ID获取智能洞察规则详情")
    public Result<ReportInsightRule> getRule(
            @Parameter(description = "规则ID") @PathVariable Long ruleId) {
        logger.info("获取智能洞察规则详情，ruleId={}", ruleId);
        ReportInsightRule rule = reportInsightRuleMapper.selectById(ruleId);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }

    /**
     * 创建规则
     * @param rule 规则实体
     * @return 创建后的规则
     */
    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的智能洞察规则")
    public Result<ReportInsightRule> createRule(@Valid @RequestBody ReportInsightRule rule) {
        logger.info("创建智能洞察规则，ruleName={}", rule.getRuleName());
        reportInsightRuleMapper.insert(rule);
        return Result.success(rule);
    }

    /**
     * 更新规则
     * @param ruleId 规则ID
     * @param rule 规则实体
     * @return 更新后的规则
     */
    @PutMapping("/{ruleId}")
    @Operation(summary = "更新规则", description = "更新智能洞察规则")
    public Result<ReportInsightRule> updateRule(
            @Parameter(description = "规则ID") @PathVariable Long ruleId,
            @Valid @RequestBody ReportInsightRule rule) {
        logger.info("更新智能洞察规则，ruleId={}", ruleId);
        rule.setRuleId(ruleId);
        reportInsightRuleMapper.updateById(rule);
        return Result.success(rule);
    }

    /**
     * 删除规则
     * @param ruleId 规则ID
     * @return 操作结果
     */
    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除规则", description = "删除智能洞察规则")
    public Result<Void> deleteRule(
            @Parameter(description = "规则ID") @PathVariable Long ruleId) {
        logger.info("删除智能洞察规则，ruleId={}", ruleId);
        reportInsightRuleMapper.deleteById(ruleId);
        return Result.success();
    }

    /**
     * 批量启用/禁用规则
     * @param ruleIds 规则ID列表
     * @param enabled 启用状态
     * @return 操作结果
     */
    @PutMapping("/batch-enable")
    @Operation(summary = "批量启用/禁用规则", description = "批量设置规则的启用状态")
    public Result<Void> batchEnableRules(
            @RequestBody List<Long> ruleIds,
            @RequestParam Boolean enabled) {
        logger.info("批量设置规则启用状态，ruleIds={}, enabled={}", ruleIds, enabled);
        for (Long ruleId : ruleIds) {
            ReportInsightRule rule = new ReportInsightRule();
            rule.setRuleId(ruleId);
            rule.setIsEnabled(enabled);
            reportInsightRuleMapper.updateById(rule);
        }
        return Result.success();
    }
}
