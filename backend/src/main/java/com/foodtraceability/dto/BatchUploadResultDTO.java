package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 批量上传结果DTO
 */
@Schema(description = "批量上传结果")
public class BatchUploadResultDTO {
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "总数量")
    private Integer totalCount;
    @Schema(description = "成功数量")
    private Integer successCount;
    @Schema(description = "失败数量")
    private Integer failCount;
    @Schema(description = "重复数量")
    private Integer duplicateCount;
    @Schema(description = "详细结果")
    private List<ElectronicVoucherUploadResultDTO> results;

    public BatchUploadResultDTO() {
    }

    public String getBatchNo() {
        return this.batchNo;
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

    public Integer getDuplicateCount() {
        return this.duplicateCount;
    }

    public List<ElectronicVoucherUploadResultDTO> getResults() {
        return this.results;
    }

    public void setBatchNo(final String batchNo) {
        this.batchNo = batchNo;
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

    public void setDuplicateCount(final Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public void setResults(final List<ElectronicVoucherUploadResultDTO> results) {
        this.results = results;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchUploadResultDTO)) return false;
        final BatchUploadResultDTO other = (BatchUploadResultDTO) o;
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
        final java.lang.Object this$duplicateCount = this.getDuplicateCount();
        final java.lang.Object other$duplicateCount = other.getDuplicateCount();
        if (this$duplicateCount == null ? other$duplicateCount != null : !this$duplicateCount.equals(other$duplicateCount)) return false;
        final java.lang.Object this$batchNo = this.getBatchNo();
        final java.lang.Object other$batchNo = other.getBatchNo();
        if (this$batchNo == null ? other$batchNo != null : !this$batchNo.equals(other$batchNo)) return false;
        final java.lang.Object this$results = this.getResults();
        final java.lang.Object other$results = other.getResults();
        if (this$results == null ? other$results != null : !this$results.equals(other$results)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchUploadResultDTO;
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
        final java.lang.Object $duplicateCount = this.getDuplicateCount();
        result = result * PRIME + ($duplicateCount == null ? 43 : $duplicateCount.hashCode());
        final java.lang.Object $batchNo = this.getBatchNo();
        result = result * PRIME + ($batchNo == null ? 43 : $batchNo.hashCode());
        final java.lang.Object $results = this.getResults();
        result = result * PRIME + ($results == null ? 43 : $results.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchUploadResultDTO(batchNo=" + this.getBatchNo() + ", totalCount=" + this.getTotalCount() + ", successCount=" + this.getSuccessCount() + ", failCount=" + this.getFailCount() + ", duplicateCount=" + this.getDuplicateCount() + ", results=" + this.getResults() + ")";
    }
}
