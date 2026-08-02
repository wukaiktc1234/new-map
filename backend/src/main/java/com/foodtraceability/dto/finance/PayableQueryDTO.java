package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应付账款查询DTO
 */
@Schema(description = "应付账款查询条件")
public class PayableQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页条数")
    private Integer size = 10;

    @Schema(description = "供应商名称（模糊查询）")
    private String supplierName;

    @Schema(description = "状态: 1待付 2部分支付 3已付清 4逾期")
    private Integer status;

    @Schema(description = "起始日期（创建时间）")
    private LocalDate startDate;

    @Schema(description = "结束日期（创建时间）")
    private LocalDate endDate;

    @Schema(description = "起始到期日")
    private LocalDate startDueDate;

    @Schema(description = "结束到期日")
    private LocalDate endDueDate;

    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getStartDueDate() { return startDueDate; }
    public void setStartDueDate(LocalDate startDueDate) { this.startDueDate = startDueDate; }
    public LocalDate getEndDueDate() { return endDueDate; }
    public void setEndDueDate(LocalDate endDueDate) { this.endDueDate = endDueDate; }
}
