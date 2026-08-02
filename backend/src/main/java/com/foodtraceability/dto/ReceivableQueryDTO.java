package com.foodtraceability.dto;

import java.util.Date;

/**
 * 应收账款查询DTO
 * 用于封装应收账款的查询条件
 */
public class ReceivableQueryDTO {
    /**
     * 客户名称
     */
    private String customerName;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 应收账款状态
     */
    private String status;
    /**
     * 到期日开始
     */
    private Date dueDateStart;
    /**
     * 到期日结束
     */
    private Date dueDateEnd;
    /**
     * 页码
     */
    private Integer pageNum = 1;
    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    public String getCustomerName() {
        return this.customerName;
    }

    public String getOrderNo() {
        return this.orderNo;
    }

    public String getStatus() {
        return this.status;
    }

    public Date getDueDateStart() {
        return this.dueDateStart;
    }

    public Date getDueDateEnd() {
        return this.dueDateEnd;
    }

    public Integer getPageNum() {
        return this.pageNum;
    }

    public Integer getPageSize() {
        return this.pageSize;
    }

    public ReceivableQueryDTO() {
    }

    /**
     * 客户名称
     */
    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    /**
     * 订单编号
     */
    public void setOrderNo(final String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 应收账款状态
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * 到期日开始
     */
    public void setDueDateStart(final Date dueDateStart) {
        this.dueDateStart = dueDateStart;
    }

    /**
     * 到期日结束
     */
    public void setDueDateEnd(final Date dueDateEnd) {
        this.dueDateEnd = dueDateEnd;
    }

    /**
     * 页码
     */
    public void setPageNum(final Integer pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 每页条数
     */
    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ReceivableQueryDTO)) return false;
        final ReceivableQueryDTO other = (ReceivableQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$pageNum = this.getPageNum();
        final java.lang.Object other$pageNum = other.getPageNum();
        if (this$pageNum == null ? other$pageNum != null : !this$pageNum.equals(other$pageNum)) return false;
        final java.lang.Object this$pageSize = this.getPageSize();
        final java.lang.Object other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !this$pageSize.equals(other$pageSize)) return false;
        final java.lang.Object this$customerName = this.getCustomerName();
        final java.lang.Object other$customerName = other.getCustomerName();
        if (this$customerName == null ? other$customerName != null : !this$customerName.equals(other$customerName)) return false;
        final java.lang.Object this$orderNo = this.getOrderNo();
        final java.lang.Object other$orderNo = other.getOrderNo();
        if (this$orderNo == null ? other$orderNo != null : !this$orderNo.equals(other$orderNo)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$dueDateStart = this.getDueDateStart();
        final java.lang.Object other$dueDateStart = other.getDueDateStart();
        if (this$dueDateStart == null ? other$dueDateStart != null : !this$dueDateStart.equals(other$dueDateStart)) return false;
        final java.lang.Object this$dueDateEnd = this.getDueDateEnd();
        final java.lang.Object other$dueDateEnd = other.getDueDateEnd();
        if (this$dueDateEnd == null ? other$dueDateEnd != null : !this$dueDateEnd.equals(other$dueDateEnd)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ReceivableQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $pageNum = this.getPageNum();
        result = result * PRIME + ($pageNum == null ? 43 : $pageNum.hashCode());
        final java.lang.Object $pageSize = this.getPageSize();
        result = result * PRIME + ($pageSize == null ? 43 : $pageSize.hashCode());
        final java.lang.Object $customerName = this.getCustomerName();
        result = result * PRIME + ($customerName == null ? 43 : $customerName.hashCode());
        final java.lang.Object $orderNo = this.getOrderNo();
        result = result * PRIME + ($orderNo == null ? 43 : $orderNo.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $dueDateStart = this.getDueDateStart();
        result = result * PRIME + ($dueDateStart == null ? 43 : $dueDateStart.hashCode());
        final java.lang.Object $dueDateEnd = this.getDueDateEnd();
        result = result * PRIME + ($dueDateEnd == null ? 43 : $dueDateEnd.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ReceivableQueryDTO(customerName=" + this.getCustomerName() + ", orderNo=" + this.getOrderNo() + ", status=" + this.getStatus() + ", dueDateStart=" + this.getDueDateStart() + ", dueDateEnd=" + this.getDueDateEnd() + ", pageNum=" + this.getPageNum() + ", pageSize=" + this.getPageSize() + ")";
    }
}
