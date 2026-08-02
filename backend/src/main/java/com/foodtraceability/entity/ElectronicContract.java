package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("electronic_contract")
public class ElectronicContract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contractNo;
    private String contractName;
    private Long supplierId;
    private String supplierName;
    private String contractType;
    private String status;
    private String signUrl;
    /** 签署使用的印章ID（关联 seals.seal_id） */
    private String sealId;
    private String viewUrl;
    private LocalDateTime signDate;
    private LocalDateTime expireDate;
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

    public ElectronicContract() {
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

    public String getStatus() {
        return this.status;
    }

    public String getSignUrl() {
        return this.signUrl;
    }

    public String getSealId() {
        return this.sealId;
    }

    public String getViewUrl() {
        return this.viewUrl;
    }

    public LocalDateTime getSignDate() {
        return this.signDate;
    }

    public LocalDateTime getExpireDate() {
        return this.expireDate;
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

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setSignUrl(final String signUrl) {
        this.signUrl = signUrl;
    }

    public void setSealId(final String sealId) {
        this.sealId = sealId;
    }

    public void setViewUrl(final String viewUrl) {
        this.viewUrl = viewUrl;
    }

    public void setSignDate(final LocalDateTime signDate) {
        this.signDate = signDate;
    }

    public void setExpireDate(final LocalDateTime expireDate) {
        this.expireDate = expireDate;
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
        if (!(o instanceof ElectronicContract)) return false;
        final ElectronicContract other = (ElectronicContract) o;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$signUrl = this.getSignUrl();
        final java.lang.Object other$signUrl = other.getSignUrl();
        if (this$signUrl == null ? other$signUrl != null : !this$signUrl.equals(other$signUrl)) return false;
        final java.lang.Object this$sealId = this.getSealId();
        final java.lang.Object other$sealId = other.getSealId();
        if (this$sealId == null ? other$sealId != null : !this$sealId.equals(other$sealId)) return false;
        final java.lang.Object this$viewUrl = this.getViewUrl();
        final java.lang.Object other$viewUrl = other.getViewUrl();
        if (this$viewUrl == null ? other$viewUrl != null : !this$viewUrl.equals(other$viewUrl)) return false;
        final java.lang.Object this$signDate = this.getSignDate();
        final java.lang.Object other$signDate = other.getSignDate();
        if (this$signDate == null ? other$signDate != null : !this$signDate.equals(other$signDate)) return false;
        final java.lang.Object this$expireDate = this.getExpireDate();
        final java.lang.Object other$expireDate = other.getExpireDate();
        if (this$expireDate == null ? other$expireDate != null : !this$expireDate.equals(other$expireDate)) return false;
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
        return other instanceof ElectronicContract;
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
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $signUrl = this.getSignUrl();
        result = result * PRIME + ($signUrl == null ? 43 : $signUrl.hashCode());
        final java.lang.Object $sealId = this.getSealId();
        result = result * PRIME + ($sealId == null ? 43 : $sealId.hashCode());
        final java.lang.Object $viewUrl = this.getViewUrl();
        result = result * PRIME + ($viewUrl == null ? 43 : $viewUrl.hashCode());
        final java.lang.Object $signDate = this.getSignDate();
        result = result * PRIME + ($signDate == null ? 43 : $signDate.hashCode());
        final java.lang.Object $expireDate = this.getExpireDate();
        result = result * PRIME + ($expireDate == null ? 43 : $expireDate.hashCode());
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
        return "ElectronicContract(id=" + this.getId() + ", contractNo=" + this.getContractNo() + ", contractName=" + this.getContractName() + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", contractType=" + this.getContractType() + ", status=" + this.getStatus() + ", signUrl=" + this.getSignUrl() + ", sealId=" + this.getSealId() + ", viewUrl=" + this.getViewUrl() + ", signDate=" + this.getSignDate() + ", expireDate=" + this.getExpireDate() + ", remark=" + this.getRemark() + ", createdBy=" + this.getCreatedBy() + ", createTime=" + this.getCreateTime() + ", updatedBy=" + this.getUpdatedBy() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
