package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.*;
import com.foodtraceability.service.schedule.SchedulePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 排班方案管理控制器
 * 提供排班方案的增删改查及统计接口（F-004）
 *
 * <p>业务规则：
 * <ul>
 *   <li>仅draft状态可编辑或删除</li>
 *   <li>乐观锁通过@Version自动处理并发冲突</li>
 * </ul>
 */
@Tag(name = "排班管理-排班方案", description = "排班方案(F-004)相关接口")
@RestController
@RequestMapping("/v1/schedule/plans")
public class SchedulePlanController {

    private final SchedulePlanService schedulePlanService;

    public SchedulePlanController(SchedulePlanService schedulePlanService) {
        this.schedulePlanService = schedulePlanService;
    }

    /**
     * 查询排班方案列表（分页+筛选）
     * 支持按状态、日期范围、模板ID筛选，按更新时间倒序返回
     */
    @Operation(summary = "查询排班方案列表",
            description = "分页查询排班方案列表，支持按状态、日期范围、模板ID筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('schedule:view:self')")
    public Result<PageResult<SchedulePlanVO>> getPlanList(SchedulePlanQueryDTO queryDTO) {
        PageResult<SchedulePlanVO> result = schedulePlanService.getPlanList(queryDTO);
        return Result.success(result);
    }

    /**
     * 获取排班方案详情（含条目列表）
     * 返回完整方案信息及关联的排班条目
     */
    @Operation(summary = "获取排班方案详情",
            description = "根据ID获取排班方案详细信息，包含关联的排班条目列表")
    @GetMapping("/{planId}")
    @PreAuthorize("hasAuthority('schedule:view:self')")
    public Result<SchedulePlanVO> getPlanById(
            @Parameter(description = "方案ID") @PathVariable String planId) {
        SchedulePlanVO vo = schedulePlanService.getPlanById(planId);
        if (vo == null) {
            return Result.error("排班方案不存在");
        }
        return Result.success(vo);
    }

    /**
     * 创建排班方案（草稿状态）
     * 新建方案默认状态为draft，版本号为1
     */
    @Operation(summary = "创建排班方案",
            description = "新建一个排班方案（默认为草稿状态），需指定名称和周期范围")
    @PostMapping
    @PreAuthorize("hasAuthority('schedule:plan:create')")
    public Result<SchedulePlanVO> createPlan(
            @Valid @RequestBody SchedulePlanCreateDTO createDTO) {
        try {
            SchedulePlanVO vo = schedulePlanService.createPlan(createDTO);
            return Result.success(vo, "方案创建成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑排班方案基本信息
     * 业务规则：仅draft状态可编辑，否则抛出异常"方案已发布无法编辑"
     * 乐观锁冲突时抛出OptimisticLockingFailureException
     */
    @Operation(summary = "编辑排班方案",
            description = "修改排班方案的基本信息（仅草稿状态可编辑）")
    @PutMapping("/{planId}")
    @PreAuthorize("hasAuthority('schedule:plan:edit')")
    public Result<SchedulePlanVO> updatePlan(
            @Parameter(description = "方案ID") @PathVariable String planId,
            @Valid @RequestBody SchedulePlanUpdateDTO updateDTO) {
        try {
            SchedulePlanVO vo = schedulePlanService.updatePlan(planId, updateDTO);
            return Result.success(vo, "方案更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除排班方案（逻辑删除）
     * 业务规则：仅draft状态可删除，否则抛出异常"已发布的方案无法删除"
     */
    @Operation(summary = "删除排班方案",
            description = "逻辑删除指定排班方案（仅草稿状态可删除）")
    @DeleteMapping("/{planId}")
    @PreAuthorize("hasAuthority('schedule:plan:delete')")
    public Result<String> deletePlan(
            @Parameter(description = "方案ID") @PathVariable String planId) {
        try {
            schedulePlanService.deletePlan(planId);
            return Result.success("方案删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 排班统计概览
     * 按当前用户门店ID过滤数据，返回各状态的方案数量
     */
    @Operation(summary = "排班统计概览",
            description = "获取当前门店各状态的排班方案数量统计")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('schedule:view:self')")
    public Result<SchedulePlanStatsVO> getPlanStats() {
        SchedulePlanStatsVO stats = schedulePlanService.getPlanStats();
        return Result.success(stats);
    }

    /**
     * 发布排班方案
     * 业务规则：仅 draft 状态可发布，发布后状态变为 published
     */
    @Operation(summary = "发布排班方案",
            description = "将草稿状态的排班方案发布（状态: draft → published）")
    @PutMapping("/{planId}/publish")
    @PreAuthorize("hasAuthority('schedule:plan:edit')")
    public Result<SchedulePlanVO> publishPlan(
            @Parameter(description = "方案ID") @PathVariable String planId) {
        try {
            SchedulePlanVO vo = schedulePlanService.publishPlan(planId);
            return Result.success(vo, "方案发布成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 撤回排班方案
     * 业务规则：仅 published 状态可撤回，撤回后状态变为 draft，需提供撤回原因
     */
    @Operation(summary = "撤回排班方案",
            description = "将已发布的排班方案撤回为草稿状态（状态: published → draft），需提供撤回原因")
    @PutMapping("/{planId}/withdraw")
    @PreAuthorize("hasAuthority('schedule:plan:edit')")
    public Result<SchedulePlanVO> withdrawPlan(
            @Parameter(description = "方案ID") @PathVariable String planId,
            @RequestBody Map<String, String> body) {
        try {
            String withdrawReason = body != null ? body.get("withdrawReason") : null;
            if (withdrawReason == null || withdrawReason.trim().isEmpty()) {
                return Result.error("撤回原因不能为空");
            }
            SchedulePlanVO vo = schedulePlanService.withdrawPlan(planId, withdrawReason);
            return Result.success(vo, "方案撤回成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 时间线日历(F-001)核心API ====================

    /**
     * 获取时间线日历数据（核心接口）
     * 根据weekStart返回一周7天的完整排班矩阵（员工×日期）
     *
     * <p>性能优化：
     * <ul>
     *   <li>一次查询7天所有条目，减少DB往返</li>
     *   <li>使用Map在内存中组装员工×日期的二维矩阵</li>
     *   <li>班次配置信息通过DataService缓存获取</li>
     * </ul>
     */
    @Operation(summary = "获取时间线日历数据",
            description = "根据周起始日期获取一周7天的排班时间线，包含员工列表、每日汇总、工时统计等")
    @GetMapping("/{planId}/timeline")
    @PreAuthorize("hasAuthority('schedule:view:self')")
    public Result<TimelineResponseVO> getTimelineData(
            @Parameter(description = "方案ID") @PathVariable String planId,
            @Valid TimelineRequestDTO requestDTO) {
        try {
            TimelineResponseVO response = schedulePlanService.getTimelineData(planId, requestDTO);
            return Result.success(response);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取排班条目列表（支持多条件筛选+分页）
     * 用于排班条目的详细查看和管理
     */
    @Operation(summary = "获取排班条目列表",
            description = "分页查询排班条目，支持按员工、日期范围、班次类型筛选")
    @GetMapping("/{planId}/entries")
    @PreAuthorize("hasAuthority('schedule:view:self')")
    public Result<PageResult<ScheduleEntryVO>> getEntryList(
            @Parameter(description = "方案ID") @PathVariable String planId,
            ScheduleEntryQueryDTO queryDTO) {
        try {
            // 设置planId到查询条件
            queryDTO.setPlanId(planId);
            PageResult<ScheduleEntryVO> result = schedulePlanService.getEntryList(planId, queryDTO);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量更新排班条目
     * 用于时间线日历的快速编辑、拖拽交换等场景
     *
     * <p>事务控制：整个操作在一个事务中完成，任何一条失败则全部回滚
     *
     * <p>业务规则：
     * <ul>
     *   <li>entryId存在 → 更新现有条目</li>
     *   <li>entryId不存在 → 创建新条目</li>
     *   <li>同一方案同一员工同一天只能有一条记录</li>
     * </ul>
     */
    @Operation(summary = "批量更新排班条目",
            description = "批量创建或更新排班条目，支持快速编辑和拖拽交换")
    @PutMapping("/{planId}/entries/batch-update")
    @PreAuthorize("hasAuthority('schedule:entry:edit')")
    public Result<SchedulePlanService.BatchUpdateResult> batchUpdateEntries(
            @Parameter(description = "方案ID") @PathVariable String planId,
            @Valid @RequestBody ScheduleEntryBatchUpdateDTO batchUpdateDTO) {
        try {
            SchedulePlanService.BatchUpdateResult result =
                    schedulePlanService.batchUpdateEntries(planId, batchUpdateDTO);
            return Result.success(result, "批量更新完成");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
