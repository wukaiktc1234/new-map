package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应收账款基本信息DTO（用于缓存和批量查询）
 * 仅包含跨模块共享的必要字段，避免传输完整实体
 */
@Schema(description = "应收账款基本信息")
public class ReceivableBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应收ID */
    @Schema(description = "应收ID", example = "1")
    private Long receivableId;

    /** 客户ID */
    @Schema(description = "客户ID", example = "2001")
    private Long customerId;

    /** 原始应收金额（单位：分） */
    @Schema(description = "原始应收金额（分）", example = "800000")
    private Long amount;

    /** 已收金额（单位：分） */
    @Schema(description = "已收金额（分）", example = "300000")
    private Long receivedAmount;

    /** 状态：1-正常 2-逾期 3-坏账 4-已核销 */
    @Schema(description = "状态: 1正常 2逾期 3坏账 4已核销", example = "1")
    private Integer status;

    /** 到期日 */
    @Schema(description = "到期日", example = "2026-08-20")
    private LocalDate dueDate;

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(Long receivedAmount) {
        this.receivedAmount = receivedAmount;
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
        return "ReceivableBasicInfo{" +
                "receivableId=" + receivableId +
                ", customerId=" + customerId +
                ", amount=" + amount +
                ", receivedAmount=" + receivedAmount +
                ", status=" + status +
                ", dueDate=" + dueDate +
                '}';
    }
}
