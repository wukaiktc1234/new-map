package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.mybatis.JsonbStringTypeHandler;

import java.time.LocalDateTime;

/**
 * 对账 P0 · 导入行级明细（设计 §5.2，表 import_rows）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>：元→分换算只允许复用 Java 侧的唯一实现
 * （见后续 FinanceMoney 工具），禁止 double/float。</p>
 */
@TableName(value = "import_rows", autoResultMap = true)
public class ImportRow {

    @TableId(value = "row_id", type = IdType.AUTO)
    private Long rowId;

    @TableField(value = "batch_id")
    private Long batchId;

    @TableField(value = "row_no")
    private Integer rowNo;

    @TableField(value = "raw_json", typeHandler = JsonbStringTypeHandler.class)
    private String rawJson;

    @TableField(value = "status")
    private String status;

    @TableField(value = "error_code")
    private String errorCode;

    @TableField(value = "error_message")
    private String errorMessage;

    @TableField(value = "duplicate_of_row_no")
    private Integer duplicateOfRowNo;

    @TableField(value = "matched_transaction_id")
    private Long matchedTransactionId;

    @TableField(value = "transaction_id")
    private Long transactionId;

    @TableField(value = "create_time")
    private java.time.LocalDateTime createTime;

    @TableField(value = "update_time")
    private java.time.LocalDateTime updateTime;

    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDuplicateOfRowNo() { return duplicateOfRowNo; }
    public void setDuplicateOfRowNo(Integer duplicateOfRowNo) { this.duplicateOfRowNo = duplicateOfRowNo; }

    public Long getMatchedTransactionId() { return matchedTransactionId; }
    public void setMatchedTransactionId(Long matchedTransactionId) { this.matchedTransactionId = matchedTransactionId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }

    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
}