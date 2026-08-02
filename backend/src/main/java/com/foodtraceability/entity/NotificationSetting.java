package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

/**
 * 通知设置实体类（用户偏好版）
 * 已废弃，请使用 NotificationUserPreference 替代
 * @deprecated 使用 {@link NotificationUserPreference} 替代
 */
@Deprecated
@TableName("notification_user_preference")
public class NotificationSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 通知类型：
     * REJECT_AUDIT - 审核拒绝通知
     * APPROVE_AUDIT - 审核通过通知
     * AUTO_APPROVE_AUDIT - 自动审核通过通知
     * PENDING_AUDIT - 待审核通知
     * TIMEOUT_AUDIT - 审核超时通知
     */
    @TableField("notification_type")
    private String notificationType;

    /**
     * 通知方式：
     * SYSTEM - 站内消息
     * EMAIL - 邮件通知
     * SMS - 短信通知
     */
    @TableField("notification_method")
    private String notificationMethod;

    /**
     * 是否启用：
     * 0 - 禁用
     * 1 - 启用
     */
    @TableField("is_enabled")
    private Integer isEnabled;

    /**
     * 通知频率：
     * REALTIME - 实时通知
     * DAILY - 每日汇总
     * WEEKLY - 每周汇总
     */
    @TableField("frequency")
    private String frequency;

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

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotificationMethod() {
        return notificationMethod;
    }

    public void setNotificationMethod(String notificationMethod) {
        this.notificationMethod = notificationMethod;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
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

    @Override
    public String toString() {
        return "NotificationSetting{" +
            "id=" + id +
            ", userId=" + userId +
            ", notificationType='" + notificationType + '\'' +
            ", notificationMethod='" + notificationMethod + '\'' +
            ", isEnabled=" + isEnabled +
            ", frequency='" + frequency + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            '}';
    }
}