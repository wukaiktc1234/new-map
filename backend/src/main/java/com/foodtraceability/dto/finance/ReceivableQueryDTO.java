package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应收账款查询DTO
 */
@Schema(description = "应收账款查询条件")
public class ReceivableQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户名称（模糊查询）")
    private String customerName;

    @Schema(description = "客户类型: 1普通 2会员 3企业")
    private Integer customerType;

    @Schema(description = "状态: 1正常 2逾期 3坏账 4已核销")
    private Integer status;

    @Schema(description = "起始到期日")
    private LocalDate startDueDate;

    @Schema(description = "结束到期日")
    private LocalDate endDueDate;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Integer getCustomerType() { return customerType; }
    public void setCustomerType(Integer customerType) { this.customerType = customerType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getStartDueDate() { return startDueDate; }
    public void setStartDueDate(LocalDate startDueDate) { this.startDueDate = startDueDate; }
    public LocalDate getEndDueDate() { return endDueDate; }
    public void setEndDueDate(LocalDate endDueDate) { this.endDueDate = endDueDate; }
}
