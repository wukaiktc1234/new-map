package com.foodtraceability.controller.purchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.purchase.PurchasePlanCreateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanQueryDTO;
import com.foodtraceability.dto.purchase.PurchasePlanUpdateDTO;
import com.foodtraceability.dto.purchase.PurchasePlanVO;
import com.foodtraceability.service.purchase.PurchasePlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购计划控制器
 *
 * <p>提供 8 个 RESTful 端点：
 * <ul>
 *   <li>GET  /v1/purchase/plans          分页查询</li>
 *   <li>GET  /v1/purchase/plans/{id}     查询详情（含明细）</li>
 *   <li>POST /v1/purchase/plans           创建</li>
 *   <li>PUT  /v1/purchase/plans/{id}      更新（仅草稿状态）</li>
 *   <li>DELETE /v1/purchase/plans/{id}   删除（仅草稿/已拒绝状态）</li>
 *   <li>PUT  /v1/purchase/plans/{id}/submit   提交审批</li>
 *   <li>PUT  /v1/purchase/plans/{id}/approve  审批通过</li>
 *   <li>PUT  /v1/purchase/plans/{id}/reject   审批拒绝</li>
 *   <li>PUT  /v1/purchase/plans/{id}/execute  开始执行</li>
 * </ul>
 * </p>
 */
@Tag(name = "采购计划管理", description = "采购计划相关接口")
@RestController
@RequestMapping("/v1/purchase/plans")
public class PurchasePlanController {

    private static final Logger log = LoggerFactory.getLogger(PurchasePlanController.class);

    private final PurchasePlanService planService;

    public PurchasePlanController(PurchasePlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "分页查询采购计划列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager')")
    public Result<IPage<PurchasePlanVO>> getPlanPage(@Valid PurchasePlanQueryDTO queryDTO) {
        try {
            IPage<PurchasePlanVO> result = planService.getPlanPage(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询采购计划列表失败", e);
            return Result.error("查询采购计划列表失败");
        }
    }

    @Operation(summary = "查询采购计划详情（含明细）")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchasePlanVO> getPlanById(@Parameter(description = "计划ID") @PathVariable Long id) {
        try {
            PurchasePlanVO vo = planService.getPlanDetail(id);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("查询采购计划详情失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询采购计划详情失败");
        }
    }

    @Operation(summary = "创建采购计划")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<PurchasePlanVO> createPlan(@Valid @RequestBody PurchasePlanCreateDTO createDTO) {
        try {
            PurchasePlanVO vo = planService.createPlan(createDTO);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("创建采购计划失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建采购计划失败");
        }
    }

    @Operation(summary = "更新采购计划（仅草稿状态可更新）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<PurchasePlanVO> updatePlan(@Parameter(description = "计划ID") @PathVariable Long id,
                                              @Valid @RequestBody PurchasePlanUpdateDTO updateDTO) {
        try {
            PurchasePlanVO vo = planService.updatePlan(id, updateDTO);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("更新采购计划失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新采购计划失败");
        }
    }

    @Operation(summary = "删除采购计划（仅草稿/已拒绝状态可删除）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> deletePlan(@Parameter(description = "计划ID") @PathVariable Long id) {
        try {
            planService.deletePlan(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除采购计划失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除采购计划失败");
        }
    }

    @Operation(summary = "提交审批（草稿 → 待审批）")
    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> submitPlan(@Parameter(description = "计划ID") @PathVariable Long id) {
        try {
            planService.submitPlan(id);
            return Result.success();
        } catch (Exception e) {
            log.error("提交采购计划审批失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "提交审批失败");
        }
    }

    @Operation(summary = "审批通过（待审批 → 已审批）")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'general_manager')")
    public Result<Void> approvePlan(@Parameter(description = "计划ID") @PathVariable Long id,
                                      @Parameter(description = "审批人") @RequestParam(required = false) String approvedBy) {
        try {
            planService.approvePlan(id, approvedBy);
            return Result.success();
        } catch (Exception e) {
            log.error("审批通过失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "审批通过失败");
        }
    }

    @Operation(summary = "审批拒绝（待审批 → 已拒绝）")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'general_manager')")
    public Result<Void> rejectPlan(@Parameter(description = "计划ID") @PathVariable Long id,
                                      @Parameter(description = "拒绝原因") @RequestParam String reason) {
        try {
            planService.rejectPlan(id, reason);
            return Result.success();
        } catch (Exception e) {
            log.error("审批拒绝失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "审批拒绝失败");
        }
    }

    @Operation(summary = "开始执行（已审批 → 执行中）")
    @PutMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> executePlan(@Parameter(description = "计划ID") @PathVariable Long id) {
        try {
            planService.executePlan(id);
            return Result.success();
        } catch (Exception e) {
            log.error("开始执行采购计划失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "开始执行失败");
        }
    }

    @Operation(summary = "从库存预警生成采购计划（草稿）")
    @PostMapping("/generate-from-stock")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<PurchasePlanVO> generateFromStock() {
        try {
            PurchasePlanVO vo = planService.generateFromStock();
            return Result.success(vo);
        } catch (Exception e) {
            log.error("从库存预警生成采购计划失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "生成采购计划失败");
        }
    }

    @Operation(summary = "导出采购计划 Excel")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public void exportPlans(@Valid PurchasePlanQueryDTO queryDTO, jakarta.servlet.http.HttpServletResponse response) {
        planService.exportPlans(queryDTO, response);
    }

    @Operation(summary = "根据采购计划生成采购订单（草稿）")
    @PostMapping("/{id}/generate-order")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<com.foodtraceability.entity.PurchaseOrder> generateOrder(@Parameter(description = "计划ID") @PathVariable Long id) {
        try {
            com.foodtraceability.entity.PurchaseOrder order = planService.generateOrder(id);
            return Result.success(order);
        } catch (Exception e) {
            log.error("采购计划生成采购订单失败，ID：{}", id, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "生成采购订单失败");
        }
    }
}
