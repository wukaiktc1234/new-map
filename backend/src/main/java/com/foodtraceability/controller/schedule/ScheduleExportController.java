package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.ConflictQueryDTO;
import com.foodtraceability.dto.schedule.ScheduleExportDTO;
import com.foodtraceability.service.schedule.ScheduleExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 排班数据导出控制器
 * 对应前端 API 路径 /v1/schedule/export
 *
 * <p>端点说明：
 * <ul>
 *   <li>POST /v1/schedule/export/schedule       - 导出排班数据</li>
 *   <li>POST /v1/schedule/export/conflicts      - 导出冲突数据</li>
 *   <li>POST /v1/schedule/export/swap           - 导出换班数据</li>
 *   <li>POST /v1/schedule/export/attendance-diff - 导出考勤差异报告</li>
 * </ul>
 *
 * <p>注意：当前导出格式为 CSV（带 UTF-8 BOM 头，确保 Excel 正确识别中文）
 */
@Tag(name = "排班管理-数据导出", description = "排班数据导出(F-010)相关接口")
@RestController
@RequestMapping("/v1/schedule/export")
public class ScheduleExportController {

    private final ScheduleExportService scheduleExportService;

    public ScheduleExportController(ScheduleExportService scheduleExportService) {
        this.scheduleExportService = scheduleExportService;
    }

    /**
     * 导出排班数据
     */
    @Operation(summary = "导出排班数据",
            description = "根据方案ID导出排班数据，支持 excel/csv 格式")
    @PostMapping("/schedule")
    public ResponseEntity<byte[]> exportSchedule(@RequestBody ScheduleExportDTO exportDTO) {
        byte[] data = scheduleExportService.exportSchedule(exportDTO);
        String filename = buildFilename("排班数据", exportDTO.getPlanId(), exportDTO.getFormat());
        return buildFileResponse(data, filename);
    }

    /**
     * 导出冲突数据
     */
    @Operation(summary = "导出冲突数据",
            description = "根据方案ID导出排班冲突数据")
    @PostMapping("/conflicts")
    public ResponseEntity<byte[]> exportConflicts(
            @Parameter(description = "方案ID", required = true) @RequestParam String planId,
            @Parameter(description = "导出格式: excel/csv") @RequestParam(defaultValue = "excel") String format) {
        byte[] data = scheduleExportService.exportConflicts(planId, format);
        String filename = buildFilename("冲突数据", planId, format);
        return buildFileResponse(data, filename);
    }

    /**
     * 导出换班请求数据
     */
    @Operation(summary = "导出换班请求数据",
            description = "根据方案ID导出换班请求数据")
    @PostMapping("/swap")
    public ResponseEntity<byte[]> exportSwapRequests(
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) String level,
            @Parameter(description = "导出格式: excel/csv") @RequestParam(defaultValue = "excel") String format) {
        ConflictQueryDTO queryDTO = new ConflictQueryDTO();
        queryDTO.setPlanId(planId);
        queryDTO.setLevel(level);
        byte[] data = scheduleExportService.exportSwapRequests(queryDTO, format);
        String filename = buildFilename("换班数据", planId, format);
        return buildFileResponse(data, filename);
    }

    /**
     * 导出考勤差异报告
     */
    @Operation(summary = "导出考勤差异报告",
            description = "根据方案ID导出考勤差异报告")
    @PostMapping("/attendance-diff")
    public ResponseEntity<byte[]> exportAttendanceDiff(
            @Parameter(description = "方案ID", required = true) @RequestParam String planId,
            @Parameter(description = "导出格式: excel/csv") @RequestParam(defaultValue = "excel") String format) {
        byte[] data = scheduleExportService.exportAttendanceDiff(planId, format);
        String filename = buildFilename("考勤差异", planId, format);
        return buildFileResponse(data, filename);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建文件名
     * 格式：{业务名}_{方案ID}_{时间戳}.{扩展名}
     * @param businessName 业务名（中文）
     * @param planId 方案ID
     * @param format 格式（excel/csv）
     * @return 文件名
     */
    private String buildFilename(String businessName, String planId, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "csv"; // 当前统一为 csv
        String idSuffix = planId != null ? planId : "all";
        return businessName + "_" + idSuffix + "_" + timestamp + "." + extension;
    }

    /**
     * 构建文件下载响应
     * @param data 文件字节数组
     * @param filename 文件名
     * @return ResponseEntity
     */
    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        // 处理中文文件名编码
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (Exception e) {
            encodedFilename = filename;
        }
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
