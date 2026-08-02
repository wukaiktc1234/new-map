package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款更新DTO
 */
@Schema(description = "应收账款更新参数")
public class ReceivableUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "应收ID")
    private Long receivableId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "原始应收金额（单位：分）")
    private BigDecimal originalAmount;

    @Schema(description = "备注")
    private String remark;

    public Long getReceivableId() { return receivableId; }
    public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
