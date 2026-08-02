package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 手动发票统计VO
 */
@Schema(description = "手动发票统计信息")
public class ManualInvoiceStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "发票总数")
    private Long total;

    @Schema(description = "已验真数")
    private Long verified;

    @Schema(description = "待验真数")
    private Long pending;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getVerified() { return verified; }
    public void setVerified(Long verified) { this.verified = verified; }
    public Long getPending() { return pending; }
    public void setPending(Long pending) { this.pending = pending; }
}
