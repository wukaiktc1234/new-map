package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤差异报告VO
 */
@Schema(description = "考勤差异报告")
public class AttendanceDiffReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报告ID */
    @Schema(description = "报告ID")
    private String reportId;

    /** 计划ID */
    @Schema(description = "排班方案ID")
    private String planId;

    /** 统计周期开始 */
    @Schema(description = "统计周期开始")
    private String periodStart;

    /** 统计周期结束 */
    @Schema(description = "统计周期结束")
    private String periodEnd;

    /** 生成时间 */
    @Schema(description = "生成时间")
    private String generateTime;

    /** 差异汇总 */
    @Schema(description = "差异汇总")
    private Summary summary;

    /** 差异明细列表 */
    @Schema(description = "差异明细列表")
    private List<AttendanceDiffDetailVO> details;

    /**
     * 差异汇总
     */
    @Schema(description = "差异汇总统计")
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 总排班条目数 */
        @Schema(description = "总排班条目数")
        private Integer totalEntries;

        /** 已打卡条目数 */
        @Schema(description = "已打卡条目数")
        private Integer checkedIn;

        /** 未打卡条目数 */
        @Schema(description = "未打卡条目数")
        private Integer notCheckedIn;

        /** 异常条目数 */
        @Schema(description = "异常条目数")
        private Integer abnormal;

        /** 迟到次数 */
        @Schema(description = "迟到次数")
        private Integer lateCount;

        /** 早退次数 */
        @Schema(description = "早退次数")
        private Integer earlyLeaveCount;

        /** 缺卡次数 */
        @Schema(description = "缺卡次数")
        private Integer missingCount;

        public Summary() {}

        public Integer getTotalEntries() { return totalEntries; }
        public void setTotalEntries(Integer totalEntries) { this.totalEntries = totalEntries; }

        public Integer getCheckedIn() { return checkedIn; }
        public void setCheckedIn(Integer checkedIn) { this.checkedIn = checkedIn; }

        public Integer getNotCheckedIn() { return notCheckedIn; }
        public void setNotCheckedIn(Integer notCheckedIn) { this.notCheckedIn = notCheckedIn; }

        public Integer getAbnormal() { return abnormal; }
        public void setAbnormal(Integer abnormal) { this.abnormal = abnormal; }

        public Integer getLateCount() { return lateCount; }
        public void setLateCount(Integer lateCount) { this.lateCount = lateCount; }

        public Integer getEarlyLeaveCount() { return earlyLeaveCount; }
        public void setEarlyLeaveCount(Integer earlyLeaveCount) { this.earlyLeaveCount = earlyLeaveCount; }

        public Integer getMissingCount() { return missingCount; }
        public void setMissingCount(Integer missingCount) { this.missingCount = missingCount; }
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }

    public String getGenerateTime() { return generateTime; }
    public void setGenerateTime(String generateTime) { this.generateTime = generateTime; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public List<AttendanceDiffDetailVO> getDetails() { return details; }
    public void setDetails(List<AttendanceDiffDetailVO> details) { this.details = details; }
}
