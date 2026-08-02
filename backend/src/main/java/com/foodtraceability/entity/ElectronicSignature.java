package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 电子签名记录实体类
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@TableName("electronic_signature")
@Schema(description = "电子签名记录实体")
public class ElectronicSignature {
    @TableId(type = IdType.AUTO)
    @Schema(description = "签名ID")
    private Long id;
    @Schema(description = "关联合同ID")
    private Long contractId;
    @Schema(description = "签署人类型: employee-员工, company-公司")
    private String signerType;
    @Schema(description = "签署人ID")
    private String signerId;
    @Schema(description = "签署人姓名")
    private String signerName;
    @Schema(description = "签名数据（Base64）")
    private String signatureData;
    @Schema(description = "签名图片URL")
    private String signatureImageUrl;
    @Schema(description = "签署时间")
    private LocalDateTime signTime;
    @Schema(description = "签署IP地址")
    private String signIp;
    @Schema(description = "签署设备信息")
    private String signDevice;
    @Schema(description = "验证码")
    private String verifyCode;
    @Schema(description = "验证时间")
    private LocalDateTime verifyTime;
    @Schema(description = "状态: pending-待签, signed-已签, rejected-已拒绝")
    private String status;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    public static final String SIGNER_EMPLOYEE = "employee";
    public static final String SIGNER_COMPANY = "company";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SIGNED = "signed";
    public static final String STATUS_REJECTED = "rejected";

    public ElectronicSignature() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getContractId() {
        return this.contractId;
    }

    public String getSignerType() {
        return this.signerType;
    }

    public String getSignerId() {
        return this.signerId;
    }

    public String getSignerName() {
        return this.signerName;
    }

    public String getSignatureData() {
        return this.signatureData;
    }

    public String getSignatureImageUrl() {
        return this.signatureImageUrl;
    }

    public LocalDateTime getSignTime() {
        return this.signTime;
    }

    public String getSignIp() {
        return this.signIp;
    }

    public String getSignDevice() {
        return this.signDevice;
    }

    public String getVerifyCode() {
        return this.verifyCode;
    }

    public LocalDateTime getVerifyTime() {
        return this.verifyTime;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
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

    public void setContractId(final Long contractId) {
        this.contractId = contractId;
    }

    public void setSignerType(final String signerType) {
        this.signerType = signerType;
    }

    public void setSignerId(final String signerId) {
        this.signerId = signerId;
    }

    public void setSignerName(final String signerName) {
        this.signerName = signerName;
    }

    public void setSignatureData(final String signatureData) {
        this.signatureData = signatureData;
    }

    public void setSignatureImageUrl(final String signatureImageUrl) {
        this.signatureImageUrl = signatureImageUrl;
    }

    public void setSignTime(final LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public void setSignIp(final String signIp) {
        this.signIp = signIp;
    }

    public void setSignDevice(final String signDevice) {
        this.signDevice = signDevice;
    }

    public void setVerifyCode(final String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public void setVerifyTime(final LocalDateTime verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
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
        if (!(o instanceof ElectronicSignature)) return false;
        final ElectronicSignature other = (ElectronicSignature) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$contractId = this.getContractId();
        final java.lang.Object other$contractId = other.getContractId();
        if (this$contractId == null ? other$contractId != null : !this$contractId.equals(other$contractId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$signerType = this.getSignerType();
        final java.lang.Object other$signerType = other.getSignerType();
        if (this$signerType == null ? other$signerType != null : !this$signerType.equals(other$signerType)) return false;
        final java.lang.Object this$signerId = this.getSignerId();
        final java.lang.Object other$signerId = other.getSignerId();
        if (this$signerId == null ? other$signerId != null : !this$signerId.equals(other$signerId)) return false;
        final java.lang.Object this$signerName = this.getSignerName();
        final java.lang.Object other$signerName = other.getSignerName();
        if (this$signerName == null ? other$signerName != null : !this$signerName.equals(other$signerName)) return false;
        final java.lang.Object this$signatureData = this.getSignatureData();
        final java.lang.Object other$signatureData = other.getSignatureData();
        if (this$signatureData == null ? other$signatureData != null : !this$signatureData.equals(other$signatureData)) return false;
        final java.lang.Object this$signatureImageUrl = this.getSignatureImageUrl();
        final java.lang.Object other$signatureImageUrl = other.getSignatureImageUrl();
        if (this$signatureImageUrl == null ? other$signatureImageUrl != null : !this$signatureImageUrl.equals(other$signatureImageUrl)) return false;
        final java.lang.Object this$signTime = this.getSignTime();
        final java.lang.Object other$signTime = other.getSignTime();
        if (this$signTime == null ? other$signTime != null : !this$signTime.equals(other$signTime)) return false;
        final java.lang.Object this$signIp = this.getSignIp();
        final java.lang.Object other$signIp = other.getSignIp();
        if (this$signIp == null ? other$signIp != null : !this$signIp.equals(other$signIp)) return false;
        final java.lang.Object this$signDevice = this.getSignDevice();
        final java.lang.Object other$signDevice = other.getSignDevice();
        if (this$signDevice == null ? other$signDevice != null : !this$signDevice.equals(other$signDevice)) return false;
        final java.lang.Object this$verifyCode = this.getVerifyCode();
        final java.lang.Object other$verifyCode = other.getVerifyCode();
        if (this$verifyCode == null ? other$verifyCode != null : !this$verifyCode.equals(other$verifyCode)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicSignature;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $contractId = this.getContractId();
        result = result * PRIME + ($contractId == null ? 43 : $contractId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $signerType = this.getSignerType();
        result = result * PRIME + ($signerType == null ? 43 : $signerType.hashCode());
        final java.lang.Object $signerId = this.getSignerId();
        result = result * PRIME + ($signerId == null ? 43 : $signerId.hashCode());
        final java.lang.Object $signerName = this.getSignerName();
        result = result * PRIME + ($signerName == null ? 43 : $signerName.hashCode());
        final java.lang.Object $signatureData = this.getSignatureData();
        result = result * PRIME + ($signatureData == null ? 43 : $signatureData.hashCode());
        final java.lang.Object $signatureImageUrl = this.getSignatureImageUrl();
        result = result * PRIME + ($signatureImageUrl == null ? 43 : $signatureImageUrl.hashCode());
        final java.lang.Object $signTime = this.getSignTime();
        result = result * PRIME + ($signTime == null ? 43 : $signTime.hashCode());
        final java.lang.Object $signIp = this.getSignIp();
        result = result * PRIME + ($signIp == null ? 43 : $signIp.hashCode());
        final java.lang.Object $signDevice = this.getSignDevice();
        result = result * PRIME + ($signDevice == null ? 43 : $signDevice.hashCode());
        final java.lang.Object $verifyCode = this.getVerifyCode();
        result = result * PRIME + ($verifyCode == null ? 43 : $verifyCode.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicSignature(id=" + this.getId() + ", contractId=" + this.getContractId() + ", signerType=" + this.getSignerType() + ", signerId=" + this.getSignerId() + ", signerName=" + this.getSignerName() + ", signatureData=" + this.getSignatureData() + ", signatureImageUrl=" + this.getSignatureImageUrl() + ", signTime=" + this.getSignTime() + ", signIp=" + this.getSignIp() + ", signDevice=" + this.getSignDevice() + ", verifyCode=" + this.getVerifyCode() + ", verifyTime=" + this.getVerifyTime() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
