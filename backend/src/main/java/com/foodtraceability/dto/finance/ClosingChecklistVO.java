package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 结账检查清单VO
 */
@Schema(description = "结账检查清单")
public class ClosingChecklistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "期间ID")
    private Long periodId;

    @Schema(description = "期间名称")
    private String periodName;

    @Schema(description = "是否有未审核凭证")
    private Boolean hasUnauditedVouchers;

    @Schema(description = "未审核凭证数")
    private Integer unauditedCount;

    @Schema(description = "试算平衡是否通过")
    private Boolean trialBalancePassed;

    @Schema(description = "损益是否已结转")
    private Boolean profitTransferred;

    @Schema(description = "是否可以结账")
    private Boolean canClose;

    @Schema(description = "检查项列表")
    private List<ChecklistItem> items;

    /**
     * 检查项内部类
     */
    @Schema(description = "检查项")
    public static class ChecklistItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "检查项名称")
        private String name;

        @Schema(description = "是否通过")
        private Boolean passed;

        @Schema(description = "说明")
        private String message;

        public ChecklistItem() {
        }

        public ChecklistItem(String name, Boolean passed, String message) {
            this.name = name;
            this.passed = passed;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getPassed() {
            return passed;
        }

        public void setPassed(Boolean passed) {
            this.passed = passed;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public Boolean getHasUnauditedVouchers() {
        return hasUnauditedVouchers;
    }

    public void setHasUnauditedVouchers(Boolean hasUnauditedVouchers) {
        this.hasUnauditedVouchers = hasUnauditedVouchers;
    }

    public Integer getUnauditedCount() {
        return unauditedCount;
    }

    public void setUnauditedCount(Integer unauditedCount) {
        this.unauditedCount = unauditedCount;
    }

    public Boolean getTrialBalancePassed() {
        return trialBalancePassed;
    }

    public void setTrialBalancePassed(Boolean trialBalancePassed) {
        this.trialBalancePassed = trialBalancePassed;
    }

    public Boolean getProfitTransferred() {
        return profitTransferred;
    }

    public void setProfitTransferred(Boolean profitTransferred) {
        this.profitTransferred = profitTransferred;
    }

    public Boolean getCanClose() {
        return canClose;
    }

    public void setCanClose(Boolean canClose) {
        this.canClose = canClose;
    }

    public List<ChecklistItem> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items;
    }
}
