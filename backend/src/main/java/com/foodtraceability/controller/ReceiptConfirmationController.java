package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.common.util.WorkLocationFilterHelper;
import com.foodtraceability.dto.ReceiptConfirmationCreateDTO;
import com.foodtraceability.dto.ReceiptConfirmationQueryDTO;
import com.foodtraceability.dto.ReceiptSignatureDTO;
import com.foodtraceability.dto.ReceiptVerifyVO;
import com.foodtraceability.entity.ReceiptConfirmation;
import com.foodtraceability.entity.ReceiptSignature;
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

import java.util.List;

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

    /**
     * 新增收货确认签名（收货方/供应商双签）
     */
    @PostMapping("/{confirmationId}/signatures")
    @PreAuthorize("hasAnyRole('admin', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "新增收货确认签名")
    public Result<ReceiptSignature> addSignature(
            @Parameter(description = "确认单ID") @PathVariable Long confirmationId,
            @Parameter(description = "签名参数") @Valid @RequestBody ReceiptSignatureDTO signatureDTO) {
        return Result.success(receiptConfirmationService.addSignature(confirmationId, signatureDTO));
    }

    /**
     * 查询收货确认单签名存证列表
     */
    @GetMapping("/{confirmationId}/signatures")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "查询收货确认单签名存证列表")
    public Result<List<ReceiptSignature>> listSignatures(
            @Parameter(description = "确认单ID") @PathVariable Long confirmationId) {
        return Result.success(receiptConfirmationService.listSignatures(confirmationId));
    }

    /**
     * 登记一次打印（打印前调用，服务端计数并记录打印时间，防打印泛滥）
     */
    @PostMapping("/{confirmationId}/print-records")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'store_manager', 'warehouse_manager')")
    @Operation(summary = "登记确认单打印次数")
    public Result<ReceiptConfirmation> recordPrint(
            @Parameter(description = "确认单ID") @PathVariable Long confirmationId) {
        return Result.success(receiptConfirmationService.recordPrint(confirmationId));
    }

    /**
     * 防伪码查验：输入 16 位防伪码查询单据基本信息与完整打印历史
     * <p>免登录（SecurityConfig 已放行 /v1/receipt-confirmations/verify/**），仅靠限流防暴力遍历。</p>
     */
    @GetMapping("/verify/{verifyCode}")
    @Operation(summary = "防伪码查验")
    public Result<ReceiptVerifyVO> verifyByCode(
            @Parameter(description = "16位防伪码") @PathVariable String verifyCode) {
        return Result.success(receiptConfirmationService.verifyByCode(verifyCode));
    }
}
