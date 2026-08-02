package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 电子凭证归档请求DTO
 */
@Schema(description = "电子凭证归档请求")
public class VoucherArchiveRequestDTO {
    @NotEmpty(message = "电子凭证ID列表不能为空")
    @Schema(description = "电子凭证ID列表")
    private List<Long> voucherIds;
    @NotNull(message = "归档信息不能为空")
    @Schema(description = "归档信息")
    private VoucherArchiveDTO archiveInfo;

    public VoucherArchiveRequestDTO() {
    }

    public List<Long> getVoucherIds() {
        return this.voucherIds;
    }

    public VoucherArchiveDTO getArchiveInfo() {
        return this.archiveInfo;
    }

    public void setVoucherIds(final List<Long> voucherIds) {
        this.voucherIds = voucherIds;
    }

    public void setArchiveInfo(final VoucherArchiveDTO archiveInfo) {
        this.archiveInfo = archiveInfo;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherArchiveRequestDTO)) return false;
        final VoucherArchiveRequestDTO other = (VoucherArchiveRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$voucherIds = this.getVoucherIds();
        final java.lang.Object other$voucherIds = other.getVoucherIds();
        if (this$voucherIds == null ? other$voucherIds != null : !this$voucherIds.equals(other$voucherIds)) return false;
        final java.lang.Object this$archiveInfo = this.getArchiveInfo();
        final java.lang.Object other$archiveInfo = other.getArchiveInfo();
        if (this$archiveInfo == null ? other$archiveInfo != null : !this$archiveInfo.equals(other$archiveInfo)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherArchiveRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $voucherIds = this.getVoucherIds();
        result = result * PRIME + ($voucherIds == null ? 43 : $voucherIds.hashCode());
        final java.lang.Object $archiveInfo = this.getArchiveInfo();
        result = result * PRIME + ($archiveInfo == null ? 43 : $archiveInfo.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherArchiveRequestDTO(voucherIds=" + this.getVoucherIds() + ", archiveInfo=" + this.getArchiveInfo() + ")";
    }
}
