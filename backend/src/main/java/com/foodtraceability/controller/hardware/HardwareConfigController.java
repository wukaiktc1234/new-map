package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.DeviceTemplate;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.service.DeviceTemplateService;
import com.foodtraceability.service.HardwareConfigService;
import com.foodtraceability.service.HardwareDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硬件设备配置管理控制器
 *
 * <p>提供硬件设备的 CRUD、设备模板管理、配置管理和连接测试等接口。
 * 注意：本控制器依赖旧版 hardware_config 表，新系统使用 devices 表
 * （由 BaseDatabaseInitializer 创建）。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理
 *             设备配置和驱动生命周期。旧控制器的配置管理逻辑应迁移到新设备管理体系。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/hardware")
@Tag(name = "硬件设备管理", description = "硬件设备配置、模板和连接管理接口")
public class HardwareConfigController {

    private static final Logger log = LoggerFactory.getLogger(HardwareConfigController.class);

    private final HardwareConfigService hardwareConfigService;
    private final HardwareDeviceService hardwareDeviceService;
    private final DeviceStatusService deviceStatusService;
    private final DeviceTemplateService deviceTemplateService;

    public HardwareConfigController(HardwareConfigService hardwareConfigService,
                                     HardwareDeviceService hardwareDeviceService,
                                     DeviceStatusService deviceStatusService,
                                     DeviceTemplateService deviceTemplateService) {
        this.hardwareConfigService = hardwareConfigService;
        this.hardwareDeviceService = hardwareDeviceService;
        this.deviceStatusService = deviceStatusService;
        this.deviceTemplateService = deviceTemplateService;
    }
    
    @GetMapping("/devices")
    @Operation(summary = "获取所有设备列表")
    @PreAuthorize("hasAuthority('hardware:config:view') or hasAuthority('*')")
    public Result<List<HardwareConfig>> getAllDevices(
        @RequestParam(required = false) Long storeId
    ) {
        log.info("✅ getAllDevices 被调用");
        return Result.success(hardwareConfigService.getAllDevices(storeId));
    }
    
    @GetMapping("/devices/{id}")
    @Operation(summary = "根据ID获取设备详情")
    @PreAuthorize("hasAuthority('hardware:config:view') or hasAuthority('*')")
    public Result<HardwareConfig> getDeviceById(@PathVariable Long id) {
        log.info("✅ getDeviceById 被调用");
        return Result.success(hardwareConfigService.getDeviceById(id));
    }
    
    @PostMapping("/devices")
    @Operation(summary = "添加新设备")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<HardwareConfig> addDevice(@RequestBody HardwareConfig config, HttpServletRequest request) {
        log.info("✅ addDevice 被调用");
        return Result.success(hardwareConfigService.saveConfig(config, request));
    }
    
    @PutMapping("/devices/{id}")
    @Operation(summary = "更新设备信息")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<HardwareConfig> updateDevice(@PathVariable Long id, @RequestBody HardwareConfig config, HttpServletRequest request) {
        log.info("✅ updateDevice 被调用");
        config.setId(id);
        return Result.success(hardwareConfigService.saveConfig(config, request));
    }
    
    @DeleteMapping("/devices/{id}")
    @Operation(summary = "删除设备")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<Boolean> deleteDevice(@PathVariable Long id, HttpServletRequest request) {
        log.info("✅ deleteDevice 被调用");
        return Result.success(hardwareConfigService.deleteConfig(id, request));
    }
    
    @GetMapping("/devices/{id}/templates")
    @Operation(summary = "获取设备模板")
    @PreAuthorize("hasAuthority('hardware:config:view') or hasAuthority('*')")
    public Result<List<DeviceTemplate>> getDeviceTemplates(
        @PathVariable Long id,
        @RequestParam(required = false) String templateType
    ) {
        log.info("✅ getDeviceTemplates 被调用");
        return Result.success(deviceTemplateService.getDeviceTemplates(id, templateType));
    }
    
    @PostMapping("/devices/{id}/templates")
    @Operation(summary = "保存设备模板")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<DeviceTemplate> saveDeviceTemplate(
        @PathVariable Long id,
        @RequestBody DeviceTemplate template,
        HttpServletRequest request
    ) {
        log.info("✅ saveDeviceTemplate 被调用");
        template.setDeviceId(id);
        return Result.success(deviceTemplateService.saveDeviceTemplate(template, request));
    }
    
    @PutMapping("/devices/{id}/templates/{templateId}")
    @Operation(summary = "更新设备模板")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<Boolean> updateDeviceTemplate(
        @PathVariable Long id,
        @PathVariable Long templateId,
        @RequestBody DeviceTemplate template,
        HttpServletRequest request
    ) {
        log.info("✅ updateDeviceTemplate 被调用");
        template.setId(templateId);
        template.setDeviceId(id);
        return Result.success(deviceTemplateService.updateDeviceTemplate(template, request));
    }
    
    @DeleteMapping("/devices/{id}/templates/{templateId}")
    @Operation(summary = "删除设备模板")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<Boolean> deleteDeviceTemplate(
        @PathVariable Long id,
        @PathVariable Long templateId,
        HttpServletRequest request
    ) {
        log.info("✅ deleteDeviceTemplate 被调用");
        return Result.success(deviceTemplateService.deleteDeviceTemplate(templateId, request));
    }
    
    @PutMapping("/devices/{id}/templates/{templateId}/default")
    @Operation(summary = "设置默认模板")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<Boolean> setDefaultTemplate(
        @PathVariable Long id,
        @PathVariable Long templateId,
        @RequestParam String templateType,
        HttpServletRequest request
    ) {
        log.info("✅ setDefaultTemplate 被调用");
        return Result.success(deviceTemplateService.setDefaultTemplate(id, templateType, templateId, request));
    }
    
    @GetMapping("/config")
    @Operation(summary = "获取硬件配置")
    @PreAuthorize("hasAuthority('hardware:config:view') or hasAuthority('*')")
    public Result<Object> getHardwareConfig(
        @RequestParam(required = false) Long storeId,
        @RequestParam(required = false) String deviceType
    ) {
        return Result.success(hardwareConfigService.getConfig(storeId, deviceType));
    }
    
    @PostMapping("/config")
    @Operation(summary = "保存硬件配置")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<HardwareConfig> saveConfig(@RequestBody HardwareConfig config, HttpServletRequest request) {
        return Result.success(hardwareConfigService.saveConfig(config, request));
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "测试设备连接")
    @PreAuthorize("hasAuthority('hardware:config:manage') or hasAuthority('*')")
    public Result<Boolean> testConnection(@RequestBody HardwareConfig config) {
        return Result.success(hardwareDeviceService.testConnection(config));
    }
    
    @GetMapping("/device/status/detailed")
    @Operation(summary = "获取详细设备状态")
    @PreAuthorize("hasAuthority('hardware:config:view') or hasAuthority('*')")
    public Result<com.foodtraceability.entity.DeviceStatus> getDetailedDeviceStatus(
        @RequestParam String deviceType
    ) {
        return Result.success(deviceStatusService.getDeviceDetailedStatus(deviceType));
    }
}