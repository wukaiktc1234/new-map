package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/v1/operation-logs")
@Tag(name = "操作日志管理", description = "系统操作日志查询与管理")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    @Operation(summary = "查询操作日志列表")
    @PreAuthorize("hasAuthority('system:log:view') or hasAuthority('*')")
    public Result<Page<OperationLogEntity>> list(
            @RequestParam(required = false) String operationModule,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperationLogEntity> page = new Page<>(current, size);

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLogEntity> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (operationModule != null && !operationModule.isEmpty()) {
            wrapper.eq(OperationLogEntity::getOperationModule, operationModule);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLogEntity::getOperationType, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(OperationLogEntity::getOperatorName, operatorName);
        }
        if (startTime != null) {
            wrapper.ge(OperationLogEntity::getOperationTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLogEntity::getOperationTime, endTime);
        }
        wrapper.orderByDesc(OperationLogEntity::getOperationTime);

        operationLogService.page(page, wrapper);
        return Result.success(page);
    }
}
