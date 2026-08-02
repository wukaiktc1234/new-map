package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.CategoryOperationLog;
import com.foodtraceability.service.CategoryOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品分类操作日志控制器
 * 提供菜品分类操作日志的查询接口
 *
 * <p>路径前缀：/v1/product-center/categories/operation-logs
 *
 * <p>注意：字面量路径（/page、/category/{categoryId}、/operation-type/{operationType}）
 * 与根路径 GET 共存，Spring MVC 按特异性匹配。
 */
@Tag(name = "菜品分类操作日志", description = "菜品分类操作日志查询接口")
@RestController
@RequestMapping("/v1/product-center/categories/operation-logs")
public class CategoryOperationLogController {

    private final CategoryOperationLogService categoryOperationLogService;

    public CategoryOperationLogController(CategoryOperationLogService categoryOperationLogService) {
        this.categoryOperationLogService = categoryOperationLogService;
    }

    /**
     * 获取所有操作日志（最多 1000 条）
     */
    @GetMapping
    @Operation(summary = "获取所有操作日志", description = "获取前 1000 条操作日志")
    public Result<List<CategoryOperationLog>> getAllOperationLogs() {
        try {
            List<CategoryOperationLog> logs = categoryOperationLogService.getAllOperationLogs(1, 1000);
            return Result.success(logs, "获取操作日志成功");
        } catch (Exception e) {
            return Result.error("获取操作日志失败");
        }
    }

    /**
     * 根据分类ID获取操作日志
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "按分类ID查询操作日志", description = "获取指定分类的所有操作日志")
    public Result<List<CategoryOperationLog>> getOperationLogsByCategoryId(
            @Parameter(description = "分类ID") @PathVariable String categoryId) {
        try {
            List<CategoryOperationLog> logs = categoryOperationLogService.getOperationLogs(categoryId);
            return Result.success(logs, "获取分类操作日志成功");
        } catch (Exception e) {
            return Result.error("获取分类操作日志失败");
        }
    }

    /**
     * 根据操作类型获取操作日志
     */
    @GetMapping("/operation-type/{operationType}")
    @Operation(summary = "按操作类型查询操作日志", description = "获取指定操作类型的所有操作日志")
    public Result<List<CategoryOperationLog>> getOperationLogsByType(
            @Parameter(description = "操作类型") @PathVariable String operationType) {
        try {
            // Service 未提供按类型查询方法，先查全部再内存过滤
            List<CategoryOperationLog> allLogs = categoryOperationLogService.getAllOperationLogs(1, 1000);
            List<CategoryOperationLog> filteredLogs = allLogs.stream()
                    .filter(log -> operationType.equals(log.getOperationType()))
                    .collect(Collectors.toList());
            return Result.success(filteredLogs, "获取操作日志成功");
        } catch (Exception e) {
            return Result.error("获取操作日志失败");
        }
    }

    /**
     * 分页获取操作日志
     */
    @GetMapping("/page")
    @Operation(summary = "分页获取操作日志", description = "按页码和每页条数分页获取操作日志")
    public Result<List<CategoryOperationLog>> getOperationLogsPage(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        try {
            List<CategoryOperationLog> logs = categoryOperationLogService.getAllOperationLogs(page, size);
            return Result.success(logs, "获取操作日志分页成功");
        } catch (Exception e) {
            return Result.error("获取操作日志分页失败");
        }
    }
}
