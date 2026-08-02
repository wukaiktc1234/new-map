package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量验签验真结果DTO
 */
@Schema(description = "批量验签验真结果")
public class BatchVerifyResultDTO {
    @Schema(description = "总数量")
    private Integer totalCount;
    @Schema(description = "成功数量")
    private Integer successCount;
    @Schema(description = "失败数量")
    private Integer failCount;

    public static BatchVerifyResultDTO of(int total, int success, int fail) {
        return new BatchVerifyResultDTO(total, success, fail);
    }

    public Integer getTotalCount() {
        return this.totalCount;
    }

    public Integer getSuccessCount() {
        return this.successCount;
    }

    public Integer getFailCount() {
        return this.failCount;
    }

    public void setTotalCount(final Integer totalCount) {
        this.totalCount = totalCount;
    }

    public void setSuccessCount(final Integer successCount) {
        this.successCount = successCount;
    }

    public void setFailCount(final Integer failCount) {
        this.failCount = failCount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchVerifyResultDTO)) return false;
        final BatchVerifyResultDTO other = (BatchVerifyResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$totalCount = this.getTotalCount();
        final java.lang.Object other$totalCount = other.getTotalCount();
        if (this$totalCount == null ? other$totalCount != null : !this$totalCount.equals(other$totalCount)) return false;
        final java.lang.Object this$successCount = this.getSuccessCount();
        final java.lang.Object other$successCount = other.getSuccessCount();
        if (this$successCount == null ? other$successCount != null : !this$successCount.equals(other$successCount)) return false;
        final java.lang.Object this$failCount = this.getFailCount();
        final java.lang.Object other$failCount = other.getFailCount();
        if (this$failCount == null ? other$failCount != null : !this$failCount.equals(other$failCount)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchVerifyResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $totalCount = this.getTotalCount();
        result = result * PRIME + ($totalCount == null ? 43 : $totalCount.hashCode());
        final java.lang.Object $successCount = this.getSuccessCount();
        result = result * PRIME + ($successCount == null ? 43 : $successCount.hashCode());
        final java.lang.Object $failCount = this.getFailCount();
        result = result * PRIME + ($failCount == null ? 43 : $failCount.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchVerifyResultDTO(totalCount=" + this.getTotalCount() + ", successCount=" + this.getSuccessCount() + ", failCount=" + this.getFailCount() + ")";
    }

    public BatchVerifyResultDTO() {
    }

    public BatchVerifyResultDTO(final Integer totalCount, final Integer successCount, final Integer failCount) {
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
    }
}
