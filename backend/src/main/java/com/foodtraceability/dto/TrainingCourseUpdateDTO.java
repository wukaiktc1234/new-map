package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * 培训课程更新DTO
 * 所有字段可选，仅更新传入的字段
 */
@Schema(description = "培训课程更新DTO")
public class TrainingCourseUpdateDTO {

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "课程类型（required/elective）")
    private String courseType;

    @Schema(description = "关联知识库文章ID")
    private String relatedArticleId;

    @Schema(description = "讲师/部门")
    private String instructor;

    @Schema(description = "总课时数")
    private Integer totalLessons;

    @PositiveOrZero(message = "时长必须大于等于0")
    @Schema(description = "时长（分钟）")
    private Integer duration;

    @Schema(description = "截止日期（YYYY-MM-DD）")
    private LocalDate deadline;

    @Schema(description = "是否可发证")
    private Boolean certificateEligible;

    @PositiveOrZero(message = "证书有效期天数必须大于等于0")
    @Schema(description = "证书有效期天数")
    private Integer certificateValidityDays;

    @Schema(description = "发布状态（draft/published/archived）")
    private String publishStatus;

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
}
