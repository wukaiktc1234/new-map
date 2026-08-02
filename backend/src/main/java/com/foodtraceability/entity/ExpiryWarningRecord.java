package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预警记录实体类
 * 记录所有临期预警信息，支持处理跟踪和统计分析
 */
@TableName("expiry_warning_records")
@Schema(description = "预警记录实体")
public class ExpiryWarningRecord {

    /** 主键ID */
    @TableId(value = "record_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long recordId;

    /** 关联规则ID */
    @TableField("rule_id")
    @Schema(description = "规则ID")
    private Long ruleId;

    /** 目标类型：1原料批次 2证件 3健康证 4合同 */
    @TableField("target_type")
    @Schema(description = "目标类型", example = "1")
    private Integer targetType;

    /** 目标ID */
    @TableField("target_id")
    @Schema(description = "目标ID")
    private Long targetId;

    /** 目标名称 */
    @TableField("target_name")
    @Schema(description = "目标名称", example = "土豆-BATCH20260420")
    private String targetName;

    /** 到期日期 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "到期日期")
    private LocalDate expiryDate;

    /** 剩余天数 */
    @TableField("days_remaining")
    @Schema(description = "剩余天数", example = "5")
    private Integer daysRemaining;

    /** 预警级别：1一般 2紧急 3严重 */
    @TableField("warning_level")
    @Schema(description = "预警级别", example = "2")
    private Integer warningLevel;

    /** 预警消息 */
    @TableField("message")
    @Schema(description = "预警消息", example = "土豆(批次BATCH20260420)将在5天后过期")
    private String message;

    /** 是否已处理 */
    @TableField("is_handled")
    @Schema(description = "是否已处理")
    private Boolean isHandled;

    /** 处理时间 */
    @TableField("handle_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    /** 处理结果 */
    @TableField("handle_result")
    @Schema(description = "处理结果")
    private String handleResult;

    /** 通知时间 */
    @TableField("notify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "通知时间")
    private LocalDateTime notifyTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    // Getter和Setter方法

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
    public Integer getWarningLevel() { return warningLevel; }
    public void setWarningLevel(Integer warningLevel) { this.warningLevel = warningLevel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getIsHandled() { return isHandled; }
    public void setIsHandled(Boolean isHandled) { this.isHandled = isHandled; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public LocalDateTime getNotifyTime() { return notifyTime; }
    public void setNotifyTime(LocalDateTime notifyTime) { this.notifyTime = notifyTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
