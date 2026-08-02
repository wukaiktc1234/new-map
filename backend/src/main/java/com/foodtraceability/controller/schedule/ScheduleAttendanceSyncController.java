package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.AttendanceDiffReportVO;
import com.foodtraceability.dto.schedule.AttendanceSyncVO;
import com.foodtraceability.service.schedule.ScheduleAttendanceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排班考勤同步控制器
 * 对应前端 API 路径 /v1/schedule/attendance
 *
 * <p>端点说明：
 * <ul>
 *   <li>POST /v1/schedule/attendance/{planId}/sync    - 同步排班到考勤系统</li>
 *   <li>GET  /v1/schedule/attendance/{planId}/status  - 获取同步状态</li>
 *   <li>GET  /v1/schedule/attendance/{planId}/diff    - 获取考勤差异报告</li>
 *   <li>GET  /v1/schedule/attendance/{planId}/history - 获取同步历史</li>
 * </ul>
 */
@Tag(name = "排班管理-考勤同步", description = "排班考勤同步(F-007)相关接口")
@RestController
@RequestMapping("/v1/schedule/attendance")
public class ScheduleAttendanceSyncController {

    private final ScheduleAttendanceSyncService attendanceSyncService;

    public ScheduleAttendanceSyncController(ScheduleAttendanceSyncService attendanceSyncService) {
        this.attendanceSyncService = attendanceSyncService;
    }

    /**
     * 同步排班数据到考勤系统
     */
    @Operation(summary = "同步排班到考勤系统",
            description = "将方案下所有排班条目同步到考勤系统，并记录同步结果")
    @PostMapping("/{planId}/sync")
    public Result<AttendanceSyncVO> syncToAttendance(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            AttendanceSyncVO vo = attendanceSyncService.syncToAttendance(planId);
            return Result.success(vo, "同步成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取方案的同步状态
     */
    @Operation(summary = "获取同步状态",
            description = "查询指定方案的最新同步状态")
    @GetMapping("/{planId}/status")
    public Result<AttendanceSyncVO> getSyncStatus(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            AttendanceSyncVO vo = attendanceSyncService.getSyncStatus(planId);
            return Result.success(vo, "获取同步状态成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取考勤差异报告
     */
    @Operation(summary = "获取考勤差异报告",
            description = "对比排班数据与实际考勤打卡数据，识别迟到/早退/缺卡等差异")
    @GetMapping("/{planId}/diff")
    public Result<AttendanceDiffReportVO> getDiffReport(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId,
            @Parameter(description = "统计周期开始日期 (YYYY-MM-DD)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "统计周期结束日期 (YYYY-MM-DD)")
            @RequestParam(required = false) String endDate) {
        try {
            AttendanceDiffReportVO vo = attendanceSyncService.getDiffReport(planId, startDate, endDate);
            return Result.success(vo, "获取差异报告成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取方案的同步历史
     */
    @Operation(summary = "获取同步历史",
            description = "查询指定方案的所有同步记录（按时间倒序）")
    @GetMapping("/{planId}/history")
    public Result<List<AttendanceSyncVO>> getSyncHistory(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            List<AttendanceSyncVO> list = attendanceSyncService.getSyncHistory(planId);
            return Result.success(list, "获取同步历史成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
