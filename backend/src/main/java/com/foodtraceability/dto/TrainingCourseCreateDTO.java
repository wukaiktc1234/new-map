package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * 培训课程创建DTO
 */
@Schema(description = "培训课程创建DTO")
public class TrainingCourseCreateDTO {

    @NotBlank(message = "课程标题不能为空")
    @Schema(description = "课程标题", example = "食品安全操作规范", required = true)
    private String title;

    @Schema(description = "课程描述")
    private String description;

    @NotBlank(message = "课程类型不能为空")
    @Schema(description = "课程类型（required/elective）", example = "required", required = true)
    private String courseType;

    @Schema(description = "关联知识库文章ID")
    private String relatedArticleId;

    @Schema(description = "讲师/部门")
    private String instructor;

    @Schema(description = "总课时数", example = "8")
    private Integer totalLessons;

    @NotNull(message = "时长不能为空")
    @PositiveOrZero(message = "时长必须大于等于0")
    @Schema(description = "时长（分钟）", example = "120", required = true)
    private Integer duration;

    @Schema(description = "截止日期（YYYY-MM-DD）", example = "2026-09-30")
    private LocalDate deadline;

    @NotNull(message = "是否可发证不能为空")
    @Schema(description = "是否可发证", example = "true", required = true)
    private Boolean certificateEligible;

    @NotNull(message = "证书有效期天数不能为空")
    @PositiveOrZero(message = "证书有效期天数必须大于等于0")
    @Schema(description = "证书有效期天数", example = "365", required = true)
    private Integer certificateValidityDays;

    @NotBlank(message = "发布状态不能为空")
    @Schema(description = "发布状态（draft/published/archived）", example = "draft", required = true)
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
