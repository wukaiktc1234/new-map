package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PlanItemRequest;
import com.foodtraceability.entity.PlanItem;
import com.foodtraceability.service.PlanItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 计划项Controller
 * 
 * @author demo
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/plan-items")
public class PlanItemController {


    public PlanItemController(PlanItemService planItemService) {
        this.planItemService = planItemService;
    }

    private final PlanItemService planItemService;

    /**
     * 创建计划项
     * 
     * @param request 计划项请求DTO
     * @return 创建的计划项
     */
    @PostMapping
    public Result<PlanItem> createPlanItem(@RequestBody PlanItemRequest request) {
        PlanItem planItem = planItemService.createPlanItem(request);
        return Result.success(planItem);
    }

    /**
     * 更新计划项
     * 
     * @param id      计划项ID
     * @param request 计划项请求DTO
     * @return 更新后的计划项
     */
    @PutMapping("/{id}")
    public Result<PlanItem> updatePlanItem(@PathVariable Long id, @RequestBody PlanItemRequest request) {
        PlanItem planItem = planItemService.updatePlanItem(id, request);
        return Result.success(planItem);
    }

    /**
     * 删除计划项
     * 
     * @param id 计划项ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Map<String, Boolean>> deletePlanItem(@PathVariable Long id) {
        boolean deleted = planItemService.deletePlanItem(id);
        Map<String, Boolean> response = Map.of("deleted", deleted);
        return Result.success(response);
    }

    /**
     * 获取计划项详情
     * 
     * @param id 计划项ID
     * @return 计划项详情
     */
    @GetMapping("/{id}")
    public Result<PlanItem> getPlanItemById(@PathVariable Long id) {
        PlanItem planItem = planItemService.getPlanItemById(id);
        return Result.success(planItem);
    }

    /**
     * 查询计划项列表
     * 
     * @param params 查询参数
     * @return 计划项列表
     */
    @GetMapping
    public Result<List<PlanItem>> listPlanItems(@RequestParam Map<String, Object> params) {
        List<PlanItem> planItems = planItemService.listPlanItems(params);
        return Result.success(planItems);
    }

    /**
     * 统计计划项数量
     * 
     * @param params 统计参数
     * @return 统计结果
     */
    @GetMapping("/count")
    public Result<Map<String, Long>> countPlanItems(@RequestParam Map<String, Object> params) {
        Map<String, Long> countResult = planItemService.countPlanItems(params);
        return Result.success(countResult);
    }

    /**
     * 批量更新计划项状态
     * 
     * @param ids    计划项ID列表
     * @param status 目标状态
     * @return 更新结果
     */
    @PostMapping("/batch-update-status")
    public Result<Map<String, Integer>> batchUpdateStatus(@RequestParam List<Long> ids, @RequestParam String status) {
        int updatedCount = planItemService.batchUpdateStatus(ids, status);
        Map<String, Integer> response = Map.of("updatedCount", updatedCount);
        return Result.success(response);
    }
}
