package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.service.HardwareDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 硬件设备操作控制器
 *
 * <p>提供扫码枪扫描、打印机打印追溯码、摄像头拍摄出餐照片等硬件设备操作接口。
 * 通过 HardwareDeviceService 委托执行具体业务逻辑。</p>
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.ScannerDriver}（扫码）、
 *             {@link com.foodtraceability.driver.PrinterDriver}（打印）、
 *             {@link com.foodtraceability.driver.CameraDriver}（拍照）等驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理。
 *             旧控制器的硬件集成逻辑应下沉为 DeviceDriver 实现类，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/hardware/device")
@Tag(name = "硬件设备操作", description = "硬件设备扫描、打印、拍照等操作接口")
public class HardwareDeviceController {

    private static final Logger log = LoggerFactory.getLogger(HardwareDeviceController.class);

    public HardwareDeviceController(HardwareDeviceService hardwareDeviceService) {
        this.hardwareDeviceService = hardwareDeviceService;
    }

    private final HardwareDeviceService hardwareDeviceService;
    
    @PostMapping("/scan")
    @Operation(summary = "扫码枪扫描库存编码")
    @PreAuthorize("hasAuthority('hardware:scan') or hasAuthority('*')")
    public Result<Boolean> scanInventoryCode(@RequestParam String inventoryCode) {
        return Result.success(hardwareDeviceService.scanInventoryCode(inventoryCode));
    }
    
    @PostMapping("/print")
    @Operation(summary = "打印机打印追溯码标签")
    @PreAuthorize("hasAuthority('hardware:print') or hasAuthority('*')")
    public Result<Boolean> printTraceabilityLabel(
        @RequestParam String traceabilityCode,
        @RequestParam String productName
    ) {
        return Result.success(hardwareDeviceService.printTraceabilityLabel(traceabilityCode, productName));
    }
    
    @PostMapping("/capture")
    @Operation(summary = "摄像头拍摄出餐照片")
    @PreAuthorize("hasAuthority('hardware:capture') or hasAuthority('*')")
    public Result<Boolean> captureMealPhoto(
        @RequestParam String orderId,
        @RequestParam String traceabilityCode
    ) {
        return Result.success(hardwareDeviceService.captureMealPhoto(orderId, traceabilityCode));
    }
    
    @PostMapping("/test-connection")
    @Operation(summary = "测试设备连接")
    @PreAuthorize("hasAuthority('hardware:device:manage') or hasAuthority('*')")
    public Result<Boolean> testConnection(@RequestBody HardwareConfig config) {
        return Result.success(hardwareDeviceService.testConnection(config));
    }

    @PostMapping("/test-connection-detailed")
    @Operation(summary = "测试设备连接（详细）")
    @PreAuthorize("hasAuthority('hardware:device:manage') or hasAuthority('*')")
    public Result<TestResult> testConnectionDetailed(@RequestBody HardwareConfig config) {
        return Result.success(hardwareDeviceService.testConnectionDetailed(config));
    }

    @GetMapping("/status")
    @Operation(summary = "获取设备状态")
    @PreAuthorize("hasAuthority('hardware:device:view') or hasAuthority('*')")
    public Result<Boolean> getDeviceStatus(@RequestParam String deviceType) {
        log.info("设备状态API被调用");
        System.out.println("设备类型: " + deviceType);
        if (deviceType == null || deviceType.trim().isEmpty()) {
            System.out.println("缺少必填参数：deviceType");
            return Result.error(400, "缺少必填参数：deviceType");
        }
        Boolean status = hardwareDeviceService.getDeviceStatus(deviceType);
        System.out.println("设备状态: " + status);
        return Result.success(status);
    }
}