package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.util.WorkLocationFilterHelper;
import com.foodtraceability.dto.PurchaseArrivalCloseDTO;
import com.foodtraceability.dto.PurchaseArrivalCreateDTO;
import com.foodtraceability.dto.PurchaseArrivalQueryDTO;
import com.foodtraceability.dto.PurchaseArrivalUpdateDTO;
import com.foodtraceability.entity.PurchaseArrival;
import com.foodtraceability.service.PurchaseArrivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购到货单控制器
 * 仅负责到货登记相关操作，不处理库存和财务
 */
@RestController
@RequestMapping("/v1/purchase/arrivals")
@Tag(name = "采购到货单", description = "采购到货登记相关接口")
public class PurchaseArrivalController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseArrivalController.class);

    private final PurchaseArrivalService purchaseArrivalService;
    private final WorkLocationFilterHelper workLocationFilterHelper;

    public PurchaseArrivalController(PurchaseArrivalService purchaseArrivalService,
                                     WorkLocationFilterHelper workLocationFilterHelper) {
        this.purchaseArrivalService = purchaseArrivalService;
        this.workLocationFilterHelper = workLocationFilterHelper;
    }

    /**
     * 分页查询采购到货单
     * 门店/仓库人员自动按工作归属过滤，采购员/管理员可查看全部
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "分页查询采购到货单")
    public Result<Page<PurchaseArrival>> getArrivalPage(PurchaseArrivalQueryDTO queryDTO) {
        workLocationFilterHelper.applyArrivalFilter(queryDTO);
        return Result.success(purchaseArrivalService.getArrivalPage(queryDTO));
    }

    /**
     * 根据ID获取到货单详情
     */
    @GetMapping("/{arrivalId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "获取到货单详情")
    public Result<PurchaseArrival> getArrivalDetail(
            @Parameter(description = "到货单ID") @PathVariable Long arrivalId) {
        return Result.success(purchaseArrivalService.getArrivalDetail(arrivalId));
    }

    /**
     * 根据采购订单创建到货单
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    @Operation(summary = "根据采购订单创建到货单")
    public Result<List<PurchaseArrival>> createArrivalsFromOrder(
            @Parameter(description = "创建参数") @Valid @RequestBody PurchaseArrivalCreateDTO createDTO) {
        return Result.success(purchaseArrivalService.createArrivalsFromOrder(createDTO));
    }

    /**
     * 更新到货单物流信息
     */
    @PutMapping("/{arrivalId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    @Operation(summary = "更新到货单物流信息")
    public Result<PurchaseArrival> updateArrival(
            @Parameter(description = "到货单ID") @PathVariable Long arrivalId,
            @Parameter(description = "更新参数") @Valid @RequestBody PurchaseArrivalUpdateDTO updateDTO) {
        return Result.success(purchaseArrivalService.updateArrival(arrivalId, updateDTO));
    }

    /**
     * 手动关闭到货单
     */
    @PutMapping("/{arrivalId}/close")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    @Operation(summary = "手动关闭待收货到货单")
    public Result<PurchaseArrival> closeArrival(
            @Parameter(description = "到货单ID") @PathVariable Long arrivalId,
            @Parameter(description = "关闭参数") @Valid @RequestBody PurchaseArrivalCloseDTO closeDTO) {
        return Result.success(purchaseArrivalService.closeArrival(arrivalId, closeDTO));
    }

    /**
     * 到货单质检（1通过/2失败）
     */
    @PutMapping("/{arrivalId}/quality-check")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'qc_user')")
    @Operation(summary = "到货单质检（1通过/2失败）")
    public Result<PurchaseArrival> qualityCheck(
            @Parameter(description = "到货单ID") @PathVariable Long arrivalId,
            @Parameter(description = "质检结果：1通过 2失败") @RequestParam Integer result,
            @Parameter(description = "质检备注") @RequestParam(required = false) String remark) {
        try {
            return Result.success(purchaseArrivalService.qualityCheck(arrivalId, result, remark));
        } catch (Exception e) {
            log.error("到货单质检失败，arrivalId={}", arrivalId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "质检操作失败");
        }
    }

    /**
     * 到货单确认入库：加库存（仓库+门店/unit_cost）+ 应付 + 回写订单实收 + 追溯码事件
     */
    @PutMapping("/{arrivalId}/confirm")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_manager')")
    @Operation(summary = "到货单确认入库")
    public Result<PurchaseArrival> confirmArrival(
            @Parameter(description = "到货单ID") @PathVariable Long arrivalId) {
        try {
            return Result.success(purchaseArrivalService.confirmArrival(arrivalId));
        } catch (Exception e) {
            log.error("到货单确认入库失败，arrivalId={}", arrivalId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "确认入库失败");
        }
    }
}
