package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 异常单处理流水（补收款 RESOLVE / 作废 VOID），一单一条（uk_exception_ops_order 部分唯一索引）。
 *
 * <p>金额一律「分」（bigint），禁止浮点 —— 见 docs/audit/money-unit-boundary.md。</p>
 */
@TableName("exception_ops")
public class ExceptionOps {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderId;
    private String opType;
    private String idempotencyKey;
    private String reason;
    private String note;
    private Long amountFen;
    private String payType;
    private Long cashReceivedFen;
    private String operatorId;
    private Long shiftId;
    private String flowNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getOpType() { return opType; }
    public void setOpType(String opType) { this.opType = opType; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getAmountFen() { return amountFen; }
    public void setAmountFen(Long amountFen) { this.amountFen = amountFen; }
    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }
    public Long getCashReceivedFen() { return cashReceivedFen; }
    public void setCashReceivedFen(Long cashReceivedFen) { this.cashReceivedFen = cashReceivedFen; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public String getFlowNo() { return flowNo; }
    public void setFlowNo(String flowNo) { this.flowNo = flowNo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
