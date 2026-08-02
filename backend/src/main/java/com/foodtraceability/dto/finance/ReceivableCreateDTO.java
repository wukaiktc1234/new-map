package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款创建DTO
 * 用于创建应收账款记录的请求参数
 */
public class ReceivableCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 客户名称 */
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /** 应收金额 */
    @NotNull(message = "应收金额不能为空")
    private BigDecimal amount;

    /** 到期日期 */
    private LocalDate dueDate;

    /** 订单号 */
    private String orderNo;

    /** 备注 */
    private String remark;

    /** 客户ID */
    private Long customerId;

    /** 客户类型: individual/company */
    private String customerType;

    /** 原始金额（折扣前） */
    private BigDecimal originalAmount;

    /** 关联订单ID */
    private Long orderId;

    /** 联系电话 */
    private String phone;

    /** 联系地址 */
    private String address;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
