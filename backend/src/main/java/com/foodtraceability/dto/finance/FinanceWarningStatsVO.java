package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 财务预警统计VO
 */
@Schema(description = "财务预警统计信息")
public class FinanceWarningStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "预警总数")
    private Long total;

    @Schema(description = "待处理数（UNHANDLED）")
    private Long pending;

    @Schema(description = "处理中数（HANDLING）")
    private Long handling;

    @Schema(description = "已解决数（RESOLVED）")
    private Long resolved;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getPending() { return pending; }
    public void setPending(Long pending) { this.pending = pending; }
    public Long getHandling() { return handling; }
    public void setHandling(Long handling) { this.handling = handling; }
    public Long getResolved() { return resolved; }
    public void setResolved(Long resolved) { this.resolved = resolved; }
}
