package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ContractTemplate;
import com.foodtraceability.service.ContractTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 合同模板控制器
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/v1/hr/contract-template")
@Tag(name = "合同模板管理", description = "合同模板的增删改查和预览")
public class ContractTemplateController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContractTemplateController.class);

    public ContractTemplateController(ContractTemplateService templateService) {
        this.templateService = templateService;
    }

    private final ContractTemplateService templateService;

    @PostMapping
    @Operation(summary = "创建合同模板")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<ContractTemplate> createTemplate(@RequestBody ContractTemplate template) {
        log.info("创建合同模板：{}", template.getTemplateName());
        // 检查模板编码是否重复
        ContractTemplate existing = templateService.getByTemplateCode(template.getTemplateCode());
        if (existing != null) {
            return Result.error("模板编码已存在");
        }
        templateService.save(template);
        return Result.success(template);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新合同模板")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<ContractTemplate> updateTemplate(@PathVariable Long id, @RequestBody ContractTemplate template) {
        log.info("更新合同模板：{}", id);
        ContractTemplate existing = templateService.getById(id);
        if (existing == null) {
            return Result.error(404, "模板不存在");
        }
        template.setId(id);
        templateService.updateById(template);
        return Result.success(template);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除合同模板")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        log.info("删除合同模板：{}", id);
        ContractTemplate existing = templateService.getById(id);
        if (existing == null) {
            return Result.error(404, "模板不存在");
        }
        templateService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    @PreAuthorize("hasAuthority('hr:contract-template:view') or hasAuthority('*')")
    public Result<ContractTemplate> getTemplate(@PathVariable Long id) {
        ContractTemplate template = templateService.getById(id);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        return Result.success(template);
    }

    @GetMapping("/list")
    @Operation(summary = "获取模板列表")
    @PreAuthorize("hasAuthority('hr:contract-template:view') or hasAuthority('*')")
    public Result<Map<String, Object>> listTemplates(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page, @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size, @Parameter(description = "合同类型") @RequestParam(required = false) String contractType, @Parameter(description = "状态") @RequestParam(required = false) String status, @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        try {
            Page<ContractTemplate> pageParam = new Page<>(page, size);
            LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
            if (contractType != null && !contractType.isEmpty()) {
                wrapper.eq(ContractTemplate::getContractType, contractType);
            }
            if (status != null && !status.isEmpty()) {
                wrapper.eq(ContractTemplate::getStatus, status);
            }
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.like(ContractTemplate::getTemplateName, keyword).or().like(ContractTemplate::getTemplateCode, keyword);
            }
            wrapper.orderByDesc(ContractTemplate::getCreateTime);
            IPage<ContractTemplate> pageResult = templateService.page(pageParam, wrapper);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("records", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            return Result.success(result);
        } catch (Exception e) {
            // 容错：表不存在或字段不匹配时返回空列表，避免阻塞页面加载
            log.warn("获取合同模板列表失败，返回空列表：{}", e.getMessage());
            Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("records", java.util.Collections.emptyList());
            emptyResult.put("total", 0L);
            return Result.success(emptyResult);
        }
    }

    @GetMapping("/active")
    @Operation(summary = "获取启用的模板列表")
    @PreAuthorize("hasAuthority('hr:contract-template:view') or hasAuthority('*')")
    public Result<List<ContractTemplate>> getActiveTemplates(@Parameter(description = "合同类型") @RequestParam(required = false) String contractType) {
        List<ContractTemplate> templates;
        if (contractType != null && !contractType.isEmpty()) {
            templates = templateService.getActiveTemplatesByType(contractType);
        } else {
            LambdaQueryWrapper<ContractTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ContractTemplate::getStatus, ContractTemplate.STATUS_ACTIVE).orderByDesc(ContractTemplate::getCreateTime);
            templates = templateService.list(wrapper);
        }
        return Result.success(templates);
    }

    @PostMapping("/{id}/preview")
    @Operation(summary = "预览模板")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<String> previewTemplate(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> variables) {
        try {
            String content = templateService.previewTemplate(id, variables);
            return Result.success(content);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "复制模板")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<ContractTemplate> duplicateTemplate(@PathVariable Long id) {
        try {
            ContractTemplate newTemplate = templateService.duplicateTemplate(id);
            return Result.success(newTemplate);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新模板状态")
    @PreAuthorize("hasAuthority('hr:contract-template:manage') or hasAuthority('*')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        ContractTemplate template = templateService.getById(id);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        template.setStatus(status);
        templateService.updateById(template);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取模板统计")
    @PreAuthorize("hasAuthority('hr:contract-template:view') or hasAuthority('*')")
    public Result<Map<String, Integer>> getStatistics() {
        return Result.success(templateService.getStatistics());
    }
}
