package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训学习记录实体类
 * 记录员工对培训课程的学习情况，与知识库 article_study_record 独立
 *
 * 与 article_study_record 的区别：
 * - 本表记录培训课程维度（course_id）
 * - article_study_record 记录知识库文章维度（article_id）
 * - 同一员工学习同一文章，可能同时产生两类记录
 */
@TableName("training_study_record")
public class TrainingStudyRecord {

    /**
     * 学习记录ID（主键，业务编号 TSR-xxx）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 员工ID
     */
    @TableField("employee_id")
    private String employeeId;

    /**
     * 员工姓名
     */
    @TableField("employee_name")
    private String employeeName;

    /**
     * 员工工号
     */
    @TableField("employee_code")
    private String employeeCode;

    /**
     * 部门名称
     */
    @TableField("department_name")
    private String departmentName;

    /**
     * 文章ID
     */
    @TableField("article_id")
    private String articleId;

    /**
     * 文章标题
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * 课程ID
     */
    @TableField("course_id")
    private String courseId;

    /**
     * 课程标题
     */
    @TableField("course_title")
    private String courseTitle;

    /**
     * 开始学习时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束学习时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 学习时长（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 是否完成
     */
    @TableField("completed")
    private Boolean completed;

    /**
     * 分数（0-100）
     */
    @TableField("score")
    private Integer score;

    /**
     * 证书ID
     */
    @TableField("certificate_id")
    private String certificateId;

    /**
     * 证书到期时间
     */
    @TableField("certificate_expiry")
    private LocalDateTime certificateExpiry;

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
