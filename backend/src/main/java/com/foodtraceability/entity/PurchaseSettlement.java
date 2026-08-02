package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购结算单实体类
 * 用于管理供应商应付账款的结算流程：创建 -> 部分付款 -> 财务审核 -> 完成
 *
 * <p>状态编码（INTEGER）：
 * <ul>
 *   <li>0 - 待结算（pending）</li>
 *   <li>1 - 部分结算（partial）</li>
 *   <li>2 - 财务审核中（finance_reviewing）</li>
 *   <li>3 - 已完成（completed）</li>
 *   <li>4 - 已逾期（overdue）</li>
 * </ul>
 *
 * <p>发票状态（INTEGER）：
 * <ul>
 *   <li>0 - 未开票</li>
 *   <li>1 - 已开票</li>
 *   <li>2 - 已收票</li>
 * </ul>
 *
 * <p>金额单位：分（BIGINT），前端显示元（number）</p>
 */
@TableName("purchase_settlements")
@Schema(description = "采购结算单实体")
public class PurchaseSettlement {

    /** 结算单主键ID（自增） */
    @TableId(value = "settlement_id", type = IdType.AUTO)
    @Schema(description = "结算单主键ID", example = "1")
    private Long settlementId;

    /** 结算单编号（对外唯一，如 STL20260629001） */
    @TableField("settlement_no")
    @Schema(description = "结算单编号", example = "STL20260629001")
    private String settlementNo;

    /** 关联的采购订单ID */
    @TableField("order_id")
    @Schema(description = "采购订单ID", example = "1")
    private Long orderId;

    /** 采购订单编号（冗余字段，前端展示用） */
    @TableField("order_no")
    @Schema(description = "采购订单编号", example = "PO20260601001")
    private String orderNo;

    /** 供应商ID */
    @TableField("supplier_id")
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /** 供应商名称（冗余字段，前端展示用） */
    @TableField("supplier_name")
    @Schema(description = "供应商名称", example = "绿源蔬菜批发")
    private String supplierName;

    /** 总金额（单位：分） */
    @TableField("total_amount")
    @Schema(description = "总金额（分）", example = "159000")
    private Long totalAmount;

    /** 已付金额（单位：分） */
    @TableField("paid_amount")
    @Schema(description = "已付金额（分）", example = "0")
    private Long paidAmount;

    /** 未付金额（单位：分） */
    @TableField("unpaid_amount")
    @Schema(description = "未付金额（分）", example = "159000")
    private Long unpaidAmount;

    /** 到期日期 */
    @TableField("due_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "到期日期")
    private LocalDate dueDate;

    /** 状态（0待结算 1部分结算 2财务审核中 3已完成 4逾期） */
    @TableField("status")
    @Schema(description = "状态（0-待结算, 1-部分结算, 2-财务审核中, 3-已完成, 4-已逾期）", example = "0")
    private Integer status;

    /** 付款方式 */
    @TableField("payment_method")
    @Schema(description = "付款方式", example = "银行转账")
    private String paymentMethod;

    /** 发票号 */
    @TableField("invoice_no")
    @Schema(description = "发票号")
    private String invoiceNo;

    /** 发票状态（0未开票 1已开票 2已收票） */
    @TableField("invoice_status")
    @Schema(description = "发票状态（0-未开票, 1-已开票, 2-已收票）", example = "0")
    private Integer invoiceStatus;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建人ID */
    @TableField("create_user_id")
    @Schema(description = "创建人ID", example = "1")
    private Long createUserId;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0未删除 1已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
