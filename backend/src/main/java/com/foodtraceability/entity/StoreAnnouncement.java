package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 门店公告实体类
 * 用于管理门店的通知、活动、警告等信息发布
 */
@TableName("store_announcements")
public class StoreAnnouncement {

    /** 公告ID */
    @TableId(type = IdType.AUTO)
    private Long announcementId;

    /** 门店ID */
    private Long storeId;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /**
     * 公告类型
     * 1-通知 2-活动 3-警告 4-其他
     */
    private Integer announcementType;

    /** 发布人ID */
    private Long publishUserId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /**
     * 优先级
     * 1-低 2-中 3-高 4-紧急
     */
    private Integer priority;

    /** 是否置顶 */
    private Boolean isTop;

    /**
     * 目标角色（JSON数组格式）
     * 例如：["all","cashier","manager"]
     */
    private String targetRoles;

    /** 阅读次数 */
    private Integer readCount;

    /** 过期时间 */
    private LocalDateTime expiryTime;

    /**
     * 发布状态
     * 1-草稿 2-已发布 3-已撤回
     */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getAnnouncementId() {
        return announcementId;
    }

    public void setAnnouncementId(Long announcementId) {
        this.announcementId = announcementId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getAnnouncementType() {
        return announcementType;
    }

    public void setAnnouncementType(Integer announcementType) {
        this.announcementType = announcementType;
    }

    public Long getPublishUserId() {
        return publishUserId;
    }

    public void setPublishUserId(Long publishUserId) {
        this.publishUserId = publishUserId;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsTop() {
        return isTop;
    }

    public void setIsTop(Boolean isTop) {
        this.isTop = isTop;
    }

    public String getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(String targetRoles) {
        this.targetRoles = targetRoles;
    }

    public Integer getReadCount() {
        return readCount;
    }

    public void setReadCount(Integer readCount) {
        this.readCount = readCount;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
