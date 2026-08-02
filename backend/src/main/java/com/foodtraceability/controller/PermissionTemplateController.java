package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PermissionTemplateDTO;
import com.foodtraceability.entity.PermissionTemplate;
import com.foodtraceability.service.PermissionTemplateService;
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
 * 权限模板控制器
 * 提供权限中心 4 种模式（集中式单店/标准连锁/大型连锁/自定义）的管理 API
 *
 * 4 种模式编码：
 * - centralized-single: 集中式单店模式（5-50人，年营收50万-500万）
 * - standard-chain: 标准连锁模式（2-5家门店，年营收500万-2000万）
 * - large-chain: 大型连锁模式（多门店，年营收2000万+）
 * - custom: 自定义（admin 全开，用户自行编辑）
 */
@RestController
@RequestMapping("/v1/permission-templates")
@Tag(name = "权限模板管理", description = "权限中心 4 种模式（集中式单店/标准连锁/大型连锁/自定义）的管理接口")
public class PermissionTemplateController {

    private static final Logger log = LoggerFactory.getLogger(PermissionTemplateController.class);

    private final PermissionTemplateService permissionTemplateService;

    public PermissionTemplateController(PermissionTemplateService permissionTemplateService) {
        this.permissionTemplateService = permissionTemplateService;
    }

    // ==================== 查询接口 ====================

    @GetMapping
    @Operation(summary = "查询所有启用的权限模板")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<List<PermissionTemplateDTO>> getAllEnabledTemplates() {
        log.info("查询所有启用的权限模板");
        return Result.success(permissionTemplateService.getAllEnabledTemplates());
    }

    @GetMapping("/system")
    @Operation(summary = "查询所有系统模板（4 种模式）")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<List<PermissionTemplateDTO>> getSystemTemplates() {
        log.info("查询所有系统模板（4 种模式）");
        return Result.success(permissionTemplateService.getSystemTemplates());
    }

    @GetMapping("/{code}")
    @Operation(summary = "根据编码查询模板")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<PermissionTemplateDTO> getTemplateByCode(
            @Parameter(description = "模板编码") @PathVariable String code) {
        log.info("根据编码查询权限模板: code={}", code);
        PermissionTemplateDTO dto = permissionTemplateService.getTemplateByCode(code);
        if (dto == null) {
            return Result.error("模板不存在: " + code);
        }
        return Result.success(dto);
    }

    @GetMapping("/entity/{code}")
    @Operation(summary = "根据编码查询完整模板实体（含 roleConfig JSON）")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<PermissionTemplate> getTemplateEntityByCode(
            @Parameter(description = "模板编码") @PathVariable String code) {
        log.info("根据编码查询完整权限模板实体: code={}", code);
        PermissionTemplate entity = permissionTemplateService.getTemplateEntityByCode(code);
        if (entity == null) {
            return Result.error("模板不存在: " + code);
        }
        return Result.success(entity);
    }

    @GetMapping("/search")
    @Operation(summary = "根据企业类型和规模查询匹配的模板")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<List<PermissionTemplateDTO>> searchTemplates(
            @Parameter(description = "企业类型") @RequestParam(required = false, defaultValue = "restaurant") String enterpriseType,
            @Parameter(description = "规模范围") @RequestParam(required = false, defaultValue = "all") String scaleRange) {
        log.info("根据企业类型和规模查询权限模板: type={}, scale={}", enterpriseType, scaleRange);
        return Result.success(permissionTemplateService.getTemplatesByEnterpriseTypeAndScale(enterpriseType, scaleRange));
    }

    // ==================== 管理接口 ====================

    @PostMapping
    @Operation(summary = "创建自定义模板")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<PermissionTemplateDTO> createTemplate(@Valid @RequestBody PermissionTemplateDTO dto) {
        log.info("创建自定义权限模板: code={}, name={}", dto.getCode(), dto.getName());
        try {
            return Result.success(permissionTemplateService.createTemplate(dto));
        } catch (Exception e) {
            log.error("创建权限模板失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<PermissionTemplateDTO> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Integer id,
            @Valid @RequestBody PermissionTemplateDTO dto) {
        log.info("更新权限模板: id={}", id);
        try {
            return Result.success(permissionTemplateService.updateTemplate(id, dto));
        } catch (Exception e) {
            log.error("更新权限模板失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板（系统模板不允许删除）")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Integer id) {
        log.info("删除权限模板: id={}", id);
        try {
            permissionTemplateService.deleteTemplate(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除权限模板失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
