package com.foodtraceability.controller;

import com.foodtraceability.service.CustomReportService;
import com.foodtraceability.dto.CustomReportVO;
import com.foodtraceability.dto.CustomReportCreateDTO;
import com.foodtraceability.dto.CustomReportQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.common.Result;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 自定义报表控制器
 * 提供自定义SQL报表的创建、执行和管理API接口
 */
@RestController
@RequestMapping("/v1/reports/custom")
public class CustomReportController {

    private final CustomReportService customReportService;

    public CustomReportController(CustomReportService customReportService) {
        this.customReportService = customReportService;
    }

    /**
     * 创建自定义报表
     * @param dto 创建信息
     * @return 报表ID
     */
    @PostMapping
    public Result<Long> createCustomReport(
            @RequestBody CustomReportCreateDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // TODO: 从JWT Token中获取当前用户ID
        if (userId == null) {
            userId = 1L; // 默认管理员，实际应从安全上下文获取
        }
        try {
            Long reportId = customReportService.createCustomReport(dto, userId);
            return Result.success(reportId, "创建成功");
        } catch (Exception e) {
            return Result.error("创建自定义报表失败: " + e.getMessage());
        }
    }

    /**
     * 执行自定义报表
     * @param reportId 报表ID
     * @param params 查询参数
     * @return 执行结果
     */
    @PostMapping("/{reportId}/execute")
    public Result<Map<String, Object>> executeCustomReport(
            @PathVariable Long reportId,
            @RequestBody(required = false) Map<String, Object> params,
            @RequestHeader(value = "X-User-Id", required = false) Long executedBy) {
        if (executedBy == null) {
            executedBy = 1L; // 默认管理员
        }
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        try {
            Map<String, Object> result = customReportService.executeCustomReport(reportId, params, executedBy);
            return Result.success(result, "执行成功");
        } catch (Exception e) {
            return Result.error("执行报表失败: " + e.getMessage());
        }
    }

    /**
     * 设置定时调度
     * @param reportId 报表ID
     * @param cronExpression cron表达式
     * @return 是否成功
     */
    @PutMapping("/{reportId}/schedule")
    public Result<Boolean> scheduleReport(
            @PathVariable Long reportId,
            @RequestParam String cronExpression,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            boolean result = customReportService.scheduleReport(reportId, cronExpression, userId);
            if (result) {
                return Result.success(true, "设置成功");
            } else {
                return Result.error("设置失败");
            }
        } catch (Exception e) {
            return Result.error("设置定时调度失败: " + e.getMessage());
        }
    }

    /**
     * 获取执行历史
     * @param reportId 报表ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 执行历史列表
     */
    @GetMapping("/{reportId}/history")
    public Result<PageResult<Map<String, Object>>> getReportHistory(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            PageResult<Map<String, Object>> result = customReportService.getReportHistory(reportId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取执行历史失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询自定义报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/query")
    public Result<PageResult<CustomReportVO>> queryCustomReports(
            @RequestBody CustomReportQueryDTO queryDTO,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            PageResult<CustomReportVO> result = customReportService.queryCustomReports(queryDTO, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询自定义报表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    @GetMapping("/{reportId}")
    public Result<CustomReportVO> getReportById(@PathVariable Long reportId) {
        try {
            CustomReportVO vo = customReportService.getReportById(reportId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取报表详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新自定义报表
     * @param reportId 报表ID
     * @param dto 更新信息
     * @return 是否成功
     */
    @PutMapping("/{reportId}")
    public Result<Boolean> updateCustomReport(
            @PathVariable Long reportId,
            @RequestBody CustomReportCreateDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            boolean result = customReportService.updateCustomReport(reportId, dto, userId);
            if (result) {
                return Result.success(true, "更新成功");
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新自定义报表失败: " + e.getMessage());
        }
    }

    /**
     * 删除自定义报表
     * @param reportId 报表ID
     * @return 是否成功
     */
    @DeleteMapping("/{reportId}")
    public Result<Boolean> deleteCustomReport(
            @PathVariable Long reportId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            userId = 1L;
        }
        try {
            boolean result = customReportService.deleteCustomReport(reportId, userId);
            if (result) {
                return Result.success(true, "删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error("删除自定义报表失败: " + e.getMessage());
        }
    }
}
