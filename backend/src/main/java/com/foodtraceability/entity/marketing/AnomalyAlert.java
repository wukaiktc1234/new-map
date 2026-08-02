package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异常交易告警实体类
 * 风控触发异常时生成告警记录
 *
 * 状态（status）：pending-待处理 handled-已处理 ignored-已忽略
 */
@TableName("anomaly_alerts")
public class AnomalyAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 告警ID */
    @TableId(value = "alert_id", type = IdType.ASSIGN_ID)
    private String alertId;

    /** 会员ID */
    @TableField("member_id")
    private String memberId;

    /** 会员姓名 */
    @TableField("member_name")
    private String memberName;

    /** 告警类型 */
    @TableField("alert_type")
    private String alertType;

    /** 告警描述 */
    @TableField("description")
    private String description;

    /** 触发金额（分） */
    @TableField("trigger_amount")
    private Long triggerAmount;

    /** 告警时间 */
    @TableField("alert_time")
    private LocalDateTime alertTime;

    /** 处理状态 */
    @TableField("status")
    private String status;

    /** 处理人 */
    @TableField("handler")
    private String handler;

    /** 处理时间 */
    @TableField("handle_time")
    private LocalDateTime handleTime;

    /** 处理备注 */
    @TableField("handle_remark")
    private String handleRemark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 状态常量 ====================
    /** 状态：待处理 */
    public static final String STATUS_PENDING = "pending";
    /** 状态：已处理 */
    public static final String STATUS_HANDLED = "handled";
    /** 状态：已忽略 */
    public static final String STATUS_IGNORED = "ignored";
    /** 告警类型：频繁充值 */
    public static final String TYPE_FREQUENT_RECHARGE = "frequent_recharge";
    /** 告警类型：充值后快速消费 */
    public static final String TYPE_FAST_CONSUME = "fast_consume";
    /** 告警类型：高退款率 */
    public static final String TYPE_HIGH_REFUND_RATE = "high_refund_rate";
    /** 告警类型：新会员高额充值 */
    public static final String TYPE_NEW_MEMBER_HIGH_RECHARGE = "new_member_high_recharge";
    /** 告警类型：多设备充值 */
    public static final String TYPE_MULTI_DEVICE = "multi_device";
    /** 告警类型：超限额 */
    public static final String TYPE_OVER_LIMIT = "over_limit";

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

    public Long getTriggerAmount() { return triggerAmount; }
    public void setTriggerAmount(Long triggerAmount) { this.triggerAmount = triggerAmount; }

    public LocalDateTime getAlertTime() { return alertTime; }
    public void setAlertTime(LocalDateTime alertTime) { this.alertTime = alertTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }

    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }

    public String getHandleRemark() { return handleRemark; }
    public void setHandleRemark(String handleRemark) { this.handleRemark = handleRemark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
