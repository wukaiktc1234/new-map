package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 费用分摊规则实体类
 * 用于定义费用分摊的规则
 */
@TableName("cost_allocation_rule")
public class CostAllocationRule {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 规则名称
     */
    private String ruleName;
    /**
     * 规则类型
     * - FIXED: 固定规则
     * - DYNAMIC: 动态规则
     */
    private String ruleType;
    /**
     * 分摊方式
     * - BY_PERCENTAGE: 按比例分摊
     * - BY_AMOUNT: 按金额分摊
     * - BY_QUANTITY: 按数量分摊
     * - BY_REVENUE: 按收入分摊
     */
    private String allocationMethod;
    /**
     * 适用费用类型
     */
    private String applicableCostType;
    /**
     * 规则状态
     * - ACTIVE: 启用
     * - INACTIVE: 禁用
     */
    private String status;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * 规则表达式
     * 用于动态计算分摊比例或金额
     */
    private String ruleExpression;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建人
     */
    private Long createBy;
    /**
     * 更新人
     */
    private Long updateBy;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getAllocationMethod() {
        return allocationMethod;
    }

    public void setAllocationMethod(String allocationMethod) {
        this.allocationMethod = allocationMethod;
    }

    public String getApplicableCostType() {
        return applicableCostType;
    }

    public void setApplicableCostType(String applicableCostType) {
        this.applicableCostType = applicableCostType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getRuleExpression() {
        return ruleExpression;
    }

    public void setRuleExpression(String ruleExpression) {
        this.ruleExpression = ruleExpression;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public CostAllocationRule() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CostAllocationRule)) return false;
        final CostAllocationRule other = (CostAllocationRule) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$ruleName = this.getRuleName();
        final java.lang.Object other$ruleName = other.getRuleName();
        if (this$ruleName == null ? other$ruleName != null : !this$ruleName.equals(other$ruleName)) return false;
        final java.lang.Object this$ruleType = this.getRuleType();
        final java.lang.Object other$ruleType = other.getRuleType();
        if (this$ruleType == null ? other$ruleType != null : !this$ruleType.equals(other$ruleType)) return false;
        final java.lang.Object this$allocationMethod = this.getAllocationMethod();
        final java.lang.Object other$allocationMethod = other.getAllocationMethod();
        if (this$allocationMethod == null ? other$allocationMethod != null : !this$allocationMethod.equals(other$allocationMethod)) return false;
        final java.lang.Object this$applicableCostType = this.getApplicableCostType();
        final java.lang.Object other$applicableCostType = other.getApplicableCostType();
        if (this$applicableCostType == null ? other$applicableCostType != null : !this$applicableCostType.equals(other$applicableCostType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$ruleExpression = this.getRuleExpression();
        final java.lang.Object other$ruleExpression = other.getRuleExpression();
        if (this$ruleExpression == null ? other$ruleExpression != null : !this$ruleExpression.equals(other$ruleExpression)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CostAllocationRule;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $ruleName = this.getRuleName();
        result = result * PRIME + ($ruleName == null ? 43 : $ruleName.hashCode());
        final java.lang.Object $ruleType = this.getRuleType();
        result = result * PRIME + ($ruleType == null ? 43 : $ruleType.hashCode());
        final java.lang.Object $allocationMethod = this.getAllocationMethod();
        result = result * PRIME + ($allocationMethod == null ? 43 : $allocationMethod.hashCode());
        final java.lang.Object $applicableCostType = this.getApplicableCostType();
        result = result * PRIME + ($applicableCostType == null ? 43 : $applicableCostType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $ruleExpression = this.getRuleExpression();
        result = result * PRIME + ($ruleExpression == null ? 43 : $ruleExpression.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CostAllocationRule(id=" + this.getId() + ", ruleName=" + this.getRuleName() + ", ruleType=" + this.getRuleType() + ", allocationMethod=" + this.getAllocationMethod() + ", applicableCostType=" + this.getApplicableCostType() + ", status=" + this.getStatus() + ", priority=" + this.getPriority() + ", ruleExpression=" + this.getRuleExpression() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ")";
    }
}
