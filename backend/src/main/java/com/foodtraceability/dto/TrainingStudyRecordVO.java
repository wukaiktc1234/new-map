package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 培训学习记录VO
 */
@Schema(description = "培训学习记录VO")
public class TrainingStudyRecordVO {

    @Schema(description = "学习记录ID")
    private String id;

    @Schema(description = "员工ID")
    private String employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "员工工号")
    private String employeeCode;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "文章ID")
    private String articleId;

    @Schema(description = "文章标题")
    private String articleTitle;

    @Schema(description = "课程ID")
    private String courseId;

    @Schema(description = "课程标题")
    private String courseTitle;

    @Schema(description = "开始学习时间")
    private LocalDateTime startTime;

    @Schema(description = "结束学习时间")
    private LocalDateTime endTime;

    @Schema(description = "学习时长（秒）")
    private Integer durationSeconds;

    @Schema(description = "是否完成")
    private Boolean completed;

    @Schema(description = "分数（0-100）")
    private Integer score;

    @Schema(description = "证书ID")
    private String certificateId;

    @Schema(description = "证书到期时间")
    private LocalDateTime certificateExpiry;

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

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public LocalDateTime getCertificateExpiry() {
        return certificateExpiry;
    }

    public void setCertificateExpiry(LocalDateTime certificateExpiry) {
        this.certificateExpiry = certificateExpiry;
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
