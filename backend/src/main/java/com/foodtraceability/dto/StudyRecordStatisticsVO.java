package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 学习记录统计 VO
 */
@Schema(description = "学习记录统计VO")
public class StudyRecordStatisticsVO {

    @Schema(description = "总学习记录数")
    private Integer totalRecords;

    @Schema(description = "已完成学习人数")
    private Integer completedCount;

    @Schema(description = "学习中人数")
    private Integer inProgressCount;

    @Schema(description = "测验通过人数")
    private Integer quizPassedCount;

    @Schema(description = "平均阅读进度")
    private Integer avgReadProgress;

    @Schema(description = "平均测验得分")
    private Integer avgQuizScore;

    @Schema(description = "发证数量")
    private Integer certifiedCount;

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(Integer inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public Integer getQuizPassedCount() {
        return quizPassedCount;
    }

    public void setQuizPassedCount(Integer quizPassedCount) {
        this.quizPassedCount = quizPassedCount;
    }

    public Integer getAvgReadProgress() {
        return avgReadProgress;
    }

    public void setAvgReadProgress(Integer avgReadProgress) {
        this.avgReadProgress = avgReadProgress;
    }

    public Integer getAvgQuizScore() {
        return avgQuizScore;
    }

    public void setAvgQuizScore(Integer avgQuizScore) {
        this.avgQuizScore = avgQuizScore;
    }

    public Integer getCertifiedCount() {
        return certifiedCount;
    }

    public void setCertifiedCount(Integer certifiedCount) {
        this.certifiedCount = certifiedCount;
    }
}
