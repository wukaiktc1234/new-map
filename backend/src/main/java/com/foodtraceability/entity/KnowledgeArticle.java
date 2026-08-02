package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 知识库文章实体类
 * 对接员工端知识库模块，支持管理端 CRUD 与联邦式审核流程
 *
 * 分类（category）：
 * - safety: 安全规范
 * - service: 服务标准
 * - manual: 操作手册
 * - policy: 公司制度
 *
 * 发布状态（publish_status）流转：
 * - draft(草稿) → pending_review(待部门审核)
 * - pending_review → pending_final(待店长终审，仅 is_important=true) → published(已发布)
 * - pending_review → published(已发布，仅 is_important=false 直接发布)
 * - 任意审核节点 → rejected(已退回)
 * - published → archived(已归档)
 */
@TableName("knowledge_article")
public class KnowledgeArticle {

    /**
     * 文章ID（主键，业务编号 kb-xxx）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 文章标题
     */
    @TableField("title")
    private String title;

    /**
     * 文章摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 分类（safety/service/manual/policy）
     */
    @TableField("category")
    private String category;

    /**
     * 图标名称（Element Plus 图标）
     */
    @TableField("icon")
    private String icon;

    /**
     * 文章内容（Markdown 格式）
     */
    @TableField("content")
    private String content;

    /**
     * 封面图URL
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 作者姓名
     */
    @TableField("author")
    private String author;

    /**
     * 发布状态（draft/pending_review/pending_final/published/archived/rejected）
     */
    @TableField("publish_status")
    private String publishStatus;

    /**
     * 阅读次数
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 标签（JSON 数组字符串）
     */
    @TableField("tags")
    private String tags;

    /**
     * 测验题目（JSON 数组字符串）
     */
    @TableField("quiz")
    private String quiz;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

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
     * 作者ID
     */
    @TableField("author_id")
    private String authorId;

    /**
     * 作者所属部门
     */
    @TableField("author_department")
    private String authorDepartment;

    /**
     * 审核人ID
     */
    @TableField("reviewer_id")
    private String reviewerId;

    /**
     * 审核人姓名
     */
    @TableField("reviewer_name")
    private String reviewerName;

    /**
     * 审核时间
     */
    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    /**
     * 审核意见
     */
    @TableField("review_comment")
    private String reviewComment;

    /**
     * 退回原因
     */
    @TableField("reject_reason")
    private String rejectReason;

    /**
     * 发布人ID
     */
    @TableField("published_by")
    private String publishedBy;

    /**
     * 是否为重要内容（true 时需店长终审）
     */
    @TableField("is_important")
    private Boolean isImportant;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getQuiz() {
        return quiz;
    }

    public void setQuiz(String quiz) {
        this.quiz = quiz;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorDepartment() {
        return authorDepartment;
    }

    public void setAuthorDepartment(String authorDepartment) {
        this.authorDepartment = authorDepartment;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public Boolean getIsImportant() {
        return isImportant;
    }

    public void setIsImportant(Boolean isImportant) {
        this.isImportant = isImportant;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
