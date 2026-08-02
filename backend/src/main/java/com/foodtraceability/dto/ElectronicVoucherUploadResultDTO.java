package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 电子凭证上传结果DTO
 */
@Schema(description = "电子凭证上传结果")
public class ElectronicVoucherUploadResultDTO {
    @Schema(description = "凭证ID")
    private Long voucherId;
    @Schema(description = "凭证类型")
    private String voucherType;
    @Schema(description = "凭证类型名称")
    private String voucherTypeName;
    @Schema(description = "凭证编号")
    private String voucherNo;
    @Schema(description = "上传状态")
    private String status;
    @Schema(description = "状态描述")
    private String statusDescription;
    @Schema(description = "文件哈希值")
    private String fileHash;
    @Schema(description = "是否重复")
    private Boolean duplicate;
    @Schema(description = "错误信息")
    private String errorMessage;

    public static ElectronicVoucherUploadResultDTO success(Long voucherId, String voucherType, String voucherNo, String fileHash) {
        ElectronicVoucherUploadResultDTO result = new ElectronicVoucherUploadResultDTO();
        result.setVoucherId(voucherId);
        result.setVoucherType(voucherType);
        result.setVoucherNo(voucherNo);
        result.setFileHash(fileHash);
        result.setStatus("SUCCESS");
        result.setStatusDescription("上传成功");
        result.setDuplicate(false);
        return result;
    }

    public static ElectronicVoucherUploadResultDTO duplicate(Long voucherId, String voucherType, String voucherNo, String fileHash) {
        ElectronicVoucherUploadResultDTO result = new ElectronicVoucherUploadResultDTO();
        result.setVoucherId(voucherId);
        result.setVoucherType(voucherType);
        result.setVoucherNo(voucherNo);
        result.setFileHash(fileHash);
        result.setStatus("DUPLICATE");
        result.setStatusDescription("文件已存在");
        result.setDuplicate(true);
        return result;
    }

    public static ElectronicVoucherUploadResultDTO failed(String errorMessage) {
        ElectronicVoucherUploadResultDTO result = new ElectronicVoucherUploadResultDTO();
        result.setStatus("FAILED");
        result.setStatusDescription("上传失败");
        result.setDuplicate(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public ElectronicVoucherUploadResultDTO() {
    }

    public Long getVoucherId() {
        return this.voucherId;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public String getVoucherTypeName() {
        return this.voucherTypeName;
    }

    public String getVoucherNo() {
        return this.voucherNo;
    }

    public String getStatus() {
        return this.status;
    }

    public String getStatusDescription() {
        return this.statusDescription;
    }

    public String getFileHash() {
        return this.fileHash;
    }

    public Boolean getDuplicate() {
        return this.duplicate;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setVoucherTypeName(final String voucherTypeName) {
        this.voucherTypeName = voucherTypeName;
    }

    public void setVoucherNo(final String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setStatusDescription(final String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public void setFileHash(final String fileHash) {
        this.fileHash = fileHash;
    }

    public void setDuplicate(final Boolean duplicate) {
        this.duplicate = duplicate;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicVoucherUploadResultDTO)) return false;
        final ElectronicVoucherUploadResultDTO other = (ElectronicVoucherUploadResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$duplicate = this.getDuplicate();
        final java.lang.Object other$duplicate = other.getDuplicate();
        if (this$duplicate == null ? other$duplicate != null : !this$duplicate.equals(other$duplicate)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$voucherTypeName = this.getVoucherTypeName();
        final java.lang.Object other$voucherTypeName = other.getVoucherTypeName();
        if (this$voucherTypeName == null ? other$voucherTypeName != null : !this$voucherTypeName.equals(other$voucherTypeName)) return false;
        final java.lang.Object this$voucherNo = this.getVoucherNo();
        final java.lang.Object other$voucherNo = other.getVoucherNo();
        if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$statusDescription = this.getStatusDescription();
        final java.lang.Object other$statusDescription = other.getStatusDescription();
        if (this$statusDescription == null ? other$statusDescription != null : !this$statusDescription.equals(other$statusDescription)) return false;
        final java.lang.Object this$fileHash = this.getFileHash();
        final java.lang.Object other$fileHash = other.getFileHash();
        if (this$fileHash == null ? other$fileHash != null : !this$fileHash.equals(other$fileHash)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicVoucherUploadResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $duplicate = this.getDuplicate();
        result = result * PRIME + ($duplicate == null ? 43 : $duplicate.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $voucherTypeName = this.getVoucherTypeName();
        result = result * PRIME + ($voucherTypeName == null ? 43 : $voucherTypeName.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $statusDescription = this.getStatusDescription();
        result = result * PRIME + ($statusDescription == null ? 43 : $statusDescription.hashCode());
        final java.lang.Object $fileHash = this.getFileHash();
        result = result * PRIME + ($fileHash == null ? 43 : $fileHash.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicVoucherUploadResultDTO(voucherId=" + this.getVoucherId() + ", voucherType=" + this.getVoucherType() + ", voucherTypeName=" + this.getVoucherTypeName() + ", voucherNo=" + this.getVoucherNo() + ", status=" + this.getStatus() + ", statusDescription=" + this.getStatusDescription() + ", fileHash=" + this.getFileHash() + ", duplicate=" + this.getDuplicate() + ", errorMessage=" + this.getErrorMessage() + ")";
    }
}
