package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 培训课程实体类
 * 通过 related_article_id 关联知识库文章，复用文章内容作为课程学习材料
 *
 * 课程类型（course_type）：
 * - required: 必修课
 * - elective: 选修课
 *
 * 发布状态（publish_status）：
 * - draft: 草稿
 * - published: 已发布
 * - archived: 已归档
 */
@TableName("training_course")
public class TrainingCourse {

    /**
     * 课程ID（主键，业务编号 tr-xxx）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 课程标题
     */
    @TableField("title")
    private String title;

    /**
     * 课程描述
     */
    @TableField("description")
    private String description;

    /**
     * 课程类型（required/elective）
     */
    @TableField("course_type")
    private String courseType;

    /**
     * 关联知识库文章ID
     */
    @TableField("related_article_id")
    private String relatedArticleId;

    /**
     * 关联文章标题（冗余字段）
     */
    @TableField("related_article_title")
    private String relatedArticleTitle;

    /**
     * 讲师
     */
    @TableField("instructor")
    private String instructor;

    /**
     * 总课时数
     */
    @TableField("total_lessons")
    private Integer totalLessons;

    /**
     * 时长（分钟）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 截止日期
     */
    @TableField("deadline")
    private LocalDate deadline;

    /**
     * 是否可发证
     */
    @TableField("certificate_eligible")
    private Boolean certificateEligible;

    /**
     * 证书有效期天数
     */
    @TableField("certificate_validity_days")
    private Integer certificateValidityDays;

    /**
     * 发布状态（draft/published/archived）
     */
    @TableField("publish_status")
    private String publishStatus;

    /**
     * 分配人数
     */
    @TableField("assigned_count")
    private Integer assignedCount;

    /**
     * 完成人数
     */
    @TableField("completed_count")
    private Integer completedCount;

    /**
     * 平均分
     */
    @TableField("average_score")
    private Integer averageScore;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
