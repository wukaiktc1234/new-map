package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 手动触发测试事件 DTO
 * 用于 Sprint 1 验收场景 E1（手动触发 TestBusinessEvent 验证 EventBus 端到端链路）
 * 对应 spec F-001（业务事件统一发布能力）
 */
public class TestEventTriggerDTO {

    /** 事件名称（用于模板变量 {eventName} 替换） */
    @NotBlank(message = "事件名称不能为空")
    @Size(max = 100, message = "事件名称长度不能超过100")
    private String eventName;

    /** 操作人名称（用于模板变量 {operatorName} 替换） */
    @NotBlank(message = "操作人名称不能为空")
    @Size(max = 50, message = "操作人名称长度不能超过50")
    private String operatorName;

    /** 接收人用户 ID 列表 */
    @NotEmpty(message = "接收人列表不能为空")
    private List<@jakarta.validation.constraints.NotNull(message = "用户ID不能为空") Long> recipientUserIds;

    /** 渠道列表（SITE_MSG / EMAIL） */
    @NotEmpty(message = "渠道列表不能为空")
    private List<String> channels;

    public TestEventTriggerDTO() {
    }

    public String getEventName() {
        return this.eventName;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public List<Long> getRecipientUserIds() {
        return this.recipientUserIds;
    }

    public List<String> getChannels() {
        return this.channels;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public void setRecipientUserIds(List<Long> recipientUserIds) {
        this.recipientUserIds = recipientUserIds;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }
}
