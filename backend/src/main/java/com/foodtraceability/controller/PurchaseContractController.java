package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.PurchaseContract;
import com.foodtraceability.service.PurchaseContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/purchase/contracts")
@Tag(name = "采购合同管理")
public class PurchaseContractController {


    public PurchaseContractController(PurchaseContractService purchaseContractService) {
        this.purchaseContractService = purchaseContractService;
    }

    private final PurchaseContractService purchaseContractService;

    @GetMapping("/page")
    @Operation(summary = "分页查询采购合同")
    @PreAuthorize("isAuthenticated()")
    public Result<Page<PurchaseContract>> getPurchaseContractPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<PurchaseContract> pageParam = new Page<>(page, pageSize);
        Page<PurchaseContract> result = purchaseContractService.getPurchaseContractPage(pageParam, contractNo, supplierId, status, startDate, endDate);
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "创建采购合同")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<PurchaseContract> createPurchaseContract(@RequestBody PurchaseContract purchaseContract) {
        return Result.success(purchaseContractService.createPurchaseContract(purchaseContract));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新采购合同")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser')")
    public Result<PurchaseContract> updatePurchaseContract(@PathVariable Long id, @RequestBody PurchaseContract purchaseContract) {
        return Result.success(purchaseContractService.updatePurchaseContract(id, purchaseContract));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取采购合同详情")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseContract> getPurchaseContractById(@PathVariable Long id) {
        return Result.success(purchaseContractService.getPurchaseContractById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除采购合同")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> deletePurchaseContract(@PathVariable Long id) {
        purchaseContractService.deletePurchaseContract(id);
        return Result.success();
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署采购合同")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<PurchaseContract> signPurchaseContract(
            @PathVariable Long id,
            @RequestParam String signatory) {
        return Result.success(purchaseContractService.signPurchaseContract(id, signatory));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止采购合同")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<PurchaseContract> terminatePurchaseContract(
            @PathVariable Long id,
            @RequestParam String reason) {
        return Result.success(purchaseContractService.terminatePurchaseContract(id, reason));
    }
}
