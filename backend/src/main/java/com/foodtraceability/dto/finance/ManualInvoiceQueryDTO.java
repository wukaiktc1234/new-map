package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 手动发票查询DTO
 */
@Schema(description = "手动发票查询条件")
public class ManualInvoiceQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票类型：invoice/vat_special/vat_general/full_electronic")
    private String invoiceType;

    @Schema(description = "起始日期（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getInvoiceType() { return invoiceType; }
    public void setInvoiceType(String invoiceType) { this.invoiceType = invoiceType; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
