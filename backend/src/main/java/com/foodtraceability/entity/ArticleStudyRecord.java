package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 员工学习记录实体类
 * 单条记录表示某员工对某文章的学习情况，对接员工端学习数据
 *
 * 学习状态（study_status）：
 * - not_started: 未开始
 * - in_progress: 学习中
 * - completed: 已完成
 *
 * 测验状态（quiz_status）：
 * - not_attempted: 未答题
 * - passed: 已通过
 * - failed: 未通过
 */
@TableName("article_study_record")
public class ArticleStudyRecord {

    /**
     * 学习记录ID（主键，业务编号 SR-xxx）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文章ID
     */
    @TableField("article_id")
    private String articleId;

    /**
     * 文章标题（冗余字段，便于展示）
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * 文章分类
     */
    @TableField("article_category")
    private String articleCategory;

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
    @TableField("employee_no")
    private String employeeNo;

    /**
     * 所属门店
     */
    @TableField("store_name")
    private String storeName;

    /**
     * 学习状态（not_started/in_progress/completed）
     */
    @TableField("study_status")
    private String studyStatus;

    /**
     * 阅读进度（0-100）
     */
    @TableField("read_progress")
    private Integer readProgress;

    /**
     * 累计阅读时长（秒）
     */
    @TableField("read_duration")
    private Integer readDuration;

    /**
     * 最后阅读时间
     */
    @TableField("last_read_time")
    private LocalDateTime lastReadTime;

    /**
     * 首次阅读时间
     */
    @TableField("first_read_time")
    private LocalDateTime firstReadTime;

    /**
     * 测验状态（not_attempted/passed/failed）
     */
    @TableField("quiz_status")
    private String quizStatus;

    /**
     * 测验得分（0-100）
     */
    @TableField("quiz_score")
    private Integer quizScore;

    /**
     * 测验答题次数
     */
    @TableField("quiz_attempt_count")
    private Integer quizAttemptCount;

    /**
     * 最后测验时间
     */
    @TableField("last_quiz_time")
    private LocalDateTime lastQuizTime;

    /**
     * 是否获得证书（关联培训模块）
     */
    @TableField("certified")
    private Boolean certified;

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

    public String getArticleCategory() {
        return articleCategory;
    }

    public void setArticleCategory(String articleCategory) {
        this.articleCategory = articleCategory;
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

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStudyStatus() {
        return studyStatus;
    }

    public void setStudyStatus(String studyStatus) {
        this.studyStatus = studyStatus;
    }

    public Integer getReadProgress() {
        return readProgress;
    }

    public void setReadProgress(Integer readProgress) {
        this.readProgress = readProgress;
    }

    public Integer getReadDuration() {
        return readDuration;
    }

    public void setReadDuration(Integer readDuration) {
        this.readDuration = readDuration;
    }

    public LocalDateTime getLastReadTime() {
        return lastReadTime;
    }

    public void setLastReadTime(LocalDateTime lastReadTime) {
        this.lastReadTime = lastReadTime;
    }

    public LocalDateTime getFirstReadTime() {
        return firstReadTime;
    }

    public void setFirstReadTime(LocalDateTime firstReadTime) {
        this.firstReadTime = firstReadTime;
    }

    public String getQuizStatus() {
        return quizStatus;
    }

    public void setQuizStatus(String quizStatus) {
        this.quizStatus = quizStatus;
    }

    public Integer getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(Integer quizScore) {
        this.quizScore = quizScore;
    }

    public Integer getQuizAttemptCount() {
        return quizAttemptCount;
    }

    public void setQuizAttemptCount(Integer quizAttemptCount) {
        this.quizAttemptCount = quizAttemptCount;
    }

    public LocalDateTime getLastQuizTime() {
        return lastQuizTime;
    }

    public void setLastQuizTime(LocalDateTime lastQuizTime) {
        this.lastQuizTime = lastQuizTime;
    }

    public Boolean getCertified() {
        return certified;
    }

    public void setCertified(Boolean certified) {
        this.certified = certified;
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
