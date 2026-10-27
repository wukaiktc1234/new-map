package com.foodtraceability.dto.finance.recon;

/** 提交结果（§1.5 I4 响应） */
public class ImportCommitResultVO {
    /** 重复提交命中：true = 本次未真正落库，返回的是上次 commit 的结果（bug-⑤③） */
    private Boolean reused;

    private Long batchId;
    private String status;
    private Integer totalRows;
    private Integer okRows;
    private Integer insertedRows;
    private Integer dupRows;
    private Integer reviewRows;
    private Integer errorRows;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer v) { this.totalRows = v; }
    public Integer getOkRows() { return okRows; }
    public void setOkRows(Integer v) { this.okRows = v; }
    public Integer getInsertedRows() { return insertedRows; }
    public void setInsertedRows(Integer v) { this.insertedRows = v; }
    public Integer getDupRows() { return dupRows; }
    public void setDupRows(Integer v) { this.dupRows = v; }
    public Integer getReviewRows() { return reviewRows; }
    public void setReviewRows(Integer v) { this.reviewRows = v; }
    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer v) { this.errorRows = v; }

    public Boolean getReused() { return reused; }
    public void setReused(Boolean reused) { this.reused = reused; }
}
