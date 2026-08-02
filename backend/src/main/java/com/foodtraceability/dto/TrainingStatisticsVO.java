package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 培训统计VO
 */
@Schema(description = "培训统计VO")
public class TrainingStatisticsVO {

    @Schema(description = "总课程数")
    private Integer totalCourses;

    @Schema(description = "已发布课程数")
    private Integer publishedCourses;

    @Schema(description = "总分配人数")
    private Integer totalAssigned;

    @Schema(description = "总完成人数")
    private Integer totalCompleted;

    @Schema(description = "完成率（0-1）")
    private Double completionRate;

    @Schema(description = "平均得分")
    private Double averageScore;

    @Schema(description = "即将到期证书数")
    private Integer expiringCertificates;

    public Integer getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(Integer totalCourses) {
        this.totalCourses = totalCourses;
    }

    public Integer getPublishedCourses() {
        return publishedCourses;
    }

    public void setPublishedCourses(Integer publishedCourses) {
        this.publishedCourses = publishedCourses;
    }

    public Integer getTotalAssigned() {
        return totalAssigned;
    }

    public void setTotalAssigned(Integer totalAssigned) {
        this.totalAssigned = totalAssigned;
    }

    public Integer getTotalCompleted() {
        return totalCompleted;
    }

    public void setTotalCompleted(Integer totalCompleted) {
        this.totalCompleted = totalCompleted;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getExpiringCertificates() {
        return expiringCertificates;
    }

    public void setExpiringCertificates(Integer expiringCertificates) {
        this.expiringCertificates = expiringCertificates;
    }
}
