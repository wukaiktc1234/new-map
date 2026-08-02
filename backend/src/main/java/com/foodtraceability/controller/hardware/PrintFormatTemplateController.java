package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.PrintFormatTemplate;
import com.foodtraceability.service.PrintFormatTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 打印格式模板控制器
 * 提供打印格式模板的CRUD操作API
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             打印格式模板管理应集成到新设备管理体系的 DeviceTemplate 模块，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的模板管理逻辑应迁移到新设备管理体系。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/print-format")
@Tag(name = "打印格式模板管理", description = "提供打印格式模板的CRUD操作API")
public class PrintFormatTemplateController {


    public PrintFormatTemplateController(PrintFormatTemplateService printFormatTemplateService) {
        this.printFormatTemplateService = printFormatTemplateService;
    }

    private final PrintFormatTemplateService printFormatTemplateService;

    /**
     * 获取打印格式模板列表
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 模板列表
     */
    @GetMapping
    @Operation(summary = "获取打印格式模板列表")
    @PreAuthorize("hasAuthority('hardware:print-format:view') or hasAuthority('*')")
    public Result<List<PrintFormatTemplate>> getTemplates(
            @Parameter(description = "模板类型") @RequestParam(required = false) String templateType,
            @Parameter(description = "设备类型") @RequestParam(required = false) String deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        List<PrintFormatTemplate> templates = printFormatTemplateService.getTemplatesByTypeAndDevice(templateType, deviceType, storeId);
        return Result.success(templates);
    }

    /**
     * 获取默认打印格式模板
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 默认模板
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:view') or hasAuthority('*')")
    public Result<PrintFormatTemplate> getDefaultTemplate(
            @Parameter(description = "模板类型") @RequestParam String templateType,
            @Parameter(description = "设备类型") @RequestParam String deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        PrintFormatTemplate defaultTemplate = printFormatTemplateService.getDefaultTemplate(templateType, deviceType, storeId);
        return Result.success(defaultTemplate);
    }

    /**
     * 获取打印格式模板详情
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取打印格式模板详情")
    @PreAuthorize("hasAuthority('hardware:print-format:view') or hasAuthority('*')")
    public Result<PrintFormatTemplate> getTemplateById(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        PrintFormatTemplate template = printFormatTemplateService.getById(id);
        return Result.success(template);
    }

    /**
     * 保存打印格式模板
     * @param template 打印格式模板
     * @return 保存结果
     */
    @PostMapping
    @Operation(summary = "保存打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:manage') or hasAuthority('*')")
    public Result<PrintFormatTemplate> saveTemplate(
            @Parameter(description = "打印格式模板") @RequestBody PrintFormatTemplate template) {
        PrintFormatTemplate savedTemplate = printFormatTemplateService.saveTemplate(template);
        return Result.success(savedTemplate);
    }

    /**
     * 更新打印格式模板
     * @param id 模板ID
     * @param template 打印格式模板
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:manage') or hasAuthority('*')")
    public Result<PrintFormatTemplate> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Parameter(description = "打印格式模板") @RequestBody PrintFormatTemplate template) {
        template.setId(id);
        PrintFormatTemplate updatedTemplate = printFormatTemplateService.saveTemplate(template);
        return Result.success(updatedTemplate);
    }

    /**
     * 删除打印格式模板
     * @param id 模板ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:manage') or hasAuthority('*')")
    public Result<Boolean> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        boolean result = printFormatTemplateService.deleteTemplate(id);
        return Result.success(result);
    }

    /**
     * 设置默认打印格式模板
     * @param id 模板ID
     * @param templateType 模板类型
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 设置结果
     */
    @PutMapping("/{id}/default")
    @Operation(summary = "设置默认打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:manage') or hasAuthority('*')")
    public Result<Boolean> setDefaultTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Parameter(description = "模板类型") @RequestParam String templateType,
            @Parameter(description = "设备类型") @RequestParam String deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        boolean result = printFormatTemplateService.setDefaultTemplate(id, templateType, deviceType, storeId);
        return Result.success(result);
    }

    /**
     * 启用/禁用打印格式模板
     * @param id 模板ID
     * @param status 状态：0-禁用, 1-启用
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用打印格式模板")
    @PreAuthorize("hasAuthority('hardware:print-format:manage') or hasAuthority('*')")
    public Result<Boolean> updateTemplateStatus(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Parameter(description = "状态：0-禁用, 1-启用") @RequestParam Integer status) {
        boolean result = printFormatTemplateService.updateTemplateStatus(id, status);
        return Result.success(result);
    }
}
