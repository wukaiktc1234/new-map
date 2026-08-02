package com.foodtraceability.dto;

/**
 * 费用分摊查询DTO
 * 用于封装费用分摊的查询条件
 */
public class CostAllocationQueryDTO {
    /**
     * 费用名称
     */
    private String costName;
    /**
     * 分摊方式
     */
    private String allocationMethod;
    /**
     * 分摊期间
     */
    private String allocationPeriod;
    /**
     * 分摊状态
     */
    private String status;
    /**
     * 规则名称
     */
    private String ruleName;
    /**
     * 页码
     */
    private Integer pageNum = 1;
    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    // Getter and Setter methods
    public String getCostName() {
        return costName;
    }

    public void setCostName(String costName) {
        this.costName = costName;
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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public CostAllocationQueryDTO() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CostAllocationQueryDTO)) return false;
        final CostAllocationQueryDTO other = (CostAllocationQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$pageNum = this.getPageNum();
        final java.lang.Object other$pageNum = other.getPageNum();
        if (this$pageNum == null ? other$pageNum != null : !this$pageNum.equals(other$pageNum)) return false;
        final java.lang.Object this$pageSize = this.getPageSize();
        final java.lang.Object other$pageSize = other.getPageSize();
        if (this$pageSize == null ? other$pageSize != null : !this$pageSize.equals(other$pageSize)) return false;
        final java.lang.Object this$costName = this.getCostName();
        final java.lang.Object other$costName = other.getCostName();
        if (this$costName == null ? other$costName != null : !this$costName.equals(other$costName)) return false;
        final java.lang.Object this$allocationMethod = this.getAllocationMethod();
        final java.lang.Object other$allocationMethod = other.getAllocationMethod();
        if (this$allocationMethod == null ? other$allocationMethod != null : !this$allocationMethod.equals(other$allocationMethod)) return false;
        final java.lang.Object this$allocationPeriod = this.getAllocationPeriod();
        final java.lang.Object other$allocationPeriod = other.getAllocationPeriod();
        if (this$allocationPeriod == null ? other$allocationPeriod != null : !this$allocationPeriod.equals(other$allocationPeriod)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$ruleName = this.getRuleName();
        final java.lang.Object other$ruleName = other.getRuleName();
        if (this$ruleName == null ? other$ruleName != null : !this$ruleName.equals(other$ruleName)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CostAllocationQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $pageNum = this.getPageNum();
        result = result * PRIME + ($pageNum == null ? 43 : $pageNum.hashCode());
        final java.lang.Object $pageSize = this.getPageSize();
        result = result * PRIME + ($pageSize == null ? 43 : $pageSize.hashCode());
        final java.lang.Object $costName = this.getCostName();
        result = result * PRIME + ($costName == null ? 43 : $costName.hashCode());
        final java.lang.Object $allocationMethod = this.getAllocationMethod();
        result = result * PRIME + ($allocationMethod == null ? 43 : $allocationMethod.hashCode());
        final java.lang.Object $allocationPeriod = this.getAllocationPeriod();
        result = result * PRIME + ($allocationPeriod == null ? 43 : $allocationPeriod.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $ruleName = this.getRuleName();
        result = result * PRIME + ($ruleName == null ? 43 : $ruleName.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CostAllocationQueryDTO(costName=" + this.getCostName() + ", allocationMethod=" + this.getAllocationMethod() + ", allocationPeriod=" + this.getAllocationPeriod() + ", status=" + this.getStatus() + ", ruleName=" + this.getRuleName() + ", pageNum=" + this.getPageNum() + ", pageSize=" + this.getPageSize() + ")";
    }
}
