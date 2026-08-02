package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PurchaseReturnApproveDTO;
import com.foodtraceability.dto.PurchaseReturnCreateDTO;
import com.foodtraceability.dto.PurchaseReturnVO;
import com.foodtraceability.service.PurchaseReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 采购退货管理接口
 */
@RestController
@RequestMapping("/v1/purchase/returns")
@Tag(name = "采购退货管理")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    public PurchaseReturnController(PurchaseReturnService purchaseReturnService) {
        this.purchaseReturnService = purchaseReturnService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询采购退货")
    public Result<Page<PurchaseReturnVO>> getPurchaseReturnPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String returnNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String stockinNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(purchaseReturnService.getPurchaseReturnPage(
                page, pageSize, returnNo, supplierId, stockinNo, status, startDate, endDate));
    }

    @PostMapping
    @Operation(summary = "创建采购退货")
    public Result<PurchaseReturnVO> createPurchaseReturn(
            @Valid @RequestBody PurchaseReturnCreateDTO dto) {
        return Result.success(purchaseReturnService.createPurchaseReturn(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新采购退货")
    public Result<PurchaseReturnVO> updatePurchaseReturn(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseReturnCreateDTO dto) {
        return Result.success(purchaseReturnService.updatePurchaseReturn(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取采购退货详情")
    public Result<PurchaseReturnVO> getPurchaseReturnById(@PathVariable Long id) {
        return Result.success(purchaseReturnService.getPurchaseReturnById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购退货")
    public Result<Void> deletePurchaseReturn(@PathVariable Long id) {
        purchaseReturnService.deletePurchaseReturn(id);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批采购退货")
    public Result<PurchaseReturnVO> approvePurchaseReturn(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseReturnApproveDTO dto) {
        return Result.success(purchaseReturnService.approvePurchaseReturn(id, dto));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成采购退货（现金退款到账确认）")
    public Result<PurchaseReturnVO> completePurchaseReturn(@PathVariable Long id) {
        return Result.success(purchaseReturnService.completePurchaseReturn(id));
    }
}
