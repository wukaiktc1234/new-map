package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 应收账款实体类
 * 管理客户欠款，支持账龄分析和催款提醒
 */
@TableName("receivables")
public class Receivable extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 应收ID */
    @TableId(type = IdType.AUTO)
    private Long receivableId;

    /** 应收编号，唯一标识 */
    private String receivableNo;

    /**
     * 客户类型
     * 1-普通客户 2-会员 3-企业客户
     */
    private Integer customerType;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 关联订单ID */
    private Long orderId;

    /** 原始应收金额（单位：分） */
    private Long originalAmount;

    /** 已收金额（单位：分） */
    private Long receivedAmount;

    /** 余额（单位：分） */
    private Long balanceAmount;

    /** 逾期天数 */
    private Integer overdueDays;

    /**
     * 状态
     * 1-正常 2-逾期 3-坏账 4-已核销
     */
    private Integer status;

    /** 到期日 */
    private LocalDate dueDate;

    /** 最后付款日期 */
    private LocalDate lastPaymentDate;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public String getReceivableNo() {
        return receivableNo;
    }

    public void setReceivableNo(String receivableNo) {
        this.receivableNo = receivableNo;
    }

    public Integer getCustomerType() {
        return customerType;
    }

    public void setCustomerType(Integer customerType) {
        this.customerType = customerType;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Long originalAmount) {
        this.originalAmount = originalAmount;
    }

    public Long getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(Long receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public Long getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(Long balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public Integer getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
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

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
