package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 凭证明细VO
 */
public class FinanceVoucherDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID */
    private Long detailId;

    /** 摘要 */
    private String summary;

    /** 会计科目ID */
    private Long subjectId;

    /** 会计科目编码 */
    private String subjectCode;

    /** 会计科目名称 */
    private String subjectName;

    /** 借方金额（单位：分） */
    private Long debitAmount;

    /** 贷方金额（单位：分） */
    private Long creditAmount;

    /** 借方金额（元，用于显示） */
    private String debitAmountDisplay;

    /** 贷方金额（元，用于显示） */
    private String creditAmountDisplay;

    /** 辅助核算项JSON */
    private String auxiliaryItem;

    /** 排序号 */
    private Integer sortOrder;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Long getDebitAmount() { return debitAmount; }
    public void setDebitAmount(Long debitAmount) { this.debitAmount = debitAmount; }
    public Long getCreditAmount() { return creditAmount; }
    public void setCreditAmount(Long creditAmount) { this.creditAmount = creditAmount; }
    public String getDebitAmountDisplay() { return debitAmountDisplay; }
    public void setDebitAmountDisplay(String debitAmountDisplay) { this.debitAmountDisplay = debitAmountDisplay; }
    public String getCreditAmountDisplay() { return creditAmountDisplay; }
    public void setCreditAmountDisplay(String creditAmountDisplay) { this.creditAmountDisplay = creditAmountDisplay; }
    public String getAuxiliaryItem() { return auxiliaryItem; }
    public void setAuxiliaryItem(String auxiliaryItem) { this.auxiliaryItem = auxiliaryItem; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
