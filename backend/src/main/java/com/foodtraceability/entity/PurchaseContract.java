package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("purchase_contract")
public class PurchaseContract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private String contractName;
    private Long supplierId;
    private String supplierName;
    private String contractType;
    private BigDecimal contractAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String signatory;
    private LocalDateTime signDate;
    private String attachmentUrl;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    public PurchaseContract() {
    }

    public Long getId() {
        return this.id;
    }

    public String getContractNo() {
        return this.contractNo;
    }

    public String getContractName() {
        return this.contractName;
    }

    public Long getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getContractType() {
        return this.contractType;
    }

    public BigDecimal getContractAmount() {
        return this.contractAmount;
    }

    public LocalDateTime getStartDate() {
        return this.startDate;
    }

    public LocalDateTime getEndDate() {
        return this.endDate;
    }

    public String getStatus() {
        return this.status;
    }

    public String getSignatory() {
        return this.signatory;
    }

    public LocalDateTime getSignDate() {
        return this.signDate;
    }

    public String getAttachmentUrl() {
        return this.attachmentUrl;
    }

    public String getRemark() {
        return this.remark;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setContractNo(final String contractNo) {
        this.contractNo = contractNo;
    }

    public void setContractName(final String contractName) {
        this.contractName = contractName;
    }

    public void setSupplierId(final Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(final String supplierName) {
        this.supplierName = supplierName;
    }

    public void setContractType(final String contractType) {
        this.contractType = contractType;
    }

    public void setContractAmount(final BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public void setStartDate(final LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setSignatory(final String signatory) {
        this.signatory = signatory;
    }

    public void setSignDate(final LocalDateTime signDate) {
        this.signDate = signDate;
    }

    public void setAttachmentUrl(final String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdatedBy(final Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseContract)) return false;
        final PurchaseContract other = (PurchaseContract) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$supplierId = this.getSupplierId();
        final java.lang.Object other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$contractNo = this.getContractNo();
        final java.lang.Object other$contractNo = other.getContractNo();
        if (this$contractNo == null ? other$contractNo != null : !this$contractNo.equals(other$contractNo)) return false;
        final java.lang.Object this$contractName = this.getContractName();
        final java.lang.Object other$contractName = other.getContractName();
        if (this$contractName == null ? other$contractName != null : !this$contractName.equals(other$contractName)) return false;
        final java.lang.Object this$supplierName = this.getSupplierName();
        final java.lang.Object other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) return false;
        final java.lang.Object this$contractType = this.getContractType();
        final java.lang.Object other$contractType = other.getContractType();
        if (this$contractType == null ? other$contractType != null : !this$contractType.equals(other$contractType)) return false;
        final java.lang.Object this$contractAmount = this.getContractAmount();
        final java.lang.Object other$contractAmount = other.getContractAmount();
        if (this$contractAmount == null ? other$contractAmount != null : !this$contractAmount.equals(other$contractAmount)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$signatory = this.getSignatory();
        final java.lang.Object other$signatory = other.getSignatory();
        if (this$signatory == null ? other$signatory != null : !this$signatory.equals(other$signatory)) return false;
        final java.lang.Object this$signDate = this.getSignDate();
        final java.lang.Object other$signDate = other.getSignDate();
        if (this$signDate == null ? other$signDate != null : !this$signDate.equals(other$signDate)) return false;
        final java.lang.Object this$attachmentUrl = this.getAttachmentUrl();
        final java.lang.Object other$attachmentUrl = other.getAttachmentUrl();
        if (this$attachmentUrl == null ? other$attachmentUrl != null : !this$attachmentUrl.equals(other$attachmentUrl)) return false;
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
        return other instanceof PurchaseContract;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $supplierId = this.getSupplierId();
        result = result * PRIME + ($supplierId == null ? 43 : $supplierId.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $contractNo = this.getContractNo();
        result = result * PRIME + ($contractNo == null ? 43 : $contractNo.hashCode());
        final java.lang.Object $contractName = this.getContractName();
        result = result * PRIME + ($contractName == null ? 43 : $contractName.hashCode());
        final java.lang.Object $supplierName = this.getSupplierName();
        result = result * PRIME + ($supplierName == null ? 43 : $supplierName.hashCode());
        final java.lang.Object $contractType = this.getContractType();
        result = result * PRIME + ($contractType == null ? 43 : $contractType.hashCode());
        final java.lang.Object $contractAmount = this.getContractAmount();
        result = result * PRIME + ($contractAmount == null ? 43 : $contractAmount.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $signatory = this.getSignatory();
        result = result * PRIME + ($signatory == null ? 43 : $signatory.hashCode());
        final java.lang.Object $signDate = this.getSignDate();
        result = result * PRIME + ($signDate == null ? 43 : $signDate.hashCode());
        final java.lang.Object $attachmentUrl = this.getAttachmentUrl();
        result = result * PRIME + ($attachmentUrl == null ? 43 : $attachmentUrl.hashCode());
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
        return "PurchaseContract(id=" + this.getId() + ", contractNo=" + this.getContractNo() + ", contractName=" + this.getContractName() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", contractType=" + this.getContractType() + ", contractAmount=" + this.getContractAmount() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", status=" + this.getStatus() + ", signatory=" + this.getSignatory() + ", signDate=" + this.getSignDate() + ", attachmentUrl=" + this.getAttachmentUrl() + ", remark=" + this.getRemark() + ", createdBy=" + this.getCreatedBy() + ", createTime=" + this.getCreateTime() + ", updatedBy=" + this.getUpdatedBy() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
