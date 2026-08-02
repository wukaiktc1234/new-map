package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.AttendanceRecord;
import com.foodtraceability.service.AttendanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理控制器
 */
@Tag(name = "考勤管理", description = "员工考勤相关接口")
@RestController
@RequestMapping("/v1/attendance")
public class AttendanceController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceRecordService attendanceRecordService;

    public AttendanceController(AttendanceRecordService attendanceRecordService) {
        this.attendanceRecordService = attendanceRecordService;
    }

    @Operation(summary = "分页查询考勤记录")
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getAttendancePage(AttendanceRecordQueryDTO queryDTO) {
        try {
            Map<String, Object> result = attendanceRecordService.getAttendancePage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询考勤记录失败", e);
            return Result.error("查询考勤记录失败");
        }
    }

    @Operation(summary = "考勤统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('hr:attendance:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", attendanceRecordService.count());
            // 今日打卡数
            LocalDate today = LocalDate.now();
            long todayCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>()
                    .eq("attendance_date", today));
            statistics.put("todayCount", todayCount);
            // 按状态分布：1=正常, 2=迟到, 3=早退, 4=缺勤, 5=加班, 6=请假
            Map<String, Object> statusDistribution = new HashMap<>();
            statusDistribution.put("normal", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 1)));
            statusDistribution.put("late", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 2)));
            statusDistribution.put("earlyLeave", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 3)));
            statusDistribution.put("absent", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 4)));
            statusDistribution.put("overtime", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 5)));
            statusDistribution.put("onLeave", attendanceRecordService.count(new QueryWrapper<AttendanceRecord>().eq("status", 6)));
            statistics.put("statusDistribution", statusDistribution);
            // 异常考勤数（非正常状态）
            long abnormalCount = attendanceRecordService.count(new QueryWrapper<AttendanceRecord>()
                    .ne("status", 1));
            statistics.put("abnormalCount", abnormalCount);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取考勤统计信息失败", e);
            return Result.error(5001, "获取统计信息失败");
        }
    }

    @Operation(summary = "获取考勤记录详情")
    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<AttendanceRecordVO> getAttendanceDetail(
            @Parameter(description = "考勤记录ID") @PathVariable Long recordId) {
        try {
            AttendanceRecordVO vo = attendanceRecordService.getAttendanceDetail(recordId);
            if (vo == null) {
                return Result.error("考勤记录不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取考勤详情失败，recordId：{}", recordId, e);
            return Result.error("获取考勤详情失败");
        }
    }

    @Operation(summary = "创建考勤记录", description = "手动创建或补录考勤记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<AttendanceRecord> createAttendance(
            @Parameter(description = "考勤信息") @Valid @RequestBody AttendanceRecordCreateDTO createDTO) {
        try {
            AttendanceRecord record = attendanceRecordService.createAttendance(createDTO);
            return Result.success(record);
        } catch (RuntimeException e) {
            log.warn("创建考勤记录失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建考勤记录异常", e);
            return Result.error("创建考勤记录失败");
        }
    }

    @Operation(summary = "更新考勤记录")
    @PutMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<AttendanceRecord> updateAttendance(
            @Parameter(description = "考勤记录ID") @PathVariable Long recordId,
            @Parameter(description = "更新信息") @Valid @RequestBody AttendanceRecordUpdateDTO updateDTO) {
        try {
            AttendanceRecord record = attendanceRecordService.updateAttendance(recordId, updateDTO);
            return Result.success(record);
        } catch (RuntimeException e) {
            log.warn("更新考勤记录失败：{}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新考勤记录异常，recordId：{}", recordId, e);
            return Result.error("更新考勤记录失败");
        }
    }

    @Operation(summary = "上班打卡", description = "员工进行上班签到")
    @PostMapping("/clock-in/{employeeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr') or #employeeId == authentication.principal")
    public Result<AttendanceRecord> clockIn(
            @Parameter(description = "员工ID") @PathVariable String employeeId) {
        try {
            AttendanceRecord record = attendanceRecordService.clockIn(employeeId);
            return Result.success(record);
        } catch (RuntimeException e) {
            log.warn("上班打卡失败（{}）：{}", employeeId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("上班打卡异常，employeeId：{}", employeeId, e);
            return Result.error("打卡失败，请联系管理员");
        }
    }

    @Operation(summary = "下班打卡", description = "员工进行下班签退")
    @PostMapping("/clock-out/{employeeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr') or #employeeId == authentication.principal")
    public Result<AttendanceRecord> clockOut(
            @Parameter(description = "员工ID") @PathVariable String employeeId) {
        try {
            AttendanceRecord record = attendanceRecordService.clockOut(employeeId);
            return Result.success(record);
        } catch (RuntimeException e) {
            log.warn("下班打卡失败（{}）：{}", employeeId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("下班打卡异常，employeeId：{}", employeeId, e);
            return Result.error("打卡失败，请联系管理员");
        }
    }

    @Operation(summary = "请假申请", description = "员工提交请假申请")
    @PostMapping("/leave/{employeeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<List<AttendanceRecord>> applyLeave(
            @Parameter(description = "员工ID") @PathVariable String employeeId,
            @RequestParam Integer leaveType,
            @RequestParam BigDecimal leaveHours,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String remark) {
        try {
            List<AttendanceRecord> records = attendanceRecordService.applyLeave(
                    employeeId, leaveType, leaveHours, startDate, endDate, remark);
            return Result.success(records);
        } catch (Exception e) {
            log.error("请假申请失败，employeeId：{}", employeeId, e);
            return Result.error("请假申请失败");
        }
    }

    @Operation(summary = "加班登记", description = "登记员工加班信息")
    @PostMapping("/overtime/{employeeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<AttendanceRecord> registerOvertime(
            @Parameter(description = "员工ID") @PathVariable String employeeId,
            @RequestParam BigDecimal overtimeHours,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) String remark) {
        try {
            AttendanceRecord record = attendanceRecordService.registerOvertime(
                    employeeId, overtimeHours, date, remark);
            return Result.success(record);
        } catch (Exception e) {
            log.error("加班登记失败，employeeId：{}", employeeId, e);
            return Result.error("加班登记失败");
        }
    }

    @Operation(summary = "获取月度考勤统计", description = "查询指定员工的月度考勤汇总数据")
    @GetMapping("/monthly-summary/{employeeId}/{yearMonth}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getMonthlySummary(
            @Parameter(description = "员工ID") @PathVariable String employeeId,
            @Parameter(description = "年月（格式：2026-04）") @PathVariable String yearMonth) {
        try {
            Map<String, Object> summary = attendanceRecordService.getMonthlySummary(employeeId, yearMonth);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取月度考勤统计失败，employeeId：{}，yearMonth：{}", employeeId, yearMonth, e);
            return Result.error("获取月度考勤统计失败");
        }
    }

    @Operation(summary = "获取部门当日考勤", description = "查询某部门所有员工在指定日期的考勤情况")
    @GetMapping("/department/{departmentId}/daily")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<AttendanceRecordVO>> getDepartmentDailyAttendance(
            @Parameter(description = "部门ID") @PathVariable String departmentId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            List<AttendanceRecordVO> voList = attendanceRecordService.getDepartmentDailyAttendance(departmentId, date);
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取部门当日考勤失败，departmentId：{}，date：{}", departmentId, date, e);
            return Result.error("获取部门当日考勤失败");
        }
    }
}
