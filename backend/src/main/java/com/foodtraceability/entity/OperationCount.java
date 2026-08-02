package com.foodtraceability.entity;

/**
 * 操作类型统计实体类
 */
public class OperationCount {
    private String operationType;
    private Long count;

    public OperationCount() {
    }

    public String getOperationType() {
        return this.operationType;
    }

    public Long getCount() {
        return this.count;
    }

    public void setOperationType(final String operationType) {
        this.operationType = operationType;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OperationCount)) return false;
        final OperationCount other = (OperationCount) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OperationCount;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OperationCount(operationType=" + this.getOperationType() + ", count=" + this.getCount() + ")";
    }
}
