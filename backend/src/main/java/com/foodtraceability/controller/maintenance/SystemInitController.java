package com.foodtraceability.controller.maintenance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.service.SystemInitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统初始化控制器
 * 处理系统初始化向导相关的API请求
 */
@RestController
@RequestMapping("/v1/system/init")
@Tag(name = "系统初始化", description = "系统初始化向导相关接口")
public class SystemInitController {

    private static final Logger log = LoggerFactory.getLogger(SystemInitController.class);

    public SystemInitController(SystemInitService systemInitService) {
        this.systemInitService = systemInitService;
    }

    private final SystemInitService systemInitService;

    /**
     * 检查系统是否已初始化
     */
    @GetMapping("/status/check")
    @Operation(summary = "检查系统是否已初始化", description = "检查系统是否已完成初始化向导")
    public Result<Boolean> checkInitialized() {
        boolean initialized = systemInitService.isSystemInitialized();
        return Result.success(initialized);
    }

    /**
     * 获取当前初始化状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取初始化状态", description = "获取当前系统初始化向导的进度状态")
    public Result<SystemInitStatusDTO> getInitStatus() {
        SystemInitStatusDTO status = systemInitService.getInitStatus();
        return Result.success(status);
    }

    /**
     * 开始初始化流程
     */
    @PostMapping("/start")
    @Operation(summary = "开始初始化", description = "开始系统初始化向导流程")
    public Result<SystemInitStatusDTO> startInit() {
        SystemInitStatusDTO status = systemInitService.startInit();
        return Result.success(status);
    }

    /**
     * 保存企业信息
     */
    @PostMapping("/enterprise")
    @Operation(summary = "保存企业信息", description = "保存企业基本信息")
    public Result<Boolean> saveEnterpriseInfo(@Valid @RequestBody EnterpriseInfoDTO enterpriseInfo) {
        boolean success = systemInitService.saveEnterpriseInfo(enterpriseInfo);
        return Result.success(success);
    }

    /**
     * 保存组织架构
     */
    @PostMapping("/organization")
    @Operation(summary = "保存组织架构", description = "保存门店、部门、职位等组织架构信息")
    public Result<Boolean> saveOrganization(@RequestBody OrganizationInfoDTO organizationInfo) {
        boolean success = systemInitService.saveOrganization(organizationInfo);
        return Result.success(success);
    }

    /**
     * 获取权限模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取权限模板列表", description = "获取所有可用的权限配置模板")
    public Result<List<PermissionTemplateDTO>> getPermissionTemplates() {
        List<PermissionTemplateDTO> templates = systemInitService.getPermissionTemplates();
        return Result.success(templates);
    }

    /**
     * 获取推荐的权限模板
     */
    @GetMapping("/templates/recommended")
    @Operation(summary = "获取推荐的权限模板", description = "根据企业类型和规模获取推荐的权限配置模板")
    public Result<List<PermissionTemplateDTO>> getRecommendedTemplates(
            @RequestParam String enterpriseType,
            @RequestParam String scale) {
        List<PermissionTemplateDTO> templates = systemInitService.getRecommendedTemplates(enterpriseType, scale);
        return Result.success(templates);
    }

    /**
     * 应用权限模板
     */
    @PostMapping("/templates/{templateId}/apply")
    @Operation(summary = "应用权限模板", description = "应用指定的权限配置模板")
    public Result<Boolean> applyPermissionTemplate(@PathVariable Integer templateId) {
        boolean success = systemInitService.applyPermissionTemplate(templateId);
        return Result.success(success);
    }

    /**
     * 自定义角色配置
     */
    @PostMapping("/roles/configure")
    @Operation(summary = "自定义角色配置", description = "自定义角色和权限配置")
    public Result<Boolean> configureRoles(@RequestBody List<RoleConfigDTO> roleConfigs) {
        boolean success = systemInitService.configureRoles(roleConfigs);
        return Result.success(success);
    }

    /**
     * 完成初始化
     */
    @PostMapping("/complete")
    @Operation(summary = "完成初始化", description = "完成系统初始化向导")
    public Result<Boolean> completeInit() {
        boolean success = systemInitService.completeInit();
        return Result.success(success);
    }

    /**
     * 跳过初始化向导
     */
    @PostMapping("/skip")
    @Operation(summary = "跳过初始化", description = "跳过系统初始化向导")
    public Result<Boolean> skipInit() {
        boolean success = systemInitService.skipInit();
        return Result.success(success);
    }

    /**
     * 重置初始化状态（仅用于测试）
     */
    @PostMapping("/reset")
    @Operation(summary = "重置初始化状态", description = "重置系统初始化状态（仅用于测试环境）")
    public Result<Boolean> resetInitStatus() {
        boolean success = systemInitService.resetInitStatus();
        return Result.success(success);
    }

    /**
     * 初始化公司信息
     */
    @PostMapping("/company")
    @Operation(summary = "初始化公司信息", description = "系统首次使用时初始化公司基础信息及默认组织架构")
    public Result<Boolean> initializeCompany(@Valid @RequestBody CompanyInitDTO companyInitDTO) {
        try {
            boolean success = systemInitService.initializeCompany(companyInitDTO);
            return Result.success(success, "公司初始化成功");
        } catch (Exception e) {
            log.error("公司初始化失败", e);
            return Result.error("初始化失败: " + e.getMessage());
        }
    }
}
