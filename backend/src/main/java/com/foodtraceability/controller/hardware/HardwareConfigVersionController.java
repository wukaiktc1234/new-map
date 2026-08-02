package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.HardwareConfigVersion;
import com.foodtraceability.service.HardwareConfigVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备配置版本管理控制器
 * 提供设备配置版本的创建、查询、回滚和删除等API接口
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             设备配置版本管理应集成到新设备管理体系的 DeviceDriverConfig 模块，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的版本管理逻辑应迁移到新设备管理体系。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/hardware/version")
@Tag(name = "设备配置版本管理")
public class HardwareConfigVersionController {
    

    public HardwareConfigVersionController(HardwareConfigVersionService hardwareConfigVersionService) {
        this.hardwareConfigVersionService = hardwareConfigVersionService;
    }

    private final HardwareConfigVersionService hardwareConfigVersionService;
    
    /**
     * 获取设备配置的版本列表
     * @param hardwareId 硬件配置ID
     * @return 版本列表
     */
    @GetMapping
    @Operation(summary = "获取设备配置版本列表")
    @PreAuthorize("hasAuthority('hardware:view') or hasAuthority('*')")
    public Result<List<HardwareConfigVersion>> getVersions(@RequestParam Long hardwareId) {
        return Result.success(hardwareConfigVersionService.getVersionsByHardwareId(hardwareId));
    }
    
    /**
     * 获取版本详情
     * @param versionId 版本ID
     * @return 版本详情
     */
    @GetMapping("/{versionId}")
    @Operation(summary = "获取设备配置版本详情")
    @PreAuthorize("hasAuthority('hardware:view')")
    public Result<HardwareConfigVersion> getVersion(@PathVariable Long versionId) {
        return Result.success(hardwareConfigVersionService.getVersionById(versionId));
    }
    
    /**
     * 回滚到指定版本
     * @param versionId 版本ID
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 回滚后的设备配置对象
     */
    @PostMapping("/{versionId}/rollback")
    @Operation(summary = "回滚到指定版本")
    @PreAuthorize("hasAuthority('hardware:edit') or hasAuthority('*')")
    public Result<HardwareConfig> rollbackToVersion(@PathVariable Long versionId, HttpServletRequest request) {
        return Result.success(hardwareConfigVersionService.rollbackToVersion(versionId, request));
    }
    
    /**
     * 删除指定版本
     * @param versionId 版本ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{versionId}")
    @Operation(summary = "删除设备配置版本")
    @PreAuthorize("hasAuthority('hardware:delete') or hasAuthority('*')")
    public Result<Boolean> deleteVersion(@PathVariable Long versionId) {
        return Result.success(hardwareConfigVersionService.deleteVersion(versionId));
    }
}