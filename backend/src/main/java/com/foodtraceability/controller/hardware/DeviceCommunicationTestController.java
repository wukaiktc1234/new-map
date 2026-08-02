package com.foodtraceability.controller.hardware;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.service.DeviceCommunicationTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 设备通信测试控制器
 * 提供设备通信测试和诊断接口
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理设备连接、
 *             心跳检测和通信诊断（通过 {@link com.foodtraceability.util.ConnectionHeartbeatManager}）。
 *             旧控制器的通信测试逻辑应下沉为 DeviceDriver 体系的能力，被新系统调用。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/device-communication")
@Tag(name = "设备通信测试", description = "设备通信测试和诊断接口")
public class DeviceCommunicationTestController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceCommunicationTestController.class);
    private final DeviceCommunicationTestService communicationTestService;

    /**
     * 测试设备通信
     */
    @PostMapping("/test")
    @Operation(summary = "测试设备通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testCommunication(@RequestBody HardwareConfig config) {
        log.info("测试设备通信: 设备类型={} 设备名称={}", config.getDeviceType(), config.getDeviceName());
        try {
            TestResult result = communicationTestService.testCommunication(config);
            return Result.success(result, "设备通信测试完成");
        } catch (Exception e) {
            log.error("测试设备通信失败: {}", e.getMessage(), e);
            return Result.error("测试设备通信失败: " + e.getMessage());
        }
    }

    /**
     * 测试打印机通信
     */
    @PostMapping("/test/printer")
    @Operation(summary = "测试打印机通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testPrinterCommunication(@RequestBody HardwareConfig config) {
        log.info("测试打印机通信: 设备名称={}", config.getDeviceName());
        try {
            TestResult result = communicationTestService.testPrinterCommunication(config);
            return Result.success(result, "打印机通信测试完成");
        } catch (Exception e) {
            log.error("测试打印机通信失败: {}", e.getMessage(), e);
            return Result.error("测试打印机通信失败: " + e.getMessage());
        }
    }

    /**
     * 测试扫码枪通信
     */
    @PostMapping("/test/scanner")
    @Operation(summary = "测试扫码枪通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testScannerCommunication(@RequestBody HardwareConfig config) {
        log.info("测试扫码枪通信: 设备名称={}", config.getDeviceName());
        try {
            TestResult result = communicationTestService.testScannerCommunication(config);
            return Result.success(result, "扫码枪通信测试完成");
        } catch (Exception e) {
            log.error("测试扫码枪通信失败: {}", e.getMessage(), e);
            return Result.error("测试扫码枪通信失败: " + e.getMessage());
        }
    }

    /**
     * 测试电子秤通信
     */
    @PostMapping("/test/scale")
    @Operation(summary = "测试电子秤通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testScaleCommunication(@RequestBody HardwareConfig config) {
        log.info("测试电子秤通信: 设备名称={}", config.getDeviceName());
        try {
            TestResult result = communicationTestService.testScaleCommunication(config);
            return Result.success(result, "电子秤通信测试完成");
        } catch (Exception e) {
            log.error("测试电子秤通信失败: {}", e.getMessage(), e);
            return Result.error("测试电子秤通信失败: " + e.getMessage());
        }
    }

    /**
     * 测试摄像头通信
     */
    @PostMapping("/test/camera")
    @Operation(summary = "测试摄像头通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testCameraCommunication(@RequestBody HardwareConfig config) {
        log.info("测试摄像头通信: 设备名称={}", config.getDeviceName());
        try {
            TestResult result = communicationTestService.testCameraCommunication(config);
            return Result.success(result, "摄像头通信测试完成");
        } catch (Exception e) {
            log.error("测试摄像头通信失败: {}", e.getMessage(), e);
            return Result.error("测试摄像头通信失败: " + e.getMessage());
        }
    }

    /**
     * 测试KDS通信
     */
    @PostMapping("/test/kds")
    @Operation(summary = "测试KDS通信")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<TestResult> testKdsCommunication(@RequestBody HardwareConfig config) {
        log.info("测试KDS通信: 设备名称={}", config.getDeviceName());
        try {
            TestResult result = communicationTestService.testKdsCommunication(config);
            return Result.success(result, "KDS通信测试完成");
        } catch (Exception e) {
            log.error("测试KDS通信失败: {}", e.getMessage(), e);
            return Result.error("测试KDS通信失败: " + e.getMessage());
        }
    }

    /**
     * 发送测试打印
     */
    @PostMapping("/test-print")
    @Operation(summary = "发送测试打印")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> sendTestPrint(@RequestBody HardwareConfig config) {
        log.info("发送测试打印: 设备名称={}", config.getDeviceName());
        try {
            Map<String, Object> result = communicationTestService.sendTestPrint(config);
            return Result.success(result, "测试打印完成");
        } catch (Exception e) {
            log.error("发送测试打印失败: {}", e.getMessage(), e);
            return Result.error("发送测试打印失败: " + e.getMessage());
        }
    }

    /**
     * 发送测试扫描
     */
    @PostMapping("/test-scan")
    @Operation(summary = "发送测试扫描")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> sendTestScan(@RequestBody HardwareConfig config) {
        log.info("发送测试扫描: 设备名称={}", config.getDeviceName());
        try {
            Map<String, Object> result = communicationTestService.sendTestScan(config);
            return Result.success(result, "测试扫描完成");
        } catch (Exception e) {
            log.error("发送测试扫描失败: {}", e.getMessage(), e);
            return Result.error("发送测试扫描失败: " + e.getMessage());
        }
    }

    /**
     * 发送测试称重
     */
    @PostMapping("/test-weigh")
    @Operation(summary = "发送测试称重")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> sendTestWeigh(@RequestBody HardwareConfig config) {
        log.info("发送测试称重: 设备名称={}", config.getDeviceName());
        try {
            Map<String, Object> result = communicationTestService.sendTestWeigh(config);
            return Result.success(result, "测试称重完成");
        } catch (Exception e) {
            log.error("发送测试称重失败: {}", e.getMessage(), e);
            return Result.error("发送测试称重失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备诊断信息
     */
    @PostMapping("/diagnostics")
    @Operation(summary = "获取设备诊断信息")
    @PreAuthorize("hasAuthority('device:communication:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> getDiagnostics(@RequestBody HardwareConfig config) {
        log.info("获取设备诊断信息: 设备名称={}", config.getDeviceName());
        try {
            Map<String, Object> diagnostics = communicationTestService.getDiagnostics(config);
            return Result.success(diagnostics, "获取设备诊断信息成功");
        } catch (Exception e) {
            log.error("获取设备诊断信息失败: {}", e.getMessage(), e);
            return Result.error("获取设备诊断信息失败: " + e.getMessage());
        }
    }

    /**
     * 快速测试网络设备
     */
    @GetMapping("/quick-test/network")
    @Operation(summary = "快速测试网络设备")
    @PreAuthorize("hasAuthority('device:communication:view') or hasAuthority('*')")
    public Result<TestResult> quickTestNetworkDevice(@Parameter(description = "IP地址") @RequestParam String ipAddress, @Parameter(description = "端口") @RequestParam(defaultValue = "9100") int port, @Parameter(description = "设备类型") @RequestParam(defaultValue = "PRINTER") String deviceType) {
        log.info("快速测试网络设备: IP={} 端口={} 设备类型={}", ipAddress, port, deviceType);
        try {
            HardwareConfig config = new HardwareConfig();
            config.setDeviceType(deviceType);
            config.setConnectionType("NETWORK");
            config.setIpAddress(ipAddress);
            config.setPort(String.valueOf(port));
            config.setDeviceName("QuickTest-" + deviceType);
            TestResult result = communicationTestService.testCommunication(config);
            return Result.success(result, "快速测试完成");
        } catch (Exception e) {
            log.error("快速测试网络设备失败: {}", e.getMessage(), e);
            return Result.error("快速测试失败: " + e.getMessage());
        }
    }

    public DeviceCommunicationTestController(final DeviceCommunicationTestService communicationTestService) {
        this.communicationTestService = communicationTestService;
    }
}
