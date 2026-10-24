package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** R2 跑批列表查询条件（accountId / businessDate / status + 分页）。 */
public class ReconRunQueryDTO {

    private Long accountId;
    private LocalDate businessDate;
    /** RUNNING / FINALIZED / FAILED */
    private String status;
    private Long current = 1L;
    private Long size = 20L;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
