package com.foodtraceability.controller.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PrintTaskCreateDTO;
import com.foodtraceability.dto.PrintTaskQueryDTO;
import com.foodtraceability.dto.PrintTaskVO;
import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.service.device.PrintTaskService;
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
 * 打印任务控制器
 * 提供打印任务的创建、查询、状态管理等API
 */
@RestController
@RequestMapping("/v1/print-tasks")
@Tag(name = "打印任务管理", description = "打印任务相关接口")
public class PrintTaskController {

    private static final Logger log = LoggerFactory.getLogger(PrintTaskController.class);

    private final PrintTaskService printTaskService;

    public PrintTaskController(PrintTaskService printTaskService) {
        this.printTaskService = printTaskService;
    }

    @PostMapping
    @Operation(summary = "创建打印任务")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<PrintTaskVO> createPrintTask(@Valid @RequestBody PrintTaskCreateDTO dto) {
        log.info("创建打印任务: deviceId={}, taskType={}", dto.getDeviceId(), dto.getTaskType());
        return printTaskService.createPrintTask(dto);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量创建打印任务（厨房单场景）")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<List<PrintTaskVO>> batchCreatePrintTasks(@Valid @RequestBody List<PrintTaskCreateDTO> dtoList) {
        log.info("批量创建打印任务: 数量={}", dtoList.size());
        return printTaskService.batchCreatePrintTasks(dtoList);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询打印任务详情")
    @PreAuthorize("hasAuthority('device:print-task:view') or hasAuthority('*')")
    public Result<PrintTaskVO> getTaskById(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        log.info("查询打印任务: taskId={}", taskId);
        return printTaskService.getTaskById(taskId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询打印任务列表")
    @PreAuthorize("hasAuthority('device:print-task:view') or hasAuthority('*')")
    public Result<IPage<PrintTaskVO>> getTaskPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "目标打印机ID") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "任务类型") @RequestParam(required = false) Integer taskType,
            @Parameter(description = "打印状态") @RequestParam(required = false) Integer printStatus,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "任务编号(模糊)") @RequestParam(required = false) String taskCode) {

        PrintTaskQueryDTO queryDto = new PrintTaskQueryDTO();
        queryDto.setDeviceId(deviceId);
        queryDto.setTaskType(taskType);
        queryDto.setPrintStatus(printStatus);
        queryDto.setStartTime(startTime);
        queryDto.setEndTime(endTime);
        queryDto.setTaskCode(taskCode);

        Page<?> pageParam = new Page<>(page, size);
        return printTaskService.getTaskPage(pageParam, queryDto);
    }

    @GetMapping("/device/{deviceId}/pending")
    @Operation(summary = "查询设备的待打印任务")
    @PreAuthorize("hasAuthority('device:print-task:view') or hasAuthority('*')")
    public Result<List<PrintTaskVO>> getPendingTasksByDevice(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        log.info("查询待打印任务: deviceId={}", deviceId);
        return printTaskService.getPendingTasksByDevice(deviceId);
    }

    @PutMapping("/{taskId}/printing")
    @Operation(summary = "标记任务为打印中")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<Void> updateTaskToPrinting(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        log.info("标记任务为打印中: taskId={}", taskId);
        return printTaskService.updateTaskToPrinting(taskId);
    }

    @PutMapping("/{taskId}/completed")
    @Operation(summary = "标记任务为已完成")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<Void> updateTaskToCompleted(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        log.info("标记任务为已完成: taskId={}", taskId);
        return printTaskService.updateTaskToCompleted(taskId);
    }

    @PutMapping("/{taskId}/failed")
    @Operation(summary = "标记任务为失败")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<Void> updateTaskToFailed(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, String> request) {
        String errorMessage = (request != null) ? request.get("errorMessage") : "打印失败";
        log.info("标记任务为失败: taskId={}, error={}", taskId, errorMessage);
        return printTaskService.updateTaskToFailed(taskId, errorMessage);
    }

    @PostMapping("/{taskId}/retry")
    @Operation(summary = "重试失败的打印任务")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<Void> retryFailedTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        log.info("重试打印任务: taskId={}", taskId);
        return printTaskService.retryFailedTask(taskId);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除打印任务")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<Void> deletePrintTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        log.info("删除打印任务: taskId={}", taskId);
        return printTaskService.removeById(taskId);
    }

    @PutMapping("/{taskId}")
    @Operation(summary = "更新打印任务")
    @PreAuthorize("hasAuthority('device:print-task:manage') or hasAuthority('*')")
    public Result<PrintTaskVO> updatePrintTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @RequestBody PrintTask task) {
        task.setTaskId(taskId);
        log.info("更新打印任务: taskId={}", taskId);
        return printTaskService.updateById(task);
    }
}
