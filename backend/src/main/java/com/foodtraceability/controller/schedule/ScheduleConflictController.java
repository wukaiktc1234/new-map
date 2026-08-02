package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.ConflictCheckResultVO;
import com.foodtraceability.dto.schedule.ConflictDetailVO;
import com.foodtraceability.dto.schedule.ConflictQueryDTO;
import com.foodtraceability.service.schedule.ScheduleConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排班冲突检测控制器
 * 对应前端 API 路径 /v1/schedule/conflicts
 *
 * <p>端点说明：
 * <ul>
 *   <li>GET  /v1/schedule/conflicts               - 查询冲突列表（参数：planId, level）</li>
 *   <li>GET  /v1/schedule/conflicts/{planId}/check - 检查方案的冲突</li>
 *   <li>PUT  /v1/schedule/conflicts/fix/{id}      - 自动修复冲突</li>
 *   <li>PUT  /v1/schedule/conflicts/{id}/ignore   - 忽略冲突</li>
 * </ul>
 */
@Tag(name = "排班管理-冲突检测", description = "排班冲突检测(F-005)相关接口")
@RestController
@RequestMapping("/v1/schedule/conflicts")
public class ScheduleConflictController {

    private final ScheduleConflictService scheduleConflictService;

    public ScheduleConflictController(ScheduleConflictService scheduleConflictService) {
        this.scheduleConflictService = scheduleConflictService;
    }

    /**
     * 查询方案的冲突列表（支持按级别筛选）
     */
    @Operation(summary = "查询冲突列表")
    @GetMapping
    public Result<List<ConflictDetailVO>> getConflictList(
            @Parameter(description = "方案ID", required = true) @RequestParam String planId,
            @Parameter(description = "冲突级别筛选: error/warning/info")
            @RequestParam(required = false) String level) {
        try {
            ConflictQueryDTO queryDTO = new ConflictQueryDTO();
            queryDTO.setPlanId(planId);
            queryDTO.setLevel(level);
            List<ConflictDetailVO> list = scheduleConflictService.getConflictList(queryDTO);
            return Result.success(list, "查询冲突列表成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查方案的排班冲突
     * 业务逻辑：扫描方案下所有排班条目，识别冲突并返回检查结果
     */
    @Operation(summary = "检查方案的排班冲突",
            description = "扫描方案下所有排班条目，识别重复班次、连续工作超限等问题")
    @GetMapping("/{planId}/check")
    public Result<ConflictCheckResultVO> checkConflicts(
            @Parameter(description = "方案ID") @PathVariable("planId") String planId) {
        try {
            ConflictCheckResultVO result = scheduleConflictService.checkConflicts(planId);
            return Result.success(result, "冲突检查完成");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 自动修复冲突
     */
    @Operation(summary = "自动修复冲突")
    @PutMapping("/fix/{id}")
    public Result<ConflictDetailVO> autoFixConflict(
            @Parameter(description = "冲突ID") @PathVariable("id") String id) {
        try {
            ConflictDetailVO vo = scheduleConflictService.autoFixConflict(id);
            return Result.success(vo, "自动修复成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 忽略冲突
     */
    @Operation(summary = "忽略冲突")
    @PutMapping("/{id}/ignore")
    public Result<ConflictDetailVO> ignoreConflict(
            @Parameter(description = "冲突ID") @PathVariable("id") String id) {
        try {
            ConflictDetailVO vo = scheduleConflictService.ignoreConflict(id);
            return Result.success(vo, "忽略冲突成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
