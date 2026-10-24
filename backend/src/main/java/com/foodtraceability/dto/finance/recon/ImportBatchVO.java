package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 导入批次视图（§1.5 I1/I2 响应） */
public class ImportBatchVO {
    private Long batchId;
    private String batchNo;
    private String sourceType;
    private Long accountId;
    private String fileName;
    private String fileSha256;
    private String encodingUsed;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer totalRows;
    private Integer okRows;
    private Integer dupRows;
    private Integer errorRows;
    private String status;
    private String errorSummary;
    private Long createUserId;
    private LocalDateTime createTime;
    private Boolean reused;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String v) { this.batchNo = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String v) { this.fileSha256 = v; }
    public String getEncodingUsed() { return encodingUsed; }
    public void setEncodingUsed(String v) { this.encodingUsed = v; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate v) { this.periodStart = v; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate v) { this.periodEnd = v; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer v) { this.totalRows = v; }
    public Integer getOkRows() { return okRows; }
    public void setOkRows(Integer v) { this.okRows = v; }
    public Integer getDupRows() { return dupRows; }
    public void setDupRows(Integer v) { this.dupRows = v; }
    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer v) { this.errorRows = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String v) { this.errorSummary = v; }
    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long v) { this.createUserId = v; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime v) { this.createTime = v; }
    public Boolean getReused() { return reused; }
    public void setReused(Boolean v) { this.reused = v; }
}
