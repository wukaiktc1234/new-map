package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知记录实体类
 * 记录排班生命周期中的通知发送情况
 */
@TableName("notification_logs")
public class NotificationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /**
     * 业务类型
     * schedule_published-排班发布 schedule_withdrawn-排班撤回
     * swap_approved-换班通过 swap_rejected-换班拒绝 daily_reminder-明日提醒
     */
    @TableField("business_type")
    private String businessType;

    /** 业务ID(方案ID/换班申请ID等) */
    @TableField("business_id")
    private String businessId;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知内容(支持模板变量) */
    @TableField("content")
    private String content;

    /**
     * 接收人用户ID数组(JSONB)
     * [101,102,103]
     */
    @TableField("receiver_ids")
    private String receiverIds;

    /**
     * 接收人姓名数组(JSONB,冗余)
     */
    @TableField("receiver_names")
    private String receiverNames;

    /**
     * 发送渠道
     * in_app-站内信(本期实现) sms-短信 email-邮件 wechat_work-企微(预留)
     */
    @TableField("channel")
    private String channel;

    /**
     * 发送状态
     * pending-待发送 sending-发送中 success-成功 failed-失败
     */
    @TableField("send_status")
    private String sendStatus;

    /** 实际发送时间 */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /** 重试次数 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 错误信息(失败时记录) */
    @TableField("error_message")
    private String errorMessage;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 业务类型常量 ====================

    /** 排班发布 */
    public static final String BUSINESS_TYPE_SCHEDULE_PUBLISHED = "schedule_published";
    /** 排班撤回 */
    public static final String BUSINESS_TYPE_SCHEDULE_WITHDRAWN = "schedule_withdrawn";
    /** 换班通过 */
    public static final String BUSINESS_TYPE_SWAP_APPROVED = "swap_approved";
    /** 换班拒绝 */
    public static final String BUSINESS_TYPE_SWAP_REJECTED = "swap_rejected";
    /** 明日提醒 */
    public static final String BUSINESS_TYPE_DAILY_REMINDER = "daily_reminder";

    // ==================== 渠道常量 ====================

    /** 站内信(本期实现) */
    public static final String CHANNEL_IN_APP = "in_app";
    /** 短信(预留) */
    public static final String CHANNEL_SMS = "sms";
    /** 邮件(预留) */
    public static final String CHANNEL_EMAIL = "email";
    /** 企微(预留) */
    public static final String CHANNEL_WECHAT_WORK = "wechat_work";

    // ==================== 发送状态常量 ====================

    /** 待发送 */
    public static final String SEND_STATUS_PENDING = "pending";
    /** 发送中 */
    public static final String SEND_STATUS_SENDING = "sending";
    /** 发送成功 */
    public static final String SEND_STATUS_SUCCESS = "success";
    /** 发送失败 */
    public static final String SEND_STATUS_FAILED = "failed";

    // ==================== Getter & Setter ====================

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
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

    public String getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(String receiverIds) {
        this.receiverIds = receiverIds;
    }

    public String getReceiverNames() {
        return receiverNames;
    }

    public void setReceiverNames(String receiverNames) {
        this.receiverNames = receiverNames;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
