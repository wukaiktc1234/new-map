package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应收账款VO
 */
public class ReceivableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应收ID */
    private Long receivableId;

    /** 应收编号 */
    private String receivableNo;

    /** 客户类型 */
    private Integer customerType;

    /** 客户类型名称 */
    private String customerTypeName;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 关联订单ID */
    private Long orderId;

    /** 原始应收金额（单位：分） */
    private Long originalAmount;

    /** 原始应收金额（元，用于显示） */
    private String originalAmountDisplay;

    /** 已收金额（单位：分） */
    private Long receivedAmount;

    /** 已收金额（元，用于显示） */
    private String receivedAmountDisplay;

    /** 余额（单位：分） */
    private Long balanceAmount;

    /** 余额（元，用于显示） */
    private String balanceAmountDisplay;

    /** 逾期天数 */
    private Integer overdueDays;

    /** 状态 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 到期日 */
    private LocalDate dueDate;

    /** 最后付款日期 */
    private LocalDate lastPaymentDate;

    /** 创建时间 */
    private java.time.LocalDateTime createTime;

    // getter和setter方法（简洁格式）
    public Long getReceivableId() { return receivableId; }
    public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
    public String getReceivableNo() { return receivableNo; }
    public void setReceivableNo(String receivableNo) { this.receivableNo = receivableNo; }
    public Integer getCustomerType() { return customerType; }
    public void setCustomerType(Integer customerType) { this.customerType = customerType; }
    public String getCustomerTypeName() { return customerTypeName; }
    public void setCustomerTypeName(String customerTypeName) { this.customerTypeName = customerTypeName; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Long originalAmount) { this.originalAmount = originalAmount; }
    public String getOriginalAmountDisplay() { return originalAmountDisplay; }
    public void setOriginalAmountDisplay(String originalAmountDisplay) { this.originalAmountDisplay = originalAmountDisplay; }
    public Long getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(Long receivedAmount) { this.receivedAmount = receivedAmount; }
    public String getReceivedAmountDisplay() { return receivedAmountDisplay; }
    public void setReceivedAmountDisplay(String receivedAmountDisplay) { this.receivedAmountDisplay = receivedAmountDisplay; }
    public Long getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(Long balanceAmount) { this.balanceAmount = balanceAmount; }
    public String getBalanceAmountDisplay() { return balanceAmountDisplay; }
    public void setBalanceAmountDisplay(String balanceAmountDisplay) { this.balanceAmountDisplay = balanceAmountDisplay; }
    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
