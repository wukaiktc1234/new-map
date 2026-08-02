package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量验签验真请求DTO
 */
@Schema(description = "批量验签验真请求")
public class BatchVerifyRequestDTO {
    @NotEmpty(message = "凭证ID列表不能为空")
    @Size(max = 100, message = "批量操作数量不能超过100")
    @Schema(description = "凭证ID列表", required = true)
    private List<Long> ids;

    public BatchVerifyRequestDTO() {
    }

    public List<Long> getIds() {
        return this.ids;
    }

    public void setIds(final List<Long> ids) {
        this.ids = ids;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BatchVerifyRequestDTO)) return false;
        final BatchVerifyRequestDTO other = (BatchVerifyRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$ids = this.getIds();
        final java.lang.Object other$ids = other.getIds();
        if (this$ids == null ? other$ids != null : !this$ids.equals(other$ids)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BatchVerifyRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $ids = this.getIds();
        result = result * PRIME + ($ids == null ? 43 : $ids.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BatchVerifyRequestDTO(ids=" + this.getIds() + ")";
    }
}
