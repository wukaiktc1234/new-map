package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 采购结算单更新 DTO
 *
 * <p>仅允许更新部分字段：总金额、到期日期、付款方式、备注。
 * settlementNo/orderId/orderNo/supplierId/supplierName 不允许更新。
 * 状态、付款金额、发票信息通过专用接口（settle/applyInvoice/receiveInvoice）更新。</p>
 *
 * <p>金额单位：分（Long），前端传入时需将元转换为分。</p>
 */
@Schema(description = "采购结算单更新 DTO")
public class PurchaseSettlementUpdateDTO {

    /** 总金额（单位：分） */
    @Min(value = 0, message = "总金额不能为负数")
    @Schema(description = "总金额（分）", example = "159000")
    private Long totalAmount;

    /** 到期日期 */
    @Schema(description = "到期日期")
    private LocalDate dueDate;

    /** 付款方式 */
    @Size(max = 50, message = "付款方式长度不能超过50")
    @Schema(description = "付款方式", example = "银行转账")
    private String paymentMethod;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
