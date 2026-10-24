package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 导入批次（设计 §5.1，表 import_batches）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>：元→分换算只允许复用 Java 侧的唯一实现
 * （见后续 FinanceMoney 工具），禁止 double/float。</p>
 */
@TableName(value = "import_batches")
public class ImportBatch {

    @TableId(value = "batch_id", type = IdType.AUTO)
    private Long batchId;

    @TableField(value = "batch_no")
    private String batchNo;

    @TableField(value = "source_type")
    private String sourceType;

    @TableField(value = "account_id")
    private Long accountId;

    @TableField(value = "file_name")
    private String fileName;

    @TableField(value = "file_sha256")
    private String fileSha256;

    @TableField(value = "encoding_used")
    private String encodingUsed;

    @TableField(value = "period_start")
    private java.time.LocalDate periodStart;

    @TableField(value = "period_end")
    private java.time.LocalDate periodEnd;

    @TableField(value = "total_rows")
    private Integer totalRows;

    @TableField(value = "ok_rows")
    private Integer okRows;

    @TableField(value = "dup_rows")
    private Integer dupRows;

    @TableField(value = "error_rows")
    private Integer errorRows;

    @TableField(value = "status")
    private String status;

    @TableField(value = "error_summary")
    private String errorSummary;

    @TableField(value = "create_user_id")
    private Long createUserId;

    @TableField(value = "created_by")
    private String createdBy;

    @TableField(value = "create_time")
    private java.time.LocalDateTime createTime;

    @TableField(value = "update_time")
    private java.time.LocalDateTime updateTime;

    @TableField(value = "deleted")
    private Integer deleted;

    @TableField(value = "version")
    private Integer version;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileSha256() { return fileSha256; }
    public void setFileSha256(String fileSha256) { this.fileSha256 = fileSha256; }

    public String getEncodingUsed() { return encodingUsed; }
    public void setEncodingUsed(String encodingUsed) { this.encodingUsed = encodingUsed; }

    public java.time.LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(java.time.LocalDate periodStart) { this.periodStart = periodStart; }

    public java.time.LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(java.time.LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public Integer getOkRows() { return okRows; }
    public void setOkRows(Integer okRows) { this.okRows = okRows; }

    public Integer getDupRows() { return dupRows; }
    public void setDupRows(Integer dupRows) { this.dupRows = dupRows; }

    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer errorRows) { this.errorRows = errorRows; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }

    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }

    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}