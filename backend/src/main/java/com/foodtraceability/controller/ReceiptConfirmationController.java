package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.util.WorkLocationFilterHelper;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.entity.ReceiptConfirmation;
import com.foodtraceability.service.ReceiptConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收货确认单控制器
 * 门店收货与库存入库共用，负责实物确认、库存增加、应付生成
 */
@RestController
@RequestMapping("/v1/receipt-confirmations")
@Tag(name = "收货确认单", description = "门店收货/库存入库确认相关接口")
public class ReceiptConfirmationController {

    private final ReceiptConfirmationService receiptConfirmationService;
    private final WorkLocationFilterHelper workLocationFilterHelper;

    public ReceiptConfirmationController(ReceiptConfirmationService receiptConfirmationService,
                                         WorkLocationFilterHelper workLocationFilterHelper) {
        this.receiptConfirmationService = receiptConfirmationService;
        this.workLocationFilterHelper = workLocationFilterHelper;
    }

    /**
     * 分页查询收货确认单
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "分页查询收货确认单")
    public Result<Page<ReceiptConfirmation>> getConfirmationPage(ReceiptConfirmationQueryDTO queryDTO) {
        workLocationFilterHelper.applyConfirmationFilter(queryDTO);
        return Result.success(receiptConfirmationService.getConfirmationPage(queryDTO));
    }

    /**
     * 根据ID获取收货确认单详情
     */
    @GetMapping("/{confirmationId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "获取收货确认单详情")
    public Result<ReceiptConfirmation> getConfirmationDetail(
            @Parameter(description = "确认单ID") @PathVariable Long confirmationId) {
        return Result.success(receiptConfirmationService.getConfirmationDetail(confirmationId));
    }

    /**
     * 创建收货确认单
     * 门店/仓库现场人员根据到货单创建确认单
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "创建收货确认单")
    public Result<ReceiptConfirmation> createConfirmation(
            @Parameter(description = "创建参数") @Valid @RequestBody ReceiptConfirmationCreateDTO createDTO) {
        return Result.success(receiptConfirmationService.createConfirmation(createDTO));
    }
}
