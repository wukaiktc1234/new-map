package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** 未达账项（人工指定时使用；自动识别时由服务端生成） */
public class StatementItemRequest {
    /** A / B / C / D */
    private String category;
    /** A/B 类填 fund_flows.flow_id */
    private Long flowId;
    /** C/D 类填 bank_transactions.transaction_id */
    private Long transactionId;
    /** 金额（分，正数；方向由 category 决定） */
    private Long amount;
    /** 业务发生日 */
    private LocalDate businessDate;
    /** **挂账人**（V6 必填） */
    private Long ownerUserId;
    /** **原因**（V6 必填） */
    private String reason;

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public Long getFlowId() { return flowId; }
    public void setFlowId(Long v) { this.flowId = v; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }
    public Long getAmount() { return amount; }
    public void setAmount(Long v) { this.amount = v; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate v) { this.businessDate = v; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long v) { this.ownerUserId = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
