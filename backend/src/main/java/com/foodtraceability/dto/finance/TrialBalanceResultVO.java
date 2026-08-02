package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 试算平衡结果VO
 */
@Schema(description = "试算平衡结果")
public class TrialBalanceResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否平衡")
    private Boolean isBalanced;

    @Schema(description = "借方合计（单位：分）")
    private Long totalDebit;

    @Schema(description = "贷方合计（单位：分）")
    private Long totalCredit;

    @Schema(description = "差额（单位：分）")
    private Long difference;

    @Schema(description = "科目余额明细列表")
    private List<SubjectBalanceItem> details;

    public Boolean getIsBalanced() {
        return isBalanced;
    }

    public void setIsBalanced(Boolean isBalanced) {
        this.isBalanced = isBalanced;
    }

    public Long getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(Long totalDebit) {
        this.totalDebit = totalDebit;
    }

    public Long getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(Long totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Long getDifference() {
        return difference;
    }

    public void setDifference(Long difference) {
        this.difference = difference;
    }

    public List<SubjectBalanceItem> getDetails() {
        return details;
    }

    public void setDetails(List<SubjectBalanceItem> details) {
        this.details = details;
    }
}
