package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** 外部流水只读查询条件（§1.5 I6） */
public class BankTransactionQueryDTO {
    private Long accountId;
    private String sourceType;
    private String matchStatus;
    private Boolean needsReview;
    private LocalDate from;
    private LocalDate to;
    private Long batchId;
    private Integer current = 1;
    private Integer size = 20;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String v) { this.matchStatus = v; }
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean v) { this.needsReview = v; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate v) { this.from = v; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate v) { this.to = v; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer v) { this.current = v; }
    public Integer getSize() { return size; }
    public void setSize(Integer v) { this.size = v; }
}
