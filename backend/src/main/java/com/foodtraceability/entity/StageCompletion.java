package com.foodtraceability.entity;

/**
 * 阶段完成统计实体类
 */
public class StageCompletion {
    private Integer stage;
    private Long count;

    public StageCompletion() {
    }

    public Integer getStage() {
        return this.stage;
    }

    public Long getCount() {
        return this.count;
    }

    public void setStage(final Integer stage) {
        this.stage = stage;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof StageCompletion)) return false;
        final StageCompletion other = (StageCompletion) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stage = this.getStage();
        final java.lang.Object other$stage = other.getStage();
        if (this$stage == null ? other$stage != null : !this$stage.equals(other$stage)) return false;
        final java.lang.Object this$count = this.getCount();
        final java.lang.Object other$count = other.getCount();
        if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof StageCompletion;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stage = this.getStage();
        result = result * PRIME + ($stage == null ? 43 : $stage.hashCode());
        final java.lang.Object $count = this.getCount();
        result = result * PRIME + ($count == null ? 43 : $count.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "StageCompletion(stage=" + this.getStage() + ", count=" + this.getCount() + ")";
    }
}
