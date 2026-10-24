package com.foodtraceability.dto.finance.recon;

import java.time.LocalDateTime;

/** 行级回执视图（§1.5 I3） */
public class ImportRowVO {
    private Long rowId;
    private Integer rowNo;
    private String status;
    private String errorCode;
    private String errorMessage;
    private Integer duplicateOfRowNo;
    private Long matchedTransactionId;
    private Long transactionId;
    private String rawPreview;
    private Long amountFen;
    private LocalDateTime txnTime;

    public Long getRowId() { return rowId; }
    public void setRowId(Long v) { this.rowId = v; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer v) { this.rowNo = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String v) { this.errorCode = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Integer getDuplicateOfRowNo() { return duplicateOfRowNo; }
    public void setDuplicateOfRowNo(Integer v) { this.duplicateOfRowNo = v; }
    public Long getMatchedTransactionId() { return matchedTransactionId; }
    public void setMatchedTransactionId(Long v) { this.matchedTransactionId = v; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }
    public String getRawPreview() { return rawPreview; }
    public void setRawPreview(String v) { this.rawPreview = v; }
    public Long getAmountFen() { return amountFen; }
    public void setAmountFen(Long v) { this.amountFen = v; }
    public LocalDateTime getTxnTime() { return txnTime; }
    public void setTxnTime(LocalDateTime v) { this.txnTime = v; }
}
