package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 异常告警VO
 * 金额字段以元为单位（字符串），与前端契约一致
 */
@Schema(description = "异常告警信息")
public class AnomalyAlertVO {

    @Schema(description = "告警ID")
    private String alertId;

    @Schema(description = "会员ID")
    private String memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "告警类型: frequent_recharge/fast_consume/high_refund_rate/new_member_high_recharge/multi_device/over_limit")
    private String alertType;

    @Schema(description = "告警描述")
    private String description;

    @Schema(description = "触发金额（元）")
    private String triggerAmount;

    @Schema(description = "告警时间")
    private String alertTime;

    @Schema(description = "处理状态: pending/handled/ignored")
    private String status;

    @Schema(description = "处理人")
    private String handler;

    @Schema(description = "处理时间")
    private String handleTime;

    @Schema(description = "处理备注")
    private String handleRemark;

    // ==================== Getter & Setter ====================

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTriggerAmount() { return triggerAmount; }
    public void setTriggerAmount(String triggerAmount) { this.triggerAmount = triggerAmount; }

    public String getAlertTime() { return alertTime; }
    public void setAlertTime(String alertTime) { this.alertTime = alertTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }

    public String getHandleTime() { return handleTime; }
    public void setHandleTime(String handleTime) { this.handleTime = handleTime; }

    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }
}
