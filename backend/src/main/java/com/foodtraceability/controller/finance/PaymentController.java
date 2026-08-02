package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.PaymentCreateDTO;
import com.foodtraceability.dto.finance.PaymentVoidDTO;
import com.foodtraceability.dto.finance.PaymentVO;
import com.foodtraceability.service.finance.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 付款单Controller
 * 提供应付账款付款登记、付款历史查询等功能
 */
@Tag(name = "付款管理", description = "应付账款付款登记、四账联动、付款历史查询")
@RestController
@RequestMapping("/v1/finance/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "登记付款", description = "登记向供应商的付款，触发四账联动：应付账款+银行账户+资金流水+会计凭证")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:payment:create')")
    public Result<PaymentVO> registerPayment(@Valid @RequestBody PaymentCreateDTO dto) {
        PaymentVO vo = paymentService.registerPayment(dto);
        return Result.success(vo);
    }

    @Operation(summary = "获取付款单详情", description = "根据付款单ID获取详情")
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAuthority('finance:payment:query')")
    public Result<PaymentVO> getDetail(@PathVariable Long paymentId) {
        PaymentVO vo = paymentService.getDetail(paymentId);
        return Result.success(vo);
    }

    @Operation(summary = "查询付款历史", description = "查询某笔应付账款的所有付款记录")
    @GetMapping("/by-payable/{payableId}")
    @PreAuthorize("hasAuthority('finance:payment:query')")
    public Result<List<PaymentVO>> getPaymentHistory(@PathVariable Long payableId) {
        List<PaymentVO> list = paymentService.getPaymentHistoryByPayableId(payableId);
        return Result.success(list);
    }

    @Operation(summary = "更新付款单", description = "更新付款单的可编辑字段（金额/方式/银行账户/日期/备注）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:payment:edit')")
    public Result<PaymentVO> update(@PathVariable Long id, @Valid @RequestBody PaymentCreateDTO dto) {
        PaymentVO vo = paymentService.update(id, dto);
        return Result.success(vo);
    }

    @Operation(summary = "删除付款单", description = "逻辑删除付款单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:payment:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        paymentService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "作废付款单", description = "作废已确认付款单，触发四账联动回滚：应付账款+银行账户+资金流水+会计凭证")
    @PostMapping("/{paymentId}/void")
    @PreAuthorize("hasAuthority('finance:payment:void')")
    public Result<PaymentVO> voidPayment(@PathVariable Long paymentId, @Valid @RequestBody PaymentVoidDTO dto) {
        PaymentVO vo = paymentService.voidPayment(paymentId, dto.getRemark());
        return Result.success(vo);
    }
}
