package com.foodtraceability.dto.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.foodtraceability.entity.PurchaseSettlement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购结算单响应 DTO
 *
 * <p>用于 Controller 返回给前端的数据结构，与实体类字段一致。
 * 通过 {@link #fromEntity(PurchaseSettlement)} 静态工厂方法从实体转换。</p>
 *
 * <p>金额单位：分（Long），前端 DataConverter 转换为元显示。
 * 状态字段为后端数字编码，前端 DataConverter 转换为语义化字符串。</p>
 */
@Schema(description = "采购结算单响应 DTO")
public class PurchaseSettlementDTO {

    /** 结算单主键ID */
    @Schema(description = "结算单主键ID", example = "1")
    private Long settlementId;

    /** 结算单编号 */
    @Schema(description = "结算单编号", example = "STL20260629001")
    private String settlementNo;

    /** 采购订单ID */
    @Schema(description = "采购订单ID")
    private Long orderId;

    /** 采购订单编号 */
    @Schema(description = "采购订单编号")
    private String orderNo;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 总金额（单位：分） */
    @Schema(description = "总金额（分）")
    private Long totalAmount;

    /** 已付金额（单位：分） */
    @Schema(description = "已付金额（分）")
    private Long paidAmount;

    /** 未付金额（单位：分） */
    @Schema(description = "未付金额（分）")
    private Long unpaidAmount;

    /** 到期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "到期日期")
    private LocalDate dueDate;

    /** 状态（0待结算 1部分结算 2财务审核中 3已完成 4逾期） */
    @Schema(description = "状态")
    private Integer status;

    /** 付款方式 */
    @Schema(description = "付款方式")
    private String paymentMethod;

    /** 发票号 */
    @Schema(description = "发票号")
    private String invoiceNo;

    /** 发票状态（0未开票 1已开票 2已收票） */
    @Schema(description = "发票状态")
    private Integer invoiceStatus;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建人ID */
    @Schema(description = "创建人ID")
    private Long createUserId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 从实体转换到 DTO
     * @param entity 采购结算单实体
     * @return 采购结算单 DTO
     */
    public static PurchaseSettlementDTO fromEntity(PurchaseSettlement entity) {
        if (entity == null) {
            return null;
        }
        PurchaseSettlementDTO dto = new PurchaseSettlementDTO();
        dto.settlementId = entity.getSettlementId();
        dto.settlementNo = entity.getSettlementNo();
        dto.orderId = entity.getOrderId();
        dto.orderNo = entity.getOrderNo();
        dto.supplierId = entity.getSupplierId();
        dto.supplierName = entity.getSupplierName();
        dto.totalAmount = entity.getTotalAmount();
        dto.paidAmount = entity.getPaidAmount();
        dto.unpaidAmount = entity.getUnpaidAmount();
        dto.dueDate = entity.getDueDate();
        dto.status = entity.getStatus();
        dto.paymentMethod = entity.getPaymentMethod();
        dto.invoiceNo = entity.getInvoiceNo();
        dto.invoiceStatus = entity.getInvoiceStatus();
        dto.remark = entity.getRemark();
        dto.createUserId = entity.getCreateUserId();
        dto.createTime = entity.getCreateTime();
        dto.updateTime = entity.getUpdateTime();
        return dto;
    }

    // ==================== Getter & Setter ====================

    public Long getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(Long settlementId) {
        this.settlementId = settlementId;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Long getUnpaidAmount() {
        return unpaidAmount;
    }

    public void setUnpaidAmount(Long unpaidAmount) {
        this.unpaidAmount = unpaidAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Integer getInvoiceStatus() {
        return invoiceStatus;
    }

    public void setInvoiceStatus(Integer invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
