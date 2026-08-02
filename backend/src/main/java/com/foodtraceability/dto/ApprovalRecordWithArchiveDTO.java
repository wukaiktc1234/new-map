package com.foodtraceability.dto;

import com.foodtraceability.entity.ApprovalRecord;
import com.foodtraceability.entity.OnboardingArchive;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 审批记录与档案关联DTO
 * 用于返回审批记录及其关联的档案信息
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "审批记录与档案关联DTO")
public class ApprovalRecordWithArchiveDTO {
    @Schema(description = "审批记录")
    private ApprovalRecord approvalRecord;
    @Schema(description = "关联的档案信息")
    private OnboardingArchive archive;

    /**
     * 从审批记录和档案构建DTO
     *
     * @param record  审批记录
     * @param archive 档案信息
     * @return DTO对象
     */
    public static ApprovalRecordWithArchiveDTO of(ApprovalRecord record, OnboardingArchive archive) {
        ApprovalRecordWithArchiveDTO dto = new ApprovalRecordWithArchiveDTO();
        dto.setApprovalRecord(record);
        dto.setArchive(archive);
        return dto;
    }

    public ApprovalRecordWithArchiveDTO() {
    }

    public ApprovalRecord getApprovalRecord() {
        return this.approvalRecord;
    }

    public OnboardingArchive getArchive() {
        return this.archive;
    }

    public void setApprovalRecord(final ApprovalRecord approvalRecord) {
        this.approvalRecord = approvalRecord;
    }

    public void setArchive(final OnboardingArchive archive) {
        this.archive = archive;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ApprovalRecordWithArchiveDTO)) return false;
        final ApprovalRecordWithArchiveDTO other = (ApprovalRecordWithArchiveDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$approvalRecord = this.getApprovalRecord();
        final java.lang.Object other$approvalRecord = other.getApprovalRecord();
        if (this$approvalRecord == null ? other$approvalRecord != null : !this$approvalRecord.equals(other$approvalRecord)) return false;
        final java.lang.Object this$archive = this.getArchive();
        final java.lang.Object other$archive = other.getArchive();
        if (this$archive == null ? other$archive != null : !this$archive.equals(other$archive)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ApprovalRecordWithArchiveDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $approvalRecord = this.getApprovalRecord();
        result = result * PRIME + ($approvalRecord == null ? 43 : $approvalRecord.hashCode());
        final java.lang.Object $archive = this.getArchive();
        result = result * PRIME + ($archive == null ? 43 : $archive.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ApprovalRecordWithArchiveDTO(approvalRecord=" + this.getApprovalRecord() + ", archive=" + this.getArchive() + ")";
    }
}
