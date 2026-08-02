package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.SwapRequestCreateDTO;
import com.foodtraceability.dto.schedule.SwapRequestQueryDTO;
import com.foodtraceability.dto.schedule.SwapRequestVO;
import com.foodtraceability.service.schedule.ScheduleSwapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 排班换班管理控制器
 * 对应前端 API 路径 /v1/schedule/swap
 *
 * <p>端点说明：
 * <ul>
 *   <li>GET /v1/schedule/swap              - 分页查询换班请求</li>
 *   <li>GET /v1/schedule/swap/pending-count - 待处理数量（必须在 /{id} 之前）</li>
 *   <li>GET /v1/schedule/swap/{id}         - 获取换班请求详情</li>
 *   <li>POST /v1/schedule/swap             - 创建换班请求</li>
 *   <li>PUT /v1/schedule/swap/{id}/approve - 审批通过</li>
 *   <li>PUT /v1/schedule/swap/{id}/reject  - 驳回</li>
 *   <li>PUT /v1/schedule/swap/{id}/cancel  - 取消</li>
 * </ul>
 *
 * <p>路径匹配优先级：字面量路径（pending-count）优先于 {id} 匹配
 */
@Tag(name = "排班管理-换班管理", description = "换班申请(F-008)相关接口")
@RestController
@RequestMapping("/v1/schedule/swap")
public class ScheduleSwapController {

    private final ScheduleSwapService scheduleSwapService;

    public ScheduleSwapController(ScheduleSwapService scheduleSwapService) {
        this.scheduleSwapService = scheduleSwapService;
    }

    /**
     * 分页查询换班请求列表
     */
    @Operation(summary = "分页查询换班请求列表")
    @GetMapping
    public Result<PageResult<SwapRequestVO>> getSwapList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "状态筛选: pending/approved/rejected/cancelled")
            @RequestParam(required = false) String status,
            @Parameter(description = "方案ID筛选") @RequestParam(required = false) String planId) {
        try {
            SwapRequestQueryDTO queryDTO = new SwapRequestQueryDTO();
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setStatus(status);
            queryDTO.setPlanId(planId);
            PageResult<SwapRequestVO> result = scheduleSwapService.getSwapList(queryDTO);
            return Result.success(result, "查询换班请求列表成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取待处理换班请求数量
     * 注意：字面量路径优先于 {id} 匹配，确保 /pending-count 不会被误识别为换班请求ID
     */
    @Operation(summary = "获取待处理换班请求数量",
            description = "返回当前用户的待审批换班请求数量，用于红点提示")
    @GetMapping("/pending-count")
    public Result<Map<String, Integer>> getPendingCount() {
        try {
            Integer count = scheduleSwapService.getPendingCount();
            Map<String, Integer> result = new HashMap<>();
            result.put("count", count != null ? count : 0);
            return Result.success(result, "获取待处理数量成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID获取换班请求详情
     */
    @Operation(summary = "获取换班请求详情")
    @GetMapping("/{id}")
    public Result<SwapRequestVO> getSwapById(
            @Parameter(description = "换班请求ID") @PathVariable("id") String id) {
        try {
            SwapRequestVO vo = scheduleSwapService.getSwapById(id);
            if (vo == null) {
                return Result.error("换班请求不存在");
            }
            return Result.success(vo, "获取换班请求详情成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建换班请求
     */
    @Operation(summary = "创建换班请求")
    @PostMapping
    public Result<SwapRequestVO> createSwap(@Valid @RequestBody SwapRequestCreateDTO createDTO) {
        try {
            SwapRequestVO vo = scheduleSwapService.createSwap(createDTO);
            return Result.success(vo, "创建换班请求成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批通过换班请求
     */
    @Operation(summary = "审批通过换班请求")
    @PutMapping("/{id}/approve")
    public Result<SwapRequestVO> approveSwap(
            @Parameter(description = "换班请求ID") @PathVariable("id") String id,
            @RequestBody Map<String, String> body) {
        try {
            String comment = body != null ? body.get("comment") : null;
            SwapRequestVO vo = scheduleSwapService.approveSwap(id, comment);
            return Result.success(vo, "审批通过成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 驳回换班请求
     */
    @Operation(summary = "驳回换班请求")
    @PutMapping("/{id}/reject")
    public Result<SwapRequestVO> rejectSwap(
            @Parameter(description = "换班请求ID") @PathVariable("id") String id,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            if (reason == null || reason.isEmpty()) {
                return Result.error("驳回原因不能为空");
            }
            SwapRequestVO vo = scheduleSwapService.rejectSwap(id, reason);
            return Result.success(vo, "驳回成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消换班请求
     */
    @Operation(summary = "取消换班请求")
    @PutMapping("/{id}/cancel")
    public Result<SwapRequestVO> cancelSwap(
            @Parameter(description = "换班请求ID") @PathVariable("id") String id) {
        try {
            SwapRequestVO vo = scheduleSwapService.cancelSwap(id);
            return Result.success(vo, "取消成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
