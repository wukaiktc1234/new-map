package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训课程VO
 * 返回给前端的课程视图对象
 */
@Schema(description = "培训课程VO")
public class TrainingCourseVO {

    @Schema(description = "课程ID")
    private String id;

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "课程类型（required/elective）")
    private String courseType;

    @Schema(description = "关联知识库文章ID")
    private String relatedArticleId;

    @Schema(description = "关联文章标题")
    private String relatedArticleTitle;

    @Schema(description = "讲师/部门")
    private String instructor;

    @Schema(description = "总课时数")
    private Integer totalLessons;

    @Schema(description = "时长（分钟）")
    private Integer duration;

    @Schema(description = "截止日期")
    private LocalDate deadline;

    @Schema(description = "是否可发证")
    private Boolean certificateEligible;

    @Schema(description = "证书有效期天数")
    private Integer certificateValidityDays;

    @Schema(description = "发布状态（draft/published/archived）")
    private String publishStatus;

    @Schema(description = "分配人数")
    private Integer assignedCount;

    @Schema(description = "完成人数")
    private Integer completedCount;

    @Schema(description = "平均分")
    private Integer averageScore;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public String getRelatedArticleId() {
        return relatedArticleId;
    }

    public void setRelatedArticleId(String relatedArticleId) {
        this.relatedArticleId = relatedArticleId;
    }

    public String getRelatedArticleTitle() {
        return relatedArticleTitle;
    }

    public void setRelatedArticleTitle(String relatedArticleTitle) {
        this.relatedArticleTitle = relatedArticleTitle;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public Integer getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(Integer totalLessons) {
        this.totalLessons = totalLessons;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Boolean getCertificateEligible() {
        return certificateEligible;
    }

    public void setCertificateEligible(Boolean certificateEligible) {
        this.certificateEligible = certificateEligible;
    }

    public Integer getCertificateValidityDays() {
        return certificateValidityDays;
    }

    public void setCertificateValidityDays(Integer certificateValidityDays) {
        this.certificateValidityDays = certificateValidityDays;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Integer getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(Integer assignedCount) {
        this.assignedCount = assignedCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Integer averageScore) {
        this.averageScore = averageScore;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
