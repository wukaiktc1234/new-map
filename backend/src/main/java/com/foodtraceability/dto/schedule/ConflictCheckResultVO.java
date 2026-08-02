package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 冲突检查结果VO
 */
@Schema(description = "排班冲突检查结果")
public class ConflictCheckResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划ID */
    @Schema(description = "排班方案ID")
    private String planId;

    /** 检查时间 */
    @Schema(description = "检查时间")
    private String checkTime;

    /** 冲突汇总 */
    @Schema(description = "冲突汇总")
    private Summary summary;

    /** 是否可以发布 */
    @Schema(description = "是否可以发布（无error级冲突时为true）")
    private Boolean canPublish;

    /** 冲突详情列表 */
    @Schema(description = "冲突详情列表")
    private List<ConflictDetailVO> conflicts;

    /**
     * 冲突汇总
     */
    @Schema(description = "冲突汇总统计")
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 错误级冲突数 */
        @Schema(description = "错误级冲突数")
        private Integer error;

        /** 警告级冲突数 */
        @Schema(description = "警告级冲突数")
        private Integer warning;

        /** 信息级提示数 */
        @Schema(description = "信息级提示数")
        private Integer info;

        public Summary() {}

        public Summary(Integer error, Integer warning, Integer info) {
            this.error = error;
            this.warning = warning;
            this.info = info;
        }

        public Integer getError() { return error; }
        public void setError(Integer error) { this.error = error; }

        public Integer getWarning() { return warning; }
        public void setWarning(Integer warning) { this.warning = warning; }

        public Integer getInfo() { return info; }
        public void setInfo(Integer info) { this.info = info; }
    }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getCheckTime() { return checkTime; }
    public void setCheckTime(String checkTime) { this.checkTime = checkTime; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public Boolean getCanPublish() { return canPublish; }
    public void setCanPublish(Boolean canPublish) { this.canPublish = canPublish; }

    public List<ConflictDetailVO> getConflicts() { return conflicts; }
    public void setConflicts(List<ConflictDetailVO> conflicts) { this.conflicts = conflicts; }
}
