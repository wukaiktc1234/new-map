package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** 现金销售 SALE 补记入参（P2-B） */
public class CashSalesBackfillRequest {
    /** 班次 ID（与 from/to 二选一；给了 shiftId 就按班次时间窗） */
    private Long shiftId;
    /** 日期区间起（shiftId 为空时必填） */
    private LocalDate from;
    /** 日期区间止 */
    private LocalDate to;
    /** 默认 true：只返回将补记的清单，不写库 */
    private Boolean dryRun = Boolean.TRUE;
    /** 补记原因（写入 remark） */
    private String reason;

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long v) { this.shiftId = v; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate v) { this.from = v; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate v) { this.to = v; }
    public Boolean getDryRun() { return dryRun; }
    public void setDryRun(Boolean v) { this.dryRun = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
}
