package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 通知日志视图对象
 */
@Schema(description = "排班通知日志")
public class NotificationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Schema(description = "日志ID")
    private String logId;

    /** 业务类型 */
    @Schema(description = "业务类型")
    private String businessType;

    /** 业务ID */
    @Schema(description = "业务ID")
    private String businessId;

    /** 通知标题 */
    @Schema(description = "通知标题")
    private String title;

    /** 通知内容 */
    @Schema(description = "通知内容")
    private String content;

    /** 接收人ID列表 */
    @Schema(description = "接收人ID列表")
    private List<String> receiverIds;

    /** 接收人姓名列表 */
    @Schema(description = "接收人姓名列表")
    private List<String> receiverNames;

    /** 通知渠道 */
    @Schema(description = "通知渠道: in_app/sms/email/wechat_work")
    private String channel;

    /** 发送状态 */
    @Schema(description = "发送状态: pending/sending/success/failed")
    private String sendStatus;

    /** 发送时间 */
    @Schema(description = "发送时间")
    private String sendTime;

    /** 重试次数 */
    @Schema(description = "重试次数")
    private Integer retryCount;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private String createTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getReceiverIds() { return receiverIds; }
    public void setReceiverIds(List<String> receiverIds) { this.receiverIds = receiverIds; }

    public List<String> getReceiverNames() { return receiverNames; }
    public void setReceiverNames(List<String> receiverNames) { this.receiverNames = receiverNames; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getSendStatus() { return sendStatus; }
    public void setSendStatus(String sendStatus) { this.sendStatus = sendStatus; }

    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
