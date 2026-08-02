package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 临期预警规则实体类
 * 定义临期物品的预警规则，支持多种预警类型和通知方式
 */
@TableName("expiry_warning_rules")
@Schema(description = "临期预警规则实体")
public class ExpiryWarningRule {

    /** 主键ID */
    @TableId(value = "rule_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long ruleId;

    /** 规则名称 */
    @TableField("rule_name")
    @Schema(description = "规则名称", example = "原料临期预警")
    private String ruleName;

    /** 预警类型：1原料临期 2证件临期 3健康证临期 4合同到期 */
    @TableField("warning_type")
    @Schema(description = "预警类型", example = "1")
    private Integer warningType;

    /** 提前天数 */
    @TableField("advance_days")
    @Schema(description = "提前天数", example = "7")
    private Integer advanceDays;

    /** 检查频率：1每日 2每周 3每月 */
    @TableField("check_frequency")
    @Schema(description = "检查频率", example = "1")
    private Integer checkFrequency;

    /** 通知方式：1站内信 2邮件 3短信 4全部 */
    @TableField("notify_method")
    @Schema(description = "通知方式", example = "4")
    private Integer notifyMethod;

    /** 通知角色JSON数组 */
    @TableField("notify_roles")
    @Schema(description = "通知角色列表")
    private Object notifyRoles;

    /** 是否启用 */
    @TableField("is_enabled")
    @Schema(description = "是否启用")
    private Boolean isEnabled;

    /** 最后检查时间 */
    @TableField("last_check_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后检查时间")
    private LocalDateTime lastCheckTime;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    // Getter和Setter方法

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Integer getWarningType() { return warningType; }
    public void setWarningType(Integer warningType) { this.warningType = warningType; }
    public Integer getAdvanceDays() { return advanceDays; }
    public void setAdvanceDays(Integer advanceDays) { this.advanceDays = advanceDays; }
    public Integer getCheckFrequency() { return checkFrequency; }
    public void setCheckFrequency(Integer checkFrequency) { this.checkFrequency = checkFrequency; }
    public Integer getNotifyMethod() { return notifyMethod; }
    public void setNotifyMethod(Integer notifyMethod) { this.notifyMethod = notifyMethod; }
    public Object getNotifyRoles() { return notifyRoles; }
    public void setNotifyRoles(Object notifyRoles) { this.notifyRoles = notifyRoles; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public LocalDateTime getLastCheckTime() { return lastCheckTime; }
    public void setLastCheckTime(LocalDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
