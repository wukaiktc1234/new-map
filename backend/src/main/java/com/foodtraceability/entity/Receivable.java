package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 应收账款实体类
 * 用于管理企业的应收账款信息
 */
@TableName("receivable")
public class Receivable {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 订单ID
     */
    private Long orderId;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 客户ID
     */
    private Long customerId;
    /**
     * 客户名称
     */
    private String customerName;
    /**
     * 客户类型
     */
    private String customerType;
    /**
     * 应收账款金额
     */
    private BigDecimal amount;
    /**
     * 已收回金额
     */
    private BigDecimal receivedAmount;
    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;
    /**
     * 发票ID
     */
    private Long invoiceId;
    /**
     * 发票编号
     */
    private String invoiceNo;
    /**
     * 应收账款状态
     * - PENDING: 待收回
     * - PARTIALLY_RECEIVED: 部分收回
     * - FULLY_RECEIVED: 全部收回
     * - OVERDUE: 逾期
     */
    private String status;
    /**
     * 到期日
     */
    private Date dueDate;
    /**
     * 逾期天数
     */
    private Integer overdueDays;
    /**
     * 最后收回日期
     */
    private Date lastReceivedDate;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 备注
     */
    private String remark;

    public Date getDueDate() {
        return this.dueDate;
    }

    public Long getId() {
        return this.id;
    }

    public Long getOrderId() {
        return this.orderId;
    }

    public String getOrderNo() {
        return this.orderNo;
    }

    public Long getCustomerId() {
        return this.customerId;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public String getCustomerType() {
        return this.customerType;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getReceivedAmount() {
        return this.receivedAmount;
    }

    public BigDecimal getRemainingAmount() {
        return this.remainingAmount;
    }

    public Long getInvoiceId() {
        return this.invoiceId;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getOverdueDays() {
        return this.overdueDays;
    }

    public Date getLastReceivedDate() {
        return this.lastReceivedDate;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public Date getUpdateTime() {
        return this.updateTime;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastReceivedDate(Date lastReceivedDate) {
        this.lastReceivedDate = lastReceivedDate;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public void setOverdueDays(Integer overdueDays) {
        this.overdueDays = overdueDays;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Receivable() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Receivable)) return false;
        final Receivable other = (Receivable) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$customerId = this.getCustomerId();
        final java.lang.Object other$customerId = other.getCustomerId();
        if (this$customerId == null ? other$customerId != null : !this$customerId.equals(other$customerId)) return false;
        final java.lang.Object this$invoiceId = this.getInvoiceId();
        final java.lang.Object other$invoiceId = other.getInvoiceId();
        if (this$invoiceId == null ? other$invoiceId != null : !this$invoiceId.equals(other$invoiceId)) return false;
        final java.lang.Object this$overdueDays = this.getOverdueDays();
        final java.lang.Object other$overdueDays = other.getOverdueDays();
        if (this$overdueDays == null ? other$overdueDays != null : !this$overdueDays.equals(other$overdueDays)) return false;
        final java.lang.Object this$orderNo = this.getOrderNo();
        final java.lang.Object other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) return false;
        final java.lang.Object this$customerName = this.getCustomerName();
        final java.lang.Object other$customerName = other.getCustomerName();
        if (this$customerName == null ? other$customerName != null : !this$customerName.equals(other$customerName)) return false;
        final java.lang.Object this$customerType = this.getCustomerType();
        final java.lang.Object other$customerType = other.getCustomerType();
        if (this$customerType == null ? other$customerType != null : !this$customerType.equals(other$customerType)) return false;
        final java.lang.Object this$amount = this.getAmount();
        final java.lang.Object other$amount = other.getAmount();
        if (this$amount == null ? other$amount != null : !this$amount.equals(other$amount)) return false;
        final java.lang.Object this$receivedAmount = this.getReceivedAmount();
        final java.lang.Object other$receivedAmount = other.getReceivedAmount();
        if (this$receivedAmount == null ? other$receivedAmount != null : !this$receivedAmount.equals(other$receivedAmount)) return false;
        final java.lang.Object this$remainingAmount = this.getRemainingAmount();
        final java.lang.Object other$remainingAmount = other.getRemainingAmount();
        if (this$remainingAmount == null ? other$remainingAmount != null : !this$remainingAmount.equals(other$remainingAmount)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$dueDate = this.getDueDate();
        final java.lang.Object other$dueDate = other.getDueDate();
        if (this$dueDate == null ? other$dueDate != null : !this$dueDate.equals(other$dueDate)) return false;
        final java.lang.Object this$lastReceivedDate = this.getLastReceivedDate();
        final java.lang.Object other$lastReceivedDate = other.getLastReceivedDate();
        if (this$lastReceivedDate == null ? other$lastReceivedDate != null : !this$lastReceivedDate.equals(other$lastReceivedDate)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Receivable;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $customerId = this.getCustomerId();
        result = result * PRIME + ($customerId == null ? 43 : $customerId.hashCode());
        final java.lang.Object $invoiceId = this.getInvoiceId();
        result = result * PRIME + ($invoiceId == null ? 43 : $invoiceId.hashCode());
        final java.lang.Object $overdueDays = this.getOverdueDays();
        result = result * PRIME + ($overdueDays == null ? 43 : $overdueDays.hashCode());
        final java.lang.Object $orderNo = this.getOrderNo();
        result = result * PRIME + ($orderNo == null ? 43 : $orderNo.hashCode());
        final java.lang.Object $customerName = this.getCustomerName();
        result = result * PRIME + ($customerName == null ? 43 : $customerName.hashCode());
        final java.lang.Object $customerType = this.getCustomerType();
        result = result * PRIME + ($customerType == null ? 43 : $customerType.hashCode());
        final java.lang.Object $amount = this.getAmount();
        result = result * PRIME + ($amount == null ? 43 : $amount.hashCode());
        final java.lang.Object $receivedAmount = this.getReceivedAmount();
        result = result * PRIME + ($receivedAmount == null ? 43 : $receivedAmount.hashCode());
        final java.lang.Object $remainingAmount = this.getRemainingAmount();
        result = result * PRIME + ($remainingAmount == null ? 43 : $remainingAmount.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $dueDate = this.getDueDate();
        result = result * PRIME + ($dueDate == null ? 43 : $dueDate.hashCode());
        final java.lang.Object $lastReceivedDate = this.getLastReceivedDate();
        result = result * PRIME + ($lastReceivedDate == null ? 43 : $lastReceivedDate.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Receivable(id=" + this.getId() + ", orderId=" + this.getOrderId() + ", orderNo=" + this.getOrderNo() + ", customerId=" + this.getCustomerId() + ", customerName=" + this.getCustomerName() + ", customerType=" + this.getCustomerType() + ", amount=" + this.getAmount() + ", receivedAmount=" + this.getReceivedAmount() + ", remainingAmount=" + this.getRemainingAmount() + ", invoiceId=" + this.getInvoiceId() + ", invoiceNo=" + this.getInvoiceNo() + ", status=" + this.getStatus() + ", dueDate=" + this.getDueDate() + ", overdueDays=" + this.getOverdueDays() + ", lastReceivedDate=" + this.getLastReceivedDate() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", remark=" + this.getRemark() + ")";
    }
}
