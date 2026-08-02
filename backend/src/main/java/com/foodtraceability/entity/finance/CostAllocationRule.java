package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成本分摊规则实体类
 * 定义公共成本（如租金、水电）的分摊方式和规则
 */
@TableName("cost_allocation_rules")
public class CostAllocationRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则ID */
    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    /** 规则名称 */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 对应的成本类型
     * 1-食材成本 2-人工成本 3-租金成本 4-水电成本
     * 5-折旧成本 6-包装成本 7-其他成本
     */
    @TableField("cost_type")
    private Integer costType;

    /**
     * 分摊依据
     * 1-按营业额 2-按面积 3-按人数 4-按工时 5-固定比例
     */
    @TableField("allocation_basis")
    private Integer allocationBasis;

    /** 分摊公式或规则描述 */
    @TableField("allocation_formula")
    private String allocationFormula;

    /** 是否启用 */
    @TableField("is_enabled")
    private Boolean isEnabled;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public Integer getCostType() { return costType; }
    public void setCostType(Integer costType) { this.costType = costType; }

    public Integer getAllocationBasis() { return allocationBasis; }
    public void setAllocationBasis(Integer allocationBasis) { this.allocationBasis = allocationBasis; }

    public String getAllocationFormula() { return allocationFormula; }
    public void setAllocationFormula(String allocationFormula) { this.allocationFormula = allocationFormula; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
