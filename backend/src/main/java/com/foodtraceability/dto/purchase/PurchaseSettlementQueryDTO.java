package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 采购结算单查询 DTO（分页参数）
 *
 * <p>状态字段 status 为后端数字编码（0~4），由前端 DataConverter 转换。
 * 发票状态字段 invoiceStatus 为后端数字编码（0~2），由前端 DataConverter 转换。</p>
 */
@Schema(description = "采购结算单查询 DTO")
public class PurchaseSettlementQueryDTO {

    /** 当前页码（默认1） */
    @Schema(description = "当前页码", example = "1")
    private Long current = 1L;

    /** 每页条数（默认10） */
    @Schema(description = "每页条数", example = "10")
    private Long size = 10L;

    /** 结算单编号（模糊匹配） */
    @Schema(description = "结算单编号")
    private String settlementNo;

    /** 采购订单编号（模糊匹配） */
    @Schema(description = "采购订单编号")
    private String orderNo;

    /** 供应商名称（模糊匹配） */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 状态（后端数字编码 0~4） */
    @Schema(description = "状态")
    private Integer status;

    /** 发票状态（后端数字编码 0~2） */
    @Schema(description = "发票状态")
    private Integer invoiceStatus;

    /** 开始日期（到期日期范围筛选） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /** 结束日期（到期日期范围筛选） */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /** 搜索关键词（结算单编号/订单编号/供应商名称模糊匹配） */
    @Schema(description = "搜索关键词")
    private String keyword;

    // ==================== Getter & Setter ====================

    public Long getCurrent() {
        return current;
    }

    public void setCurrent(Long current) {
        this.current = current;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(Integer invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
