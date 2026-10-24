package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** S4 列表查询条件 */
public class StatementQueryDTO {
    private Long accountId;
    private String status;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer current = 1;
    private Integer size = 20;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate v) { this.periodStart = v; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate v) { this.periodEnd = v; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer v) { this.current = v; }
    public Integer getSize() { return size; }
    public void setSize(Integer v) { this.size = v; }
}
