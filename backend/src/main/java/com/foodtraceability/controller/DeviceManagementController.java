package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DevicePrintRequest;
import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.service.DevicePrintService;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.service.HardwareConfigService;
import com.foodtraceability.service.HardwareDeviceService;
import com.foodtraceability.util.PrintTaskQueueManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统一设备管理API控制器
 * 提供统一的设备管理接口，供其他菜单入口调用
 */
@RestController
@RequestMapping("/v1/device-management")
@Tag(name = "统一设备管理API", description = "提供统一的设备管理接口，供其他菜单入口调用")
public class DeviceManagementController {


    public DeviceManagementController(HardwareConfigService hardwareConfigService, DeviceStatusService deviceStatusService, HardwareDeviceService hardwareDeviceService, DevicePrintService devicePrintService, PrintTaskQueueManager printTaskQueueManager) {
        this.hardwareConfigService = hardwareConfigService;
        this.deviceStatusService = deviceStatusService;
        this.hardwareDeviceService = hardwareDeviceService;
        this.devicePrintService = devicePrintService;
        this.printTaskQueueManager = printTaskQueueManager;
    }

    private final HardwareConfigService hardwareConfigService;

    private final DeviceStatusService deviceStatusService;

    private final HardwareDeviceService hardwareDeviceService;

    private final DevicePrintService devicePrintService;
    
    private final PrintTaskQueueManager printTaskQueueManager;

    /**
     * 获取设备配置
     * @param storeId 门店ID
     * @param deviceType 设备类型
     * @return 设备配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取设备配置")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<HardwareConfig> getDeviceConfig(
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "设备类型") @RequestParam String deviceType) {
        return Result.success(hardwareConfigService.getConfig(storeId, deviceType));
    }

    /**
     * 获取设备状态
     * @param deviceType 设备类型
     * @return 设备状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取设备状态")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<DeviceStatus> getDeviceStatus(
            @Parameter(description = "设备类型") @RequestParam String deviceType) {
        return Result.success(deviceStatusService.getDeviceDetailedStatus(deviceType));
    }

    /**
     * 测试设备连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    @PostMapping("/test-connection")
    @Operation(summary = "测试设备连接")
    @PreAuthorize("hasAuthority('device:management:manage') or hasAuthority('*')")
    public Result<Boolean> testDeviceConnection(
            @Parameter(description = "设备配置") @RequestBody HardwareConfig config) {
        return Result.success(hardwareDeviceService.testConnection(config));
    }

    /**
     * 统一打印接口
     * @param printRequest 打印请求参数
     * @return 打印结果，包含任务ID
     */
    @PostMapping("/print")
    @Operation(summary = "统一打印接口")
    @PreAuthorize("hasAuthority('device:management:manage') or hasAuthority('*')")
    public Result<PrintTask> print(
            @Parameter(description = "打印请求参数") @RequestBody DevicePrintRequest printRequest) {
        // 创建打印任务
        PrintTask task = new PrintTask();

        // 修复：根据PrintTask实体类的实际字段进行映射（原代码使用了不存在的setter方法）

        // 映射任务类型：将字符串类型转为整数编码
        // THERMAL_PAPER→1(小票), FILE→3(报表), INVOICE→1(小票), TRACEABILITY_LABEL→2(标签)
        String printType = printRequest.getPrintType();
        if (printType != null) {
            switch (printType.toUpperCase()) {
                case "THERMAL_PAPER":
                case "INVOICE":
                    task.setTaskType(1); // 小票
                    break;
                case "TRACEABILITY_LABEL":
                    task.setTaskType(2); // 标签
                    break;
                case "FILE":
                    task.setTaskType(3); // 报表
                    break;
                default:
                    task.setTaskType(1); // 默认小票
            }
        }

        // 映射设备ID：deviceType为字符串标识符，需要转换为实际的设备Long ID
        // 注意：此处简化处理，实际应通过HardwareDeviceService查询设备ID
        String deviceType = printRequest.getDeviceType();
        if (deviceType != null && deviceType.matches("\\d+")) {
            task.setDeviceId(Long.parseLong(deviceType));
        }

        // 映射打印内容：将content和额外信息整合到contentJson字段
        StringBuilder contentJson = new StringBuilder();
        if (printRequest.getContent() != null) {
            contentJson.append(printRequest.getContent());
        }

        // 将额外业务数据（filePath、invoiceData、traceabilityCode、productName）序列化后追加到contentJson
        // PrintTask实体使用contentJson存储完整的打印内容和元数据
        if (printRequest.getFilePath() != null || printRequest.getInvoiceData() != null
                || printRequest.getTraceabilityCode() != null || printRequest.getProductName() != null) {
            contentJson.append("|||METADATA:");
            if (printRequest.getFilePath() != null) {
                contentJson.append("filePath=").append(printRequest.getFilePath()).append(";");
            }
            if (printRequest.getProductName() != null) {
                contentJson.append("productName=").append(printRequest.getProductName()).append(";");
            }
            if (printRequest.getTraceabilityCode() != null) {
                contentJson.append("traceabilityCode=").append(printRequest.getTraceabilityCode()).append(";");
            }
            if (printRequest.getInvoiceData() != null) {
                contentJson.append("invoiceData=").append(printRequest.getInvoiceData().toString());
            }
        }

        task.setContentJson(contentJson.toString());

        // 设置初始状态为待打印
        task.setPrintStatus(0);

        // 提交打印任务到队列
        PrintTask submittedTask = devicePrintService.submitPrintTask(task);

        return Result.success(submittedTask);
    }

    /**
     * 获取所有设备状态
     * @return 所有设备状态
     */
    @GetMapping("/status/all")
    @Operation(summary = "获取所有设备状态")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<Map<String, DeviceStatus>> getAllDeviceStatus() {
        return Result.success(deviceStatusService.getAllDeviceStatus());
    }

    /**
     * 刷新设备状态
     * @param deviceType 设备类型，为空则刷新所有设备
     * @return 刷新结果
     */
    @PostMapping("/status/refresh")
    @Operation(summary = "刷新设备状态")
    @PreAuthorize("hasAuthority('device:management:manage') or hasAuthority('*')")
    public Result<Boolean> refreshDeviceStatus(
            @Parameter(description = "设备类型") @RequestParam(required = false) String deviceType) {
        if (deviceType != null) {
            deviceStatusService.asyncCheckDeviceStatus(deviceType);
        } else {
            deviceStatusService.asyncCheckAllDeviceStatus();
        }
        return Result.success(true);
    }

    /**
     * 扫码枪扫描
     * @param inventoryCode 库存编码
     * @return 扫描结果
     */
    @PostMapping("/scan")
    @Operation(summary = "扫码枪扫描")
    @PreAuthorize("hasAuthority('device:management:manage') or hasAuthority('*')")
    public Result<Boolean> scanInventoryCode(
            @Parameter(description = "库存编码") @RequestParam String inventoryCode) {
        return Result.success(hardwareDeviceService.scanInventoryCode(inventoryCode));
    }

    /**
     * 摄像头拍照
     * @param orderId 订单ID
     * @param traceabilityCode 溯源码
     * @return 拍照结果
     */
    @PostMapping("/camera/capture")
    @Operation(summary = "摄像头拍照")
    @PreAuthorize("hasAuthority('device:management:manage') or hasAuthority('*')")
    public Result<Boolean> capturePhoto(
            @Parameter(description = "订单ID") @RequestParam String orderId,
            @Parameter(description = "溯源码") @RequestParam String traceabilityCode) {
        return Result.success(hardwareDeviceService.captureMealPhoto(orderId, traceabilityCode));
    }
    
    /**
     * 查询打印任务状态
     * @param taskId 任务ID
     * @return 打印任务信息
     */
    @GetMapping("/print/task/{taskId}")
    @Operation(summary = "查询打印任务状态")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<PrintTask> getPrintTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        PrintTask task = printTaskQueueManager.getTaskById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }
        return Result.success(task);
    }
    
    /**
     * 查询指定设备类型的打印任务
     * @param deviceType 设备类型，为空则查询所有设备类型
     * @return 打印任务列表
     */
    @GetMapping("/print/tasks")
    @Operation(summary = "查询打印任务列表")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<List<PrintTask>> getPrintTasks(
            @Parameter(description = "设备类型") @RequestParam(required = false) String deviceType) {
        return Result.success(printTaskQueueManager.getTasksByDeviceType(deviceType));
    }
    
    /**
     * 查询打印任务历史记录
     * @param deviceType 设备类型，为空则查询所有设备类型
     * @param limit 限制数量，0表示查询所有记录
     * @return 打印任务历史记录列表
     */
    @GetMapping("/print/history")
    @Operation(summary = "查询打印任务历史记录")
    @PreAuthorize("hasAuthority('device:management:view') or hasAuthority('*')")
    public Result<List<PrintTask>> getPrintHistory(
            @Parameter(description = "设备类型") @RequestParam(required = false) String deviceType,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "50") int limit) {
        return Result.success(printTaskQueueManager.getTaskHistory(deviceType, limit));
    }

}
