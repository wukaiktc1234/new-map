package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 费用分摊明细实体类
 * 用于记录费用分摊的具体明细信息
 */
@TableName("cost_allocation_detail")
public class CostAllocationDetail {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 费用分摊ID
     */
    private Long allocationId;
    /**
     * 成本中心ID
     */
    private Long costCenterId;
    /**
     * 成本中心名称
     */
    private String costCenterName;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 分摊金额
     */
    private BigDecimal allocationAmount;
    /**
     * 分摊比例
     */
    private BigDecimal allocationPercentage;
    /**
     * 分摊依据
     */
    private String allocationBasis;
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

    public CostAllocationDetail() {
    }

    /**
     * 主键ID
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 费用分摊ID
     */
    public Long getAllocationId() {
        return this.allocationId;
    }

    /**
     * 成本中心ID
     */
    public Long getCostCenterId() {
        return this.costCenterId;
    }

    /**
     * 成本中心名称
     */
    public String getCostCenterName() {
        return this.costCenterName;
    }

    /**
     * 产品ID
     */
    public Long getProductId() {
        return this.productId;
    }

    /**
     * 产品名称
     */
    public String getProductName() {
        return this.productName;
    }

    /**
     * 分摊金额
     */
    public BigDecimal getAllocationAmount() {
        return this.allocationAmount;
    }

    /**
     * 分摊比例
     */
    public BigDecimal getAllocationPercentage() {
        return this.allocationPercentage;
    }

    /**
     * 分摊依据
     */
    public String getAllocationBasis() {
        return this.allocationBasis;
    }

    /**
     * 备注
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 创建时间
     */
    public Date getCreateTime() {
        return this.createTime;
    }

    /**
     * 更新时间
     */
    public Date getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 主键ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 费用分摊ID
     */
    public void setAllocationId(final Long allocationId) {
        this.allocationId = allocationId;
    }

    /**
     * 成本中心ID
     */
    public void setCostCenterId(final Long costCenterId) {
        this.costCenterId = costCenterId;
    }

    /**
     * 成本中心名称
     */
    public void setCostCenterName(final String costCenterName) {
        this.costCenterName = costCenterName;
    }

    /**
     * 产品ID
     */
    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    /**
     * 产品名称
     */
    public void setProductName(final String productName) {
        this.productName = productName;
    }

    /**
     * 分摊金额
     */
    public void setAllocationAmount(final BigDecimal allocationAmount) {
        this.allocationAmount = allocationAmount;
    }

    /**
     * 分摊比例
     */
    public void setAllocationPercentage(final BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    /**
     * 分摊依据
     */
    public void setAllocationBasis(final String allocationBasis) {
        this.allocationBasis = allocationBasis;
    }

    /**
     * 备注
     */
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    /**
     * 创建时间
     */
    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新时间
     */
    public void setUpdateTime(final Date updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CostAllocationDetail)) return false;
        final CostAllocationDetail other = (CostAllocationDetail) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$allocationId = this.getAllocationId();
        final java.lang.Object other$allocationId = other.getAllocationId();
        if (this$allocationId == null ? other$allocationId != null : !this$allocationId.equals(other$allocationId)) return false;
        final java.lang.Object this$costCenterId = this.getCostCenterId();
        final java.lang.Object other$costCenterId = other.getCostCenterId();
        if (this$costCenterId == null ? other$costCenterId != null : !this$costCenterId.equals(other$costCenterId)) return false;
        final java.lang.Object this$productId = this.getProductId();
        final java.lang.Object other$productId = other.getProductId();
        if (this$productId == null ? other$productId != null : !this$productId.equals(other$productId)) return false;
        final java.lang.Object this$costCenterName = this.getCostCenterName();
        final java.lang.Object other$costCenterName = other.getCostCenterName();
        if (this$costCenterName == null ? other$costCenterName != null : !this$costCenterName.equals(other$costCenterName)) return false;
        final java.lang.Object this$productName = this.getProductName();
        final java.lang.Object other$productName = other.getProductName();
        if (this$productName == null ? other$productName != null : !this$productName.equals(other$productName)) return false;
        final java.lang.Object this$allocationAmount = this.getAllocationAmount();
        final java.lang.Object other$allocationAmount = other.getAllocationAmount();
        if (this$allocationAmount == null ? other$allocationAmount != null : !this$allocationAmount.equals(other$allocationAmount)) return false;
        final java.lang.Object this$allocationPercentage = this.getAllocationPercentage();
        final java.lang.Object other$allocationPercentage = other.getAllocationPercentage();
        if (this$allocationPercentage == null ? other$allocationPercentage != null : !this$allocationPercentage.equals(other$allocationPercentage)) return false;
        final java.lang.Object this$allocationBasis = this.getAllocationBasis();
        final java.lang.Object other$allocationBasis = other.getAllocationBasis();
        if (this$allocationBasis == null ? other$allocationBasis != null : !this$allocationBasis.equals(other$allocationBasis)) return false;
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
        return other instanceof CostAllocationDetail;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $allocationId = this.getAllocationId();
        result = result * PRIME + ($allocationId == null ? 43 : $allocationId.hashCode());
        final java.lang.Object $costCenterId = this.getCostCenterId();
        result = result * PRIME + ($costCenterId == null ? 43 : $costCenterId.hashCode());
        final java.lang.Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final java.lang.Object $costCenterName = this.getCostCenterName();
        result = result * PRIME + ($costCenterName == null ? 43 : $costCenterName.hashCode());
        final java.lang.Object $productName = this.getProductName();
        result = result * PRIME + ($productName == null ? 43 : $productName.hashCode());
        final java.lang.Object $allocationAmount = this.getAllocationAmount();
        result = result * PRIME + ($allocationAmount == null ? 43 : $allocationAmount.hashCode());
        final java.lang.Object $allocationPercentage = this.getAllocationPercentage();
        result = result * PRIME + ($allocationPercentage == null ? 43 : $allocationPercentage.hashCode());
        final java.lang.Object $allocationBasis = this.getAllocationBasis();
        result = result * PRIME + ($allocationBasis == null ? 43 : $allocationBasis.hashCode());
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
        return "CostAllocationDetail(id=" + this.getId() + ", allocationId=" + this.getAllocationId() + ", costCenterId=" + this.getCostCenterId() + ", costCenterName=" + this.getCostCenterName() + ", productId=" + this.getProductId() + ", productName=" + this.getProductName() + ", allocationAmount=" + this.getAllocationAmount() + ", allocationPercentage=" + this.getAllocationPercentage() + ", allocationBasis=" + this.getAllocationBasis() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
