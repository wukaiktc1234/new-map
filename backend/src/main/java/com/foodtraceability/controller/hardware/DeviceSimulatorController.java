package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.simulator.DeviceSimulator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备模拟控制器
 * 用于管理设备模拟器的启动、停止和状态查询
 *
 * 安全说明：仅在开发/测试环境（dev/test profile）激活，生产环境（prod）不暴露此接口。
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理设备驱动，
 *             模拟设备逻辑应作为 DeviceDriver 的测试实现类，而非独立控制器。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/device-simulator")
@Profile({"dev", "test"})
@Tag(name = "设备模拟器", description = "设备模拟器管理接口")
public class DeviceSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulatorController.class);


    public DeviceSimulatorController(DeviceSimulator deviceSimulator) {
        this.deviceSimulator = deviceSimulator;
    }

    private final DeviceSimulator deviceSimulator;

    /**
     * 启动所有模拟设备
     */
    @PostMapping("/start")
    @Operation(summary = "启动所有模拟设备")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> startAllSimulators() {
        log.info("收到启动所有模拟设备请求");
        try {
            deviceSimulator.startAllSimulators();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "started");
            result.put("devices", deviceSimulator.getSimulatedDevices().keySet());
            result.put("message", "设备模拟器启动成功");
            
            return Result.success(result, "设备模拟器启动成功");
        } catch (Exception e) {
            log.error("启动设备模拟器失败: {}", e.getMessage(), e);
            return Result.error("启动设备模拟器失败: " + e.getMessage());
        }
    }

    /**
     * 停止所有模拟设备
     */
    @PostMapping("/stop")
    @Operation(summary = "停止所有模拟设备")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> stopAllSimulators() {
        log.info("收到停止所有模拟设备请求");
        try {
            deviceSimulator.stopAllSimulators();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "stopped");
            result.put("message", "设备模拟器已停止");
            
            return Result.success(result, "设备模拟器已停止");
        } catch (Exception e) {
            log.error("停止设备模拟器失败: {}", e.getMessage(), e);
            return Result.error("停止设备模拟器失败: " + e.getMessage());
        }
    }

    /**
     * 获取模拟设备状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取模拟设备状态")
    @PreAuthorize("hasAuthority('device:simulator:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getSimulatorStatus() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("running", deviceSimulator.isRunning());
            result.put("devices", deviceSimulator.getSimulatedDevices());
            
            return Result.success(result, "获取模拟设备状态成功");
        } catch (Exception e) {
            log.error("获取模拟设备状态失败: {}", e.getMessage(), e);
            return Result.error("获取模拟设备状态失败: " + e.getMessage());
        }
    }

    /**
     * 启动单个模拟设备
     */
    @PostMapping("/printer/start")
    @Operation(summary = "启动打印机模拟器")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> startPrinterSimulator(
            @RequestParam(defaultValue = "9100") int port) {
        log.info("收到启动打印机模拟器请求: 端口={}", port);
        try {
            deviceSimulator.startPrinterSimulator(port);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deviceType", "PRINTER");
            result.put("port", port);
            result.put("status", "started");
            
            return Result.success(result, "打印机模拟器启动成功");
        } catch (Exception e) {
            log.error("启动打印机模拟器失败: {}", e.getMessage(), e);
            return Result.error("启动打印机模拟器失败: " + e.getMessage());
        }
    }

    /**
     * 启动扫码枪模拟器
     */
    @PostMapping("/scanner/start")
    @Operation(summary = "启动扫码枪模拟器")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> startScannerSimulator(
            @RequestParam(defaultValue = "9101") int port) {
        log.info("收到启动扫码枪模拟器请求: 端口={}", port);
        try {
            deviceSimulator.startScannerSimulator(port);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deviceType", "SCANNER");
            result.put("port", port);
            result.put("status", "started");
            
            return Result.success(result, "扫码枪模拟器启动成功");
        } catch (Exception e) {
            log.error("启动扫码枪模拟器失败: {}", e.getMessage(), e);
            return Result.error("启动扫码枪模拟器失败: " + e.getMessage());
        }
    }

    /**
     * 启动电子秤模拟器
     */
    @PostMapping("/scale/start")
    @Operation(summary = "启动电子秤模拟器")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> startScaleSimulator(
            @RequestParam(defaultValue = "9102") int port) {
        log.info("收到启动电子秤模拟器请求: 端口={}", port);
        try {
            deviceSimulator.startScaleSimulator(port);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deviceType", "SCALE");
            result.put("port", port);
            result.put("status", "started");
            
            return Result.success(result, "电子秤模拟器启动成功");
        } catch (Exception e) {
            log.error("启动电子秤模拟器失败: {}", e.getMessage(), e);
            return Result.error("启动电子秤模拟器失败: " + e.getMessage());
        }
    }

    /**
     * 启动KDS模拟器
     */
    @PostMapping("/kds/start")
    @Operation(summary = "启动KDS模拟器")
    @PreAuthorize("hasAuthority('device:simulator:manage') or hasRole('ADMIN') or hasAuthority('*')")
    public Result<Map<String, Object>> startKdsSimulator(
            @RequestParam(defaultValue = "8080") int port) {
        log.info("收到启动KDS模拟器请求: 端口={}", port);
        try {
            deviceSimulator.startKdsSimulator(port);
            
            Map<String, Object> result = new HashMap<>();
            result.put("deviceType", "KDS");
            result.put("port", port);
            result.put("status", "started");
            
            return Result.success(result, "KDS模拟器启动成功");
        } catch (Exception e) {
            log.error("启动KDS模拟器失败: {}", e.getMessage(), e);
            return Result.error("启动KDS模拟器失败: " + e.getMessage());
        }
    }
}
