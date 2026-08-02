package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.TimelineRequestDTO;
import com.foodtraceability.dto.schedule.TimelineResponseVO;
import com.foodtraceability.service.schedule.SchedulePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 排班日历视图控制器
 * 对应前端 API 路径 /v1/schedule/calendar
 *
 * <p>端点说明：
 * <ul>
 *   <li>GET /v1/schedule/calendar         - 获取日历数据（参数：planId, weekStart）</li>
 *   <li>GET /v1/schedule/calendar/timeline - 获取周视图时间线数据（前端兼容端点）</li>
 *   <li>GET /v1/schedule/calendar/export   - 导出日历</li>
 * </ul>
 *
 * <p>注意：本控制器复用 SchedulePlanService.getTimelineData() 方法实现
 */
@Tag(name = "排班管理-日历视图", description = "排班日历视图(F-001)相关接口")
@RestController
@RequestMapping("/v1/schedule/calendar")
public class ScheduleCalendarController {

    private final SchedulePlanService schedulePlanService;

    public ScheduleCalendarController(SchedulePlanService schedulePlanService) {
        this.schedulePlanService = schedulePlanService;
    }

    /**
     * 获取日历数据
     * 复用 SchedulePlanService.getTimelineData() 方法
     */
    @Operation(summary = "获取日历数据",
            description = "根据方案ID和周起始日期获取一周7天的排班时间线数据")
    @GetMapping
    public Result<TimelineResponseVO> getCalendarData(
            @Parameter(description = "方案ID", required = true) @RequestParam String planId,
            @Parameter(description = "周一日期 (YYYY-MM-DD)", required = true)
            @RequestParam String weekStart) {
        try {
            TimelineRequestDTO requestDTO = new TimelineRequestDTO();
            requestDTO.setWeekStart(weekStart);
            TimelineResponseVO response = schedulePlanService.getTimelineData(planId, requestDTO);
            return Result.success(response, "获取日历数据成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取周视图时间线数据（前端兼容端点）
     * 前端 calendar.ts 调用 /v1/schedule/calendar/timeline
     */
    @Operation(summary = "获取周视图时间线数据",
            description = "根据周起始日期获取一周7天的排班时间线（前端兼容端点）")
    @GetMapping("/timeline")
    public Result<TimelineResponseVO> getTimeline(
            @Parameter(description = "周一日期 (YYYY-MM-DD)", required = true)
            @RequestParam String weekStart,
            @Parameter(description = "方案ID（可选）") @RequestParam(required = false) String planId) {
        try {
            TimelineRequestDTO requestDTO = new TimelineRequestDTO();
            requestDTO.setWeekStart(weekStart);
            // 如果未传 planId，使用默认值（SchedulePlanService 会处理）
            String targetPlanId = planId != null ? planId : "default";
            TimelineResponseVO response = schedulePlanService.getTimelineData(targetPlanId, requestDTO);
            return Result.success(response, "获取时间线数据成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出日历
     * 当前为框架实现，返回带表头的空 CSV 文件
     */
    @Operation(summary = "导出日历",
            description = "导出指定方案的日历数据为 CSV 文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCalendar(
            @Parameter(description = "方案ID", required = true) @RequestParam String planId,
            @Parameter(description = "周一日期 (YYYY-MM-DD)") @RequestParam(required = false) String weekStart) {
        // TODO: 实现完整的日历导出逻辑（调用 ScheduleExportService）
        // 当前返回带表头的空 CSV
        String csvContent = "\uFEFF员工ID,员工姓名,岗位,周一,周二,周三,周四,周五,周六,周日,周工时,冲突标记\n";

        byte[] data = csvContent.getBytes(StandardCharsets.UTF_8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "日历_" + planId + "_" + timestamp + ".csv";
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (Exception e) {
            encodedFilename = filename;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }
}
