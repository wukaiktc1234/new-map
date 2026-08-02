package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.GenerateScheduleDTO;
import com.foodtraceability.dto.schedule.ScheduleEntryVO;
import com.foodtraceability.service.schedule.ScheduleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 排班生成引擎控制器
 * 对应前端 API 路径 /v1/schedule/engine
 *
 * <p>端点说明：
 * <ul>
 *   <li>POST /v1/schedule/engine/generate     - 生成排班（仅预览，不保存）</li>
 *   <li>GET  /v1/schedule/engine/preview/{planId} - 预览排班</li>
 *   <li>GET  /v1/schedule/engine/progress/{planId} - 获取生成进度</li>
 *   <li>GET  /v1/schedule/engine/history/{planId} - 获取生成历史</li>
 * </ul>
 */
@Tag(name = "排班管理-排班引擎", description = "自动排班生成(F-006)相关接口")
@RestController
@RequestMapping("/v1/schedule/engine")
public class ScheduleEngineController {

    private final ScheduleEngineService scheduleEngineService;

    public ScheduleEngineController(ScheduleEngineService scheduleEngineService) {
        this.scheduleEngineService = scheduleEngineService;
    }

    /**
     * 生成排班（仅预览，不保存）
     */
    @Operation(summary = "生成排班",
            description = "基于模板和约束自动生成排班方案（仅预览，不保存）")
    @PostMapping("/generate")
    public Result<List<ScheduleEntryVO>> generateSchedule(
            @Valid @RequestBody GenerateScheduleDTO generateDTO) {
        try {
            List<ScheduleEntryVO> result = scheduleEngineService.generateSchedule(generateDTO);
            return Result.success(result, "排班生成成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 预览排班
     */
    @Operation(summary = "预览排班",
            description = "基于方案和模板预览排班结果（含条目列表和统计信息）")
    @GetMapping("/preview/{planId}")
    public Result<Map<String, Object>> previewSchedule(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId,
            @Parameter(description = "模板ID（可选）") @RequestParam(required = false) String templateId) {
        try {
            Map<String, Object> result = scheduleEngineService.previewSchedule(planId, templateId);
            return Result.success(result, "预览排班成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取生成进度
     */
    @Operation(summary = "获取生成进度",
            description = "查询指定方案的排班生成进度")
    @GetMapping("/progress/{planId}")
    public Result<Map<String, Object>> getGenerateProgress(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            Map<String, Object> result = scheduleEngineService.getGenerateProgress(planId);
            return Result.success(result, "获取生成进度成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取生成历史
     */
    @Operation(summary = "获取生成历史",
            description = "查询指定方案的排班生成历史记录")
    @GetMapping("/history/{planId}")
    public Result<List<Map<String, Object>>> getGenerateHistory(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            List<Map<String, Object>> result = scheduleEngineService.getGenerateHistory(planId);
            return Result.success(result, "获取生成历史成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
