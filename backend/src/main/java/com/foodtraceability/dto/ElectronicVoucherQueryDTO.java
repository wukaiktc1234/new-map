package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 电子凭证查询参数DTO
 */
@Schema(description = "电子凭证查询参数")
public class ElectronicVoucherQueryDTO {
    @Schema(description = "凭证编号")
    @Size(max = 100, message = "凭证编号最长100个字符")
    private String voucherNo;
    @Schema(description = "凭证类型：invoice/ticket/bank_receipt等")
    private String voucherType;
    @Schema(description = "状态：0-待处理，1-已入账，2-已归档")
    private Integer status;
    @Schema(description = "验签状态：0-未验签，1-验签通过，2-验签失败")
    private Integer signatureStatus;
    @Schema(description = "验真状态：0-未验真，1-验真通过，2-验真失败")
    private Integer verifyStatus;
    @Schema(description = "开始日期")
    private LocalDate startDate;
    @Schema(description = "结束日期")
    private LocalDate endDate;
    @Schema(description = "关联业务类型")
    private String businessType;
    @Schema(description = "关联业务ID")
    private Long businessId;
    @Schema(description = "页码", example = "1")
    private Integer page = 1;
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    public ElectronicVoucherQueryDTO() {
    }

    public String getVoucherNo() {
        return this.voucherNo;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public Integer getStatus() {
        return this.status;
    }

    public Integer getSignatureStatus() {
        return this.signatureStatus;
    }

    public Integer getVerifyStatus() {
        return this.verifyStatus;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public String getBusinessType() {
        return this.businessType;
    }

    public Long getBusinessId() {
        return this.businessId;
    }

    public Integer getPage() {
        return this.page;
    }

    public Integer getSize() {
        return this.size;
    }

    public void setVoucherNo(final String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setSignatureStatus(final Integer signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    public void setVerifyStatus(final Integer verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public void setBusinessId(final Long businessId) {
        this.businessId = businessId;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    public void setSize(final Integer size) {
        this.size = size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicVoucherQueryDTO)) return false;
        final ElectronicVoucherQueryDTO other = (ElectronicVoucherQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$signatureStatus = this.getSignatureStatus();
        final java.lang.Object other$signatureStatus = other.getSignatureStatus();
        if (this$signatureStatus == null ? other$signatureStatus != null : !this$signatureStatus.equals(other$signatureStatus)) return false;
        final java.lang.Object this$verifyStatus = this.getVerifyStatus();
        final java.lang.Object other$verifyStatus = other.getVerifyStatus();
        if (this$verifyStatus == null ? other$verifyStatus != null : !this$verifyStatus.equals(other$verifyStatus)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$page = this.getPage();
        final java.lang.Object other$page = other.getPage();
        if (this$page == null ? other$page != null : !this$page.equals(other$page)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$voucherNo = this.getVoucherNo();
        final java.lang.Object other$voucherNo = other.getVoucherNo();
        if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicVoucherQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $signatureStatus = this.getSignatureStatus();
        result = result * PRIME + ($signatureStatus == null ? 43 : $signatureStatus.hashCode());
        final java.lang.Object $verifyStatus = this.getVerifyStatus();
        result = result * PRIME + ($verifyStatus == null ? 43 : $verifyStatus.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $page = this.getPage();
        result = result * PRIME + ($page == null ? 43 : $page.hashCode());
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicVoucherQueryDTO(voucherNo=" + this.getVoucherNo() + ", voucherType=" + this.getVoucherType() + ", status=" + this.getStatus() + ", signatureStatus=" + this.getSignatureStatus() + ", verifyStatus=" + this.getVerifyStatus() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", businessType=" + this.getBusinessType() + ", businessId=" + this.getBusinessId() + ", page=" + this.getPage() + ", size=" + this.getSize() + ")";
    }
}
