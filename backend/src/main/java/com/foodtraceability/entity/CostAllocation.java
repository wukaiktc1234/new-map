package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 费用分摊实体类
 * 用于管理企业的费用分摊信息
 */
@TableName("cost_allocation")
public class CostAllocation {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 财务支出记录ID
     */
    private Long financeRecordId;
    /**
     * 费用名称
     */
    private String costName;
    /**
     * 费用金额
     */
    private BigDecimal totalAmount;
    /**
     * 分摊规则ID
     */
    private Long allocationRuleId;
    /**
     * 分摊规则名称
     */
    private String allocationRuleName;
    /**
     * 分摊方式
     * - BY_PERCENTAGE: 按比例分摊
     * - BY_AMOUNT: 按金额分摊
     * - BY_QUANTITY: 按数量分摊
     * - BY_REVENUE: 按收入分摊
     */
    private String allocationMethod;
    /**
     * 分摊期间
     */
    private String allocationPeriod;
    /**
     * 分摊状态
     * - PENDING: 待分摊
     * - PROCESSING: 分摊中
     * - COMPLETED: 已完成
     * - FAILED: 分摊失败
     */
    private String status;
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

    public Long getFinanceRecordId() {
        return financeRecordId;
    }

    public void setFinanceRecordId(Long financeRecordId) {
        this.financeRecordId = financeRecordId;
    }

    public String getCostName() {
        return costName;
    }

    public void setCostName(String costName) {
        this.costName = costName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getAllocationRuleId() {
        return allocationRuleId;
    }

    public void setAllocationRuleId(Long allocationRuleId) {
        this.allocationRuleId = allocationRuleId;
    }

    public String getAllocationRuleName() {
        return allocationRuleName;
    }

    public void setAllocationRuleName(String allocationRuleName) {
        this.allocationRuleName = allocationRuleName;
    }

    public String getAllocationMethod() {
        return allocationMethod;
    }

    public void setAllocationMethod(String allocationMethod) {
        this.allocationMethod = allocationMethod;
    }

    public String getAllocationPeriod() {
        return allocationPeriod;
    }

    public void setAllocationPeriod(String allocationPeriod) {
        this.allocationPeriod = allocationPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public CostAllocation() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CostAllocation)) return false;
        final CostAllocation other = (CostAllocation) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$financeRecordId = this.getFinanceRecordId();
        final java.lang.Object other$financeRecordId = other.getFinanceRecordId();
        if (this$financeRecordId == null ? other$financeRecordId != null : !this$financeRecordId.equals(other$financeRecordId)) return false;
        final java.lang.Object this$allocationRuleId = this.getAllocationRuleId();
        final java.lang.Object other$allocationRuleId = other.getAllocationRuleId();
        if (this$allocationRuleId == null ? other$allocationRuleId != null : !this$allocationRuleId.equals(other$allocationRuleId)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$costName = this.getCostName();
        final java.lang.Object other$costName = other.getCostName();
        if (this$costName == null ? other$costName != null : !this$costName.equals(other$costName)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$allocationRuleName = this.getAllocationRuleName();
        final java.lang.Object other$allocationRuleName = other.getAllocationRuleName();
        if (this$allocationRuleName == null ? other$allocationRuleName != null : !this$allocationRuleName.equals(other$allocationRuleName)) return false;
        final java.lang.Object this$allocationMethod = this.getAllocationMethod();
        final java.lang.Object other$allocationMethod = other.getAllocationMethod();
        if (this$allocationMethod == null ? other$allocationMethod != null : !this$allocationMethod.equals(other$allocationMethod)) return false;
        final java.lang.Object this$allocationPeriod = this.getAllocationPeriod();
        final java.lang.Object other$allocationPeriod = other.getAllocationPeriod();
        if (this$allocationPeriod == null ? other$allocationPeriod != null : !this$allocationPeriod.equals(other$allocationPeriod)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
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
        return other instanceof CostAllocation;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $financeRecordId = this.getFinanceRecordId();
        result = result * PRIME + ($financeRecordId == null ? 43 : $financeRecordId.hashCode());
        final java.lang.Object $allocationRuleId = this.getAllocationRuleId();
        result = result * PRIME + ($allocationRuleId == null ? 43 : $allocationRuleId.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $costName = this.getCostName();
        result = result * PRIME + ($costName == null ? 43 : $costName.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $allocationRuleName = this.getAllocationRuleName();
        result = result * PRIME + ($allocationRuleName == null ? 43 : $allocationRuleName.hashCode());
        final java.lang.Object $allocationMethod = this.getAllocationMethod();
        result = result * PRIME + ($allocationMethod == null ? 43 : $allocationMethod.hashCode());
        final java.lang.Object $allocationPeriod = this.getAllocationPeriod();
        result = result * PRIME + ($allocationPeriod == null ? 43 : $allocationPeriod.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
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
        return "CostAllocation(id=" + this.getId() + ", financeRecordId=" + this.getFinanceRecordId() + ", costName=" + this.getCostName() + ", totalAmount=" + this.getTotalAmount() + ", allocationRuleId=" + this.getAllocationRuleId() + ", allocationRuleName=" + this.getAllocationRuleName() + ", allocationMethod=" + this.getAllocationMethod() + ", allocationPeriod=" + this.getAllocationPeriod() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ")";
    }
}
