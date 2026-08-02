package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 发票查询DTO
 */
@Schema(description = "发票查询条件")
public class FinanceInvoiceQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票类型: 1专票 2普票 3电子 4其他")
    private Integer invoiceType;

    @Schema(description = "发票类别: 1进项 2销项")
    private Integer invoiceCategory;

    @Schema(description = "发票状态: 0待认证 1已认证 2已抵扣 3异常 4已红冲")
    private Integer invoiceStatus;

    @Schema(description = "购买方名称（模糊查询）")
    private String buyerName;

    @Schema(description = "销售方名称（模糊查询）")
    private String sellerName;

    @Schema(description = "起始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "当前页码")
    private Long current = 1L;

    @Schema(description = "每页条数")
    private Long size = 10L;

    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public Integer getInvoiceType() { return invoiceType; }
    public void setInvoiceType(Integer invoiceType) { this.invoiceType = invoiceType; }
    public Integer getInvoiceCategory() { return invoiceCategory; }
    public void setInvoiceCategory(Integer invoiceCategory) { this.invoiceCategory = invoiceCategory; }
    public Integer getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(Integer invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
