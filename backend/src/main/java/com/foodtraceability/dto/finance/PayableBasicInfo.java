package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应付账款基本信息DTO（用于缓存和批量查询）
 * 仅包含跨模块共享的必要字段，避免传输完整实体
 */
@Schema(description = "应付账款基本信息")
public class PayableBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应付ID */
    @Schema(description = "应付ID", example = "1")
    private Long payableId;

    /** 供应商ID */
    @Schema(description = "供应商ID", example = "1001")
    private Long supplierId;

    /** 原始应付金额（单位：分） */
    @Schema(description = "原始应付金额（分）", example = "500000")
    private Long amount;

    /** 已付金额（单位：分） */
    @Schema(description = "已付金额（分）", example = "200000")
    private Long paidAmount;

    /** 状态：1-待付 2-部分支付 3-已付清 4-逾期 */
    @Schema(description = "状态: 1待付 2部分支付 3已付清 4逾期", example = "2")
    private Integer status;

    /** 到期日 */
    @Schema(description = "到期日", example = "2026-07-15")
    private LocalDate dueDate;

    public Long getPayableId() {
        return payableId;
    }

    public void setPayableId(Long payableId) {
        this.payableId = payableId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public String toString() {
        return "PayableBasicInfo{" +
                "payableId=" + payableId +
                ", supplierId=" + supplierId +
                ", amount=" + amount +
                ", paidAmount=" + paidAmount +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}
