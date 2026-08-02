package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量电子凭证入账请求DTO
 */
@Schema(description = "批量电子凭证入账请求")
public class BatchAccountingRequestDTO {
    @NotEmpty(message = "电子凭证ID列表不能为空")
    @Schema(description = "电子凭证ID列表")
    private List<Long> ids;
    @NotNull(message = "入账信息不能为空")
    @Schema(description = "入账信息")
    private VoucherAccountingRequestDTO accountingInfo;

    public BatchAccountingRequestDTO() {
    }

    public List<Long> getIds() {
        return this.ids;
    }

    public VoucherAccountingRequestDTO getAccountingInfo() {
        return this.accountingInfo;
    }

    public void setIds(final List<Long> ids) {
        this.ids = ids;
    }

    public void setAccountingInfo(final VoucherAccountingRequestDTO accountingInfo) {
        this.accountingInfo = accountingInfo;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchAccountingRequestDTO)) return false;
        final BatchAccountingRequestDTO other = (BatchAccountingRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$ids = this.getIds();
        final java.lang.Object other$ids = other.getIds();
        if (this$ids == null ? other$ids != null : !this$ids.equals(other$ids)) return false;
        final java.lang.Object this$accountingInfo = this.getAccountingInfo();
        final java.lang.Object other$accountingInfo = other.getAccountingInfo();
        if (this$accountingInfo == null ? other$accountingInfo != null : !this$accountingInfo.equals(other$accountingInfo)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchAccountingRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $ids = this.getIds();
        result = result * PRIME + ($ids == null ? 43 : $ids.hashCode());
        final java.lang.Object $accountingInfo = this.getAccountingInfo();
        result = result * PRIME + ($accountingInfo == null ? 43 : $accountingInfo.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchAccountingRequestDTO(ids=" + this.getIds() + ", accountingInfo=" + this.getAccountingInfo() + ")";
    }
}
