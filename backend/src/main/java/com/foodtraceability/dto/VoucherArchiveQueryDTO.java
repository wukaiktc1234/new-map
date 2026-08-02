package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 电子凭证归档查询DTO
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 */
@Schema(description = "电子凭证归档查询条件")
public class VoucherArchiveQueryDTO extends PageQuery {
    @Schema(description = "归档编号")
    private String archiveNo;
    @Schema(description = "归档名称")
    private String archiveName;
    @Schema(description = "会计年度")
    private String fiscalYear;
    @Schema(description = "归档类型：1-月度归档，2-季度归档，3-年度归档")
    private Integer archiveType;
    @Schema(description = "归档状态：0-待归档，1-已归档，2-已封存")
    private Integer status;
    @Schema(description = "归档日期起")
    private String archiveDateFrom;
    @Schema(description = "归档日期止")
    private String archiveDateTo;
    @Schema(description = "归档人ID")
    private Long archivistId;

    public VoucherArchiveQueryDTO() {
    }

    public String getArchiveNo() {
        return this.archiveNo;
    }

    public String getArchiveName() {
        return this.archiveName;
    }

    public String getFiscalYear() {
        return this.fiscalYear;
    }

    public Integer getArchiveType() {
        return this.archiveType;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getArchiveDateFrom() {
        return this.archiveDateFrom;
    }

    public String getArchiveDateTo() {
        return this.archiveDateTo;
    }

    public Long getArchivistId() {
        return this.archivistId;
    }

    public void setArchiveNo(final String archiveNo) {
        this.archiveNo = archiveNo;
    }

    public void setArchiveName(final String archiveName) {
        this.archiveName = archiveName;
    }

    public void setFiscalYear(final String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public void setArchiveType(final Integer archiveType) {
        this.archiveType = archiveType;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setArchiveDateFrom(final String archiveDateFrom) {
        this.archiveDateFrom = archiveDateFrom;
    }

    public void setArchiveDateTo(final String archiveDateTo) {
        this.archiveDateTo = archiveDateTo;
    }

    public void setArchivistId(final Long archivistId) {
        this.archivistId = archivistId;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherArchiveQueryDTO(archiveNo=" + this.getArchiveNo() + ", archiveName=" + this.getArchiveName() + ", fiscalYear=" + this.getFiscalYear() + ", archiveType=" + this.getArchiveType() + ", status=" + this.getStatus() + ", archiveDateFrom=" + this.getArchiveDateFrom() + ", archiveDateTo=" + this.getArchiveDateTo() + ", archivistId=" + this.getArchivistId() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherArchiveQueryDTO)) return false;
        final VoucherArchiveQueryDTO other = (VoucherArchiveQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!super.equals(o)) return false;
        final java.lang.Object this$archiveType = this.getArchiveType();
        final java.lang.Object other$archiveType = other.getArchiveType();
        if (this$archiveType == null ? other$archiveType != null : !this$archiveType.equals(other$archiveType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$archivistId = this.getArchivistId();
        final java.lang.Object other$archivistId = other.getArchivistId();
        if (this$archivistId == null ? other$archivistId != null : !this$archivistId.equals(other$archivistId)) return false;
        final java.lang.Object this$archiveNo = this.getArchiveNo();
        final java.lang.Object other$archiveNo = other.getArchiveNo();
        if (this$archiveNo == null ? other$archiveNo != null : !this$archiveNo.equals(other$archiveNo)) return false;
        final java.lang.Object this$archiveName = this.getArchiveName();
        final java.lang.Object other$archiveName = other.getArchiveName();
        if (this$archiveName == null ? other$archiveName != null : !this$archiveName.equals(other$archiveName)) return false;
        final java.lang.Object this$fiscalYear = this.getFiscalYear();
        final java.lang.Object other$fiscalYear = other.getFiscalYear();
        if (this$fiscalYear == null ? other$fiscalYear != null : !this$fiscalYear.equals(other$fiscalYear)) return false;
        final java.lang.Object this$archiveDateFrom = this.getArchiveDateFrom();
        final java.lang.Object other$archiveDateFrom = other.getArchiveDateFrom();
        if (this$archiveDateFrom == null ? other$archiveDateFrom != null : !this$archiveDateFrom.equals(other$archiveDateFrom)) return false;
        final java.lang.Object this$archiveDateTo = this.getArchiveDateTo();
        final java.lang.Object other$archiveDateTo = other.getArchiveDateTo();
        if (this$archiveDateTo == null ? other$archiveDateTo != null : !this$archiveDateTo.equals(other$archiveDateTo)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherArchiveQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final java.lang.Object $archiveType = this.getArchiveType();
        result = result * PRIME + ($archiveType == null ? 43 : $archiveType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $archivistId = this.getArchivistId();
        result = result * PRIME + ($archivistId == null ? 43 : $archivistId.hashCode());
        final java.lang.Object $archiveNo = this.getArchiveNo();
        result = result * PRIME + ($archiveNo == null ? 43 : $archiveNo.hashCode());
        final java.lang.Object $archiveName = this.getArchiveName();
        result = result * PRIME + ($archiveName == null ? 43 : $archiveName.hashCode());
        final java.lang.Object $fiscalYear = this.getFiscalYear();
        result = result * PRIME + ($fiscalYear == null ? 43 : $fiscalYear.hashCode());
        final java.lang.Object $archiveDateFrom = this.getArchiveDateFrom();
        result = result * PRIME + ($archiveDateFrom == null ? 43 : $archiveDateFrom.hashCode());
        final java.lang.Object $archiveDateTo = this.getArchiveDateTo();
        result = result * PRIME + ($archiveDateTo == null ? 43 : $archiveDateTo.hashCode());
        return result;
    }
}
