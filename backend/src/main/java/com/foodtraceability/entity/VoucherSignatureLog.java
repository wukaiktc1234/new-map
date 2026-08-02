package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 电子凭证验签记录实体类
 */
@TableName("voucher_signature_log")
@Schema(description = "电子凭证验签记录实体")
public class VoucherSignatureLog {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private Long tenantId;
    @TableField("voucher_id")
    @Schema(description = "电子凭证ID")
    private Long voucherId;
    @TableField("sign_type")
    @Schema(description = "签名类型：RSA/SM2/ECDSA等")
    private String signType;
    @TableField("sign_algorithm")
    @Schema(description = "签名算法")
    private String signAlgorithm;
    @TableField("signer")
    @Schema(description = "签名者")
    private String signer;
    @TableField("signer_cert_no")
    @Schema(description = "签名者证书编号")
    private String signerCertNo;
    @TableField("sign_time")
    @Schema(description = "签名时间")
    private LocalDateTime signTime;
    @TableField("sign_original")
    @Schema(description = "签名原文")
    private String signOriginal;
    @TableField("sign_value")
    @Schema(description = "签名值")
    private String signValue;
    @TableField("sign_result")
    @Schema(description = "验签结果：1-成功，2-失败")
    private Integer signResult;
    @TableField("verify_time")
    @Schema(description = "验签时间")
    private LocalDateTime verifyTime;
    @TableField("certificate_info")
    @Schema(description = "证书信息JSON")
    private String certificateInfo;
    @TableField("certificate_issuer")
    @Schema(description = "证书颁发者")
    private String certificateIssuer;
    @TableField("certificate_valid_from")
    @Schema(description = "证书有效期起始")
    private LocalDateTime certificateValidFrom;
    @TableField("certificate_valid_to")
    @Schema(description = "证书有效期截止")
    private LocalDateTime certificateValidTo;
    @TableField("timestamp_token")
    @Schema(description = "时间戳令牌")
    private String timestampToken;
    @TableField("timestamp_authority")
    @Schema(description = "时间戳服务机构")
    private String timestampAuthority;
    @TableField("error_code")
    @Schema(description = "错误代码")
    private String errorCode;
    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    private Integer deleted;

    public VoucherSignatureLog() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getVoucherId() {
        return this.voucherId;
    }

    public String getSignType() {
        return this.signType;
    }

    public String getSignAlgorithm() {
        return this.signAlgorithm;
    }

    public String getSigner() {
        return this.signer;
    }

    public String getSignerCertNo() {
        return this.signerCertNo;
    }

    public LocalDateTime getSignTime() {
        return this.signTime;
    }

    public String getSignOriginal() {
        return this.signOriginal;
    }

    public String getSignValue() {
        return this.signValue;
    }

    public Integer getSignResult() {
        return this.signResult;
    }

    public LocalDateTime getVerifyTime() {
        return this.verifyTime;
    }

    public String getCertificateInfo() {
        return this.certificateInfo;
    }

    public String getCertificateIssuer() {
        return this.certificateIssuer;
    }

    public LocalDateTime getCertificateValidFrom() {
        return this.certificateValidFrom;
    }

    public LocalDateTime getCertificateValidTo() {
        return this.certificateValidTo;
    }

    public String getTimestampToken() {
        return this.timestampToken;
    }

    public String getTimestampAuthority() {
        return this.timestampAuthority;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    public void setSignType(final String signType) {
        this.signType = signType;
    }

    public void setSignAlgorithm(final String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public void setSigner(final String signer) {
        this.signer = signer;
    }

    public void setSignerCertNo(final String signerCertNo) {
        this.signerCertNo = signerCertNo;
    }

    public void setSignTime(final LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public void setSignOriginal(final String signOriginal) {
        this.signOriginal = signOriginal;
    }

    public void setSignValue(final String signValue) {
        this.signValue = signValue;
    }

    public void setSignResult(final Integer signResult) {
        this.signResult = signResult;
    }

    public void setVerifyTime(final LocalDateTime verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setCertificateInfo(final String certificateInfo) {
        this.certificateInfo = certificateInfo;
    }

    public void setCertificateIssuer(final String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public void setCertificateValidFrom(final LocalDateTime certificateValidFrom) {
        this.certificateValidFrom = certificateValidFrom;
    }

    public void setCertificateValidTo(final LocalDateTime certificateValidTo) {
        this.certificateValidTo = certificateValidTo;
    }

    public void setTimestampToken(final String timestampToken) {
        this.timestampToken = timestampToken;
    }

    public void setTimestampAuthority(final String timestampAuthority) {
        this.timestampAuthority = timestampAuthority;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherSignatureLog)) return false;
        final VoucherSignatureLog other = (VoucherSignatureLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$signResult = this.getSignResult();
        final java.lang.Object other$signResult = other.getSignResult();
        if (this$signResult == null ? other$signResult != null : !this$signResult.equals(other$signResult)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$signType = this.getSignType();
        final java.lang.Object other$signType = other.getSignType();
        if (this$signType == null ? other$signType != null : !this$signType.equals(other$signType)) return false;
        final java.lang.Object this$signAlgorithm = this.getSignAlgorithm();
        final java.lang.Object other$signAlgorithm = other.getSignAlgorithm();
        if (this$signAlgorithm == null ? other$signAlgorithm != null : !this$signAlgorithm.equals(other$signAlgorithm)) return false;
        final java.lang.Object this$signer = this.getSigner();
        final java.lang.Object other$signer = other.getSigner();
        if (this$signer == null ? other$signer != null : !this$signer.equals(other$signer)) return false;
        final java.lang.Object this$signerCertNo = this.getSignerCertNo();
        final java.lang.Object other$signerCertNo = other.getSignerCertNo();
        if (this$signerCertNo == null ? other$signerCertNo != null : !this$signerCertNo.equals(other$signerCertNo)) return false;
        final java.lang.Object this$signTime = this.getSignTime();
        final java.lang.Object other$signTime = other.getSignTime();
        if (this$signTime == null ? other$signTime != null : !this$signTime.equals(other$signTime)) return false;
        final java.lang.Object this$signOriginal = this.getSignOriginal();
        final java.lang.Object other$signOriginal = other.getSignOriginal();
        if (this$signOriginal == null ? other$signOriginal != null : !this$signOriginal.equals(other$signOriginal)) return false;
        final java.lang.Object this$signValue = this.getSignValue();
        final java.lang.Object other$signValue = other.getSignValue();
        if (this$signValue == null ? other$signValue != null : !this$signValue.equals(other$signValue)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$certificateInfo = this.getCertificateInfo();
        final java.lang.Object other$certificateInfo = other.getCertificateInfo();
        if (this$certificateInfo == null ? other$certificateInfo != null : !this$certificateInfo.equals(other$certificateInfo)) return false;
        final java.lang.Object this$certificateIssuer = this.getCertificateIssuer();
        final java.lang.Object other$certificateIssuer = other.getCertificateIssuer();
        if (this$certificateIssuer == null ? other$certificateIssuer != null : !this$certificateIssuer.equals(other$certificateIssuer)) return false;
        final java.lang.Object this$certificateValidFrom = this.getCertificateValidFrom();
        final java.lang.Object other$certificateValidFrom = other.getCertificateValidFrom();
        if (this$certificateValidFrom == null ? other$certificateValidFrom != null : !this$certificateValidFrom.equals(other$certificateValidFrom)) return false;
        final java.lang.Object this$certificateValidTo = this.getCertificateValidTo();
        final java.lang.Object other$certificateValidTo = other.getCertificateValidTo();
        if (this$certificateValidTo == null ? other$certificateValidTo != null : !this$certificateValidTo.equals(other$certificateValidTo)) return false;
        final java.lang.Object this$timestampToken = this.getTimestampToken();
        final java.lang.Object other$timestampToken = other.getTimestampToken();
        if (this$timestampToken == null ? other$timestampToken != null : !this$timestampToken.equals(other$timestampToken)) return false;
        final java.lang.Object this$timestampAuthority = this.getTimestampAuthority();
        final java.lang.Object other$timestampAuthority = other.getTimestampAuthority();
        if (this$timestampAuthority == null ? other$timestampAuthority != null : !this$timestampAuthority.equals(other$timestampAuthority)) return false;
        final java.lang.Object this$errorCode = this.getErrorCode();
        final java.lang.Object other$errorCode = other.getErrorCode();
        if (this$errorCode == null ? other$errorCode != null : !this$errorCode.equals(other$errorCode)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherSignatureLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $signResult = this.getSignResult();
        result = result * PRIME + ($signResult == null ? 43 : $signResult.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $signType = this.getSignType();
        result = result * PRIME + ($signType == null ? 43 : $signType.hashCode());
        final java.lang.Object $signAlgorithm = this.getSignAlgorithm();
        result = result * PRIME + ($signAlgorithm == null ? 43 : $signAlgorithm.hashCode());
        final java.lang.Object $signer = this.getSigner();
        result = result * PRIME + ($signer == null ? 43 : $signer.hashCode());
        final java.lang.Object $signerCertNo = this.getSignerCertNo();
        result = result * PRIME + ($signerCertNo == null ? 43 : $signerCertNo.hashCode());
        final java.lang.Object $signTime = this.getSignTime();
        result = result * PRIME + ($signTime == null ? 43 : $signTime.hashCode());
        final java.lang.Object $signOriginal = this.getSignOriginal();
        result = result * PRIME + ($signOriginal == null ? 43 : $signOriginal.hashCode());
        final java.lang.Object $signValue = this.getSignValue();
        result = result * PRIME + ($signValue == null ? 43 : $signValue.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $certificateInfo = this.getCertificateInfo();
        result = result * PRIME + ($certificateInfo == null ? 43 : $certificateInfo.hashCode());
        final java.lang.Object $certificateIssuer = this.getCertificateIssuer();
        result = result * PRIME + ($certificateIssuer == null ? 43 : $certificateIssuer.hashCode());
        final java.lang.Object $certificateValidFrom = this.getCertificateValidFrom();
        result = result * PRIME + ($certificateValidFrom == null ? 43 : $certificateValidFrom.hashCode());
        final java.lang.Object $certificateValidTo = this.getCertificateValidTo();
        result = result * PRIME + ($certificateValidTo == null ? 43 : $certificateValidTo.hashCode());
        final java.lang.Object $timestampToken = this.getTimestampToken();
        result = result * PRIME + ($timestampToken == null ? 43 : $timestampToken.hashCode());
        final java.lang.Object $timestampAuthority = this.getTimestampAuthority();
        result = result * PRIME + ($timestampAuthority == null ? 43 : $timestampAuthority.hashCode());
        final java.lang.Object $errorCode = this.getErrorCode();
        result = result * PRIME + ($errorCode == null ? 43 : $errorCode.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherSignatureLog(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", voucherId=" + this.getVoucherId() + ", signType=" + this.getSignType() + ", signAlgorithm=" + this.getSignAlgorithm() + ", signer=" + this.getSigner() + ", signerCertNo=" + this.getSignerCertNo() + ", signTime=" + this.getSignTime() + ", signOriginal=" + this.getSignOriginal() + ", signValue=" + this.getSignValue() + ", signResult=" + this.getSignResult() + ", verifyTime=" + this.getVerifyTime() + ", certificateInfo=" + this.getCertificateInfo() + ", certificateIssuer=" + this.getCertificateIssuer() + ", certificateValidFrom=" + this.getCertificateValidFrom() + ", certificateValidTo=" + this.getCertificateValidTo() + ", timestampToken=" + this.getTimestampToken() + ", timestampAuthority=" + this.getTimestampAuthority() + ", errorCode=" + this.getErrorCode() + ", errorMessage=" + this.getErrorMessage() + ", createdAt=" + this.getCreatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
