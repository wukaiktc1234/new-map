package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 员工学习记录视图对象
 */
@Schema(description = "员工学习记录VO")
public class StudyRecordVO {

    @Schema(description = "学习记录ID")
    private String id;

    @Schema(description = "文章ID")
    private String articleId;

    @Schema(description = "文章标题")
    private String articleTitle;

    @Schema(description = "文章分类")
    private String articleCategory;

    @Schema(description = "员工ID")
    private String employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "员工工号")
    private String employeeNo;

    @Schema(description = "所属门店")
    private String storeName;

    @Schema(description = "学习状态（not_started/in_progress/completed）")
    private String studyStatus;

    @Schema(description = "阅读进度（0-100）")
    private Integer readProgress;

    @Schema(description = "累计阅读时长（秒）")
    private Integer readDuration;

    @Schema(description = "最后阅读时间")
    private LocalDateTime lastReadTime;

    @Schema(description = "首次阅读时间")
    private LocalDateTime firstReadTime;

    @Schema(description = "测验状态（not_attempted/passed/failed）")
    private String quizStatus;

    @Schema(description = "测验得分（0-100）")
    private Integer quizScore;

    @Schema(description = "测验答题次数")
    private Integer quizAttemptCount;

    @Schema(description = "最后测验时间")
    private LocalDateTime lastQuizTime;

    @Schema(description = "是否获得证书")
    private Boolean certified;

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
}
