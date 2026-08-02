package com.foodtraceability.controller.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PrintTemplateCreateDTO;
import com.foodtraceability.dto.PrintTemplateUpdateDTO;
import com.foodtraceability.dto.PrintTemplateVO;
import com.foodtraceability.service.device.PrintTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 打印模板控制器
 * 提供打印模板的CRUD、渲染、默认设置等API
 */
@RestController
@RequestMapping("/v1/print-templates")
@Tag(name = "打印模板管理", description = "打印模板相关接口")
public class PrintTemplateController {

    private static final Logger log = LoggerFactory.getLogger(PrintTemplateController.class);

    private final PrintTemplateService printTemplateService;

    public PrintTemplateController(PrintTemplateService printTemplateService) {
        this.printTemplateService = printTemplateService;
    }

    @PostMapping
    @Operation(summary = "创建打印模板")
    @PreAuthorize("hasAuthority('device:print-template:manage') or hasAuthority('*')")
    public Result<PrintTemplateVO> createTemplate(@Valid @RequestBody PrintTemplateCreateDTO dto) {
        log.info("创建打印模板: templateName={}, type={}", dto.getTemplateName(), dto.getTemplateType());
        return printTemplateService.createTemplate(dto);
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "更新打印模板")
    @PreAuthorize("hasAuthority('device:print-template:manage') or hasAuthority('*')")
    public Result<PrintTemplateVO> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody PrintTemplateUpdateDTO dto) {
        log.info("更新打印模板: templateId={}", templateId);
        return printTemplateService.updateTemplate(templateId, dto);
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除打印模板")
    @PreAuthorize("hasAuthority('device:print-template:manage') or hasAuthority('*')")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        log.info("删除打印模板: templateId={}", templateId);
        return printTemplateService.deleteTemplate(templateId);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "查询打印模板详情")
    @PreAuthorize("hasAuthority('device:print-template:view') or hasAuthority('*')")
    public Result<PrintTemplateVO> getTemplateById(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        log.info("查询打印模板: templateId={}", templateId);
        return printTemplateService.getTemplateById(templateId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询打印模板列表")
    @PreAuthorize("hasAuthority('device:print-template:view') or hasAuthority('*')")
    public Result<IPage<PrintTemplateVO>> getTemplatePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "模板类型") @RequestParam(required = false) Integer templateType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {

        Page<?> pageParam = new Page<>(page, size);
        return printTemplateService.getTemplatePage(pageParam, templateType, storeId);
    }

    @GetMapping("/list")
    @Operation(summary = "查询启用的打印模板列表")
    @PreAuthorize("hasAuthority('device:print-template:view') or hasAuthority('*')")
    public Result<List<PrintTemplateVO>> getEnabledTemplates(
            @Parameter(description = "模板类型") @RequestParam(required = false) Integer templateType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        return printTemplateService.getEnabledTemplates(templateType, storeId);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认模板")
    @PreAuthorize("hasAuthority('device:print-template:view') or hasAuthority('*')")
    public Result<PrintTemplateVO> getDefaultTemplate(
            @Parameter(description = "模板类型") @RequestParam Integer templateType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        return printTemplateService.getDefaultTemplate(templateType, storeId);
    }

    @PutMapping("/{templateId}/default")
    @Operation(summary = "设置为默认模板")
    @PreAuthorize("hasAuthority('device:print-template:manage') or hasAuthority('*')")
    public Result<Void> setAsDefault(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        log.info("设置默认模板: templateId={}", templateId);
        return printTemplateService.setAsDefault(templateId);
    }

    @PostMapping("/{templateId}/render")
    @Operation(summary = "渲染模板内容")
    @PreAuthorize("hasAuthority('device:print-template:manage') or hasAuthority('*')")
    public Result<String> renderTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestBody Map<String, Object> variables) {
        log.info("渲染模板: templateId={}, 变量数量={}", templateId, variables.size());
        return printTemplateService.renderTemplate(templateId, variables);
    }
}
