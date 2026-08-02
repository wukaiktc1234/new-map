package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 文章审核记录实体类
 * 记录文章在联邦式审核流程中每个节点的操作历史
 *
 * 审核动作（action）：
 * - submit: 提交审核
 * - dept_approve: 部门审核通过
 * - dept_reject: 部门审核退回
 * - final_approve: 店长终审通过
 * - final_reject: 店长终审退回
 * - publish: 直接发布（普通内容）
 * - archive: 归档
 *
 * 操作人角色（operator_role）：
 * - contributor: 内容贡献者
 * - dept_manager: 部门管理员
 * - hr_manager: 人事管理员
 * - super_admin: 超级管理员
 */
@TableName("article_review_record")
public class ArticleReviewRecord {

    /**
     * 审核记录ID（主键，业务编号 RR-xxx）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String recordId;

    /**
     * 文章ID
     */
    @TableField("article_id")
    private String articleId;

    /**
     * 审核动作（submit/dept_approve/dept_reject/final_approve/final_reject/publish/archive）
     */
    @TableField("action")
    private String action;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private String operatorId;

    /**
     * 操作人姓名
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作人角色（contributor/dept_manager/hr_manager/super_admin）
     */
    @TableField("operator_role")
    private String operatorRole;

    /**
     * 操作时间
     */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /**
     * 审核意见
     */
    @TableField("comment")
    private String comment;

    /**
     * 退回原因（action 为 dept_reject 或 final_reject 时有值）
     */
    @TableField("reject_reason")
    private String rejectReason;

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

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
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
