package com.foodtraceability.entity;

/**
 * 操作者统计实体类
 */
public class OperatorCount {
    private String operator;
    private Long count;

    public OperatorCount() {
    }

    public String getOperator() {
        return this.operator;
    }

    public Long getCount() {
        return this.count;
    }

    public void setOperator(final String operator) {
        this.operator = operator;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OperatorCount)) return false;
        final OperatorCount other = (OperatorCount) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OperatorCount;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OperatorCount(operator=" + this.getOperator() + ", count=" + this.getCount() + ")";
    }
}
