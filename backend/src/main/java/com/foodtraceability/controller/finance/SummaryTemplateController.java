package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.SummaryTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 摘要模板控制器
 * 管理凭证录入时的常用摘要模板
 */
@Tag(name = "摘要模板管理", description = "凭证录入常用摘要模板的增删改查及搜索")
@RestController
@RequestMapping("/v1/finance/summary-templates")
public class SummaryTemplateController {

    private final SummaryTemplateService summaryTemplateService;

    public SummaryTemplateController(SummaryTemplateService summaryTemplateService) {
        this.summaryTemplateService = summaryTemplateService;
    }

    @Operation(summary = "创建摘要模板", description = "新增凭证摘要模板")
    @PostMapping
    public Result<SummaryTemplateVO> create(@Valid @RequestBody SummaryTemplateCreateDTO dto) {
        return Result.success(summaryTemplateService.create(dto));
    }

    @Operation(summary = "更新摘要模板")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody SummaryTemplateCreateDTO dto) {
        return Result.success(summaryTemplateService.update(id, dto));
    }

    @Operation(summary = "删除摘要模板", description = "逻辑删除指定摘要模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(summaryTemplateService.delete(id));
    }

    @Operation(summary = "分页查询摘要模板", description = "支持按分类等条件筛选")
    @GetMapping
    public Result<IPage<SummaryTemplateVO>> getPage(SummaryTemplateQueryDTO query) {
        return Result.success(summaryTemplateService.getPage(query));
    }

    @Operation(summary = "搜索摘要模板", description = "按关键词搜索摘要模板，用于凭证录入时的快速选择")
    @GetMapping("/search")
    public Result<List<SummaryTemplateVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(summaryTemplateService.search(keyword, limit));
    }

    @Operation(summary = "增加使用次数", description = "记录摘要模板被使用，统计使用频次")
    @PutMapping("/{id}/increment-usage")
    public Result<Boolean> incrementUsageCount(@PathVariable Long id) {
        return Result.success(summaryTemplateService.incrementUsageCount(id));
    }
}
