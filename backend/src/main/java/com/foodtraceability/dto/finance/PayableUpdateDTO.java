package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应付账款更新DTO
 */
@Schema(description = "应付账款更新请求")
public class PayableUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "应付账款ID", required = true)
    @NotNull(message = "应付账款ID不能为空")
    private Long payableId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "到期日")
    private LocalDate dueDate;

    @Schema(description = "账期天数")
    private Integer paymentTerm;

    @Schema(description = "备注")
    private String remark;

    public Long getPayableId() { return payableId; }
    public void setPayableId(Long payableId) { this.payableId = payableId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getPaymentTerm() { return paymentTerm; }
    public void setPaymentTerm(Integer paymentTerm) { this.paymentTerm = paymentTerm; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
