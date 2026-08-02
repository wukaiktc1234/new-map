package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 科目余额明细项
 * 用于试算平衡表中按科目展示借方/贷方余额
 */
@Schema(description = "科目余额明细项")
public class SubjectBalanceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "科目编码")
    private String subjectCode;

    @Schema(description = "科目名称")
    private String subjectName;

    @Schema(description = "借方金额（单位：分）")
    private Long debitAmount;

    @Schema(description = "贷方金额（单位：分）")
    private Long creditAmount;

    public SubjectBalanceItem() {
    }

    public SubjectBalanceItem(String subjectCode, String subjectName, Long debitAmount, Long creditAmount) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Long getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(Long debitAmount) {
        this.debitAmount = debitAmount;
    }

    public Long getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(Long creditAmount) {
        this.creditAmount = creditAmount;
    }
}
