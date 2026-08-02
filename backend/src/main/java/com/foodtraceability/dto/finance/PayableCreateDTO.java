package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应付账款创建DTO
 */
@Schema(description = "应付账款创建请求")
public class PayableCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "供应商ID", required = true)
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    @Schema(description = "供应商名称", required = true)
    @NotNull(message = "供应商名称不能为空")
    private String supplierName;

    @Schema(description = "关联采购单ID")
    private Long purchaseOrderId;

    @Schema(description = "原始应付金额（单位：分）", required = true)
    @NotNull(message = "应付金额不能为空")
    @Positive(message = "应付金额必须大于0")
    private Long originalAmount;

    @Schema(description = "到期日", required = true)
    @NotNull(message = "到期日不能为空")
    private LocalDate dueDate;

    @Schema(description = "账期天数")
    private Integer paymentTerm;

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public Long getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Long originalAmount) { this.originalAmount = originalAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getPaymentTerm() { return paymentTerm; }
    public void setPaymentTerm(Integer paymentTerm) { this.paymentTerm = paymentTerm; }
}
