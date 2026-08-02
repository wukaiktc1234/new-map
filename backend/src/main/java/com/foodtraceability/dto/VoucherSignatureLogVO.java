package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验签记录VO
 */
@Schema(description = "验签记录VO")
public class VoucherSignatureLogVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "签名类型")
    private String signType;
    @Schema(description = "签名算法")
    private String signAlgorithm;
    @Schema(description = "签名者")
    private String signer;
    @Schema(description = "签名者证书编号")
    private String signerCertNo;
    @Schema(description = "签名时间")
    private String signTime;
    @Schema(description = "验签结果")
    private Integer signResult;
    @Schema(description = "验签结果名称")
    private String signResultName;
    @Schema(description = "验签时间")
    private String verifyTime;
    @Schema(description = "证书颁发者")
    private String certificateIssuer;
    @Schema(description = "证书有效期起始")
    private String certificateValidFrom;
    @Schema(description = "证书有效期截止")
    private String certificateValidTo;
    @Schema(description = "时间戳服务机构")
    private String timestampAuthority;
    @Schema(description = "错误代码")
    private String errorCode;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "创建时间")
    private String createdAt;

    public VoucherSignatureLogVO() {
    }

    public Long getId() {
        return this.id;
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

    public String getSignTime() {
        return this.signTime;
    }

    public Integer getSignResult() {
        return this.signResult;
    }

    public String getSignResultName() {
        return this.signResultName;
    }

    public String getVerifyTime() {
        return this.verifyTime;
    }

    public String getCertificateIssuer() {
        return this.certificateIssuer;
    }

    public String getCertificateValidFrom() {
        return this.certificateValidFrom;
    }

    public String getCertificateValidTo() {
        return this.certificateValidTo;
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

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public void setSignTime(final String signTime) {
        this.signTime = signTime;
    }

    public void setSignResult(final Integer signResult) {
        this.signResult = signResult;
    }

    public void setSignResultName(final String signResultName) {
        this.signResultName = signResultName;
    }

    public void setVerifyTime(final String verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setCertificateIssuer(final String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public void setCertificateValidFrom(final String certificateValidFrom) {
        this.certificateValidFrom = certificateValidFrom;
    }

    public void setCertificateValidTo(final String certificateValidTo) {
        this.certificateValidTo = certificateValidTo;
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

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherSignatureLogVO)) return false;
        final VoucherSignatureLogVO other = (VoucherSignatureLogVO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$signResult = this.getSignResult();
        final java.lang.Object other$signResult = other.getSignResult();
        if (this$signResult == null ? other$signResult != null : !this$signResult.equals(other$signResult)) return false;
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
        final java.lang.Object this$signResultName = this.getSignResultName();
        final java.lang.Object other$signResultName = other.getSignResultName();
        if (this$signResultName == null ? other$signResultName != null : !this$signResultName.equals(other$signResultName)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$certificateIssuer = this.getCertificateIssuer();
        final java.lang.Object other$certificateIssuer = other.getCertificateIssuer();
        if (this$certificateIssuer == null ? other$certificateIssuer != null : !this$certificateIssuer.equals(other$certificateIssuer)) return false;
        final java.lang.Object this$certificateValidFrom = this.getCertificateValidFrom();
        final java.lang.Object other$certificateValidFrom = other.getCertificateValidFrom();
        if (this$certificateValidFrom == null ? other$certificateValidFrom != null : !this$certificateValidFrom.equals(other$certificateValidFrom)) return false;
        final java.lang.Object this$certificateValidTo = this.getCertificateValidTo();
        final java.lang.Object other$certificateValidTo = other.getCertificateValidTo();
        if (this$certificateValidTo == null ? other$certificateValidTo != null : !this$certificateValidTo.equals(other$certificateValidTo)) return false;
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
        return other instanceof VoucherSignatureLogVO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $signResult = this.getSignResult();
        result = result * PRIME + ($signResult == null ? 43 : $signResult.hashCode());
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
        final java.lang.Object $signResultName = this.getSignResultName();
        result = result * PRIME + ($signResultName == null ? 43 : $signResultName.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $certificateIssuer = this.getCertificateIssuer();
        result = result * PRIME + ($certificateIssuer == null ? 43 : $certificateIssuer.hashCode());
        final java.lang.Object $certificateValidFrom = this.getCertificateValidFrom();
        result = result * PRIME + ($certificateValidFrom == null ? 43 : $certificateValidFrom.hashCode());
        final java.lang.Object $certificateValidTo = this.getCertificateValidTo();
        result = result * PRIME + ($certificateValidTo == null ? 43 : $certificateValidTo.hashCode());
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
        return "VoucherSignatureLogVO(id=" + this.getId() + ", signType=" + this.getSignType() + ", signAlgorithm=" + this.getSignAlgorithm() + ", signer=" + this.getSigner() + ", signerCertNo=" + this.getSignerCertNo() + ", signTime=" + this.getSignTime() + ", signResult=" + this.getSignResult() + ", signResultName=" + this.getSignResultName() + ", verifyTime=" + this.getVerifyTime() + ", certificateIssuer=" + this.getCertificateIssuer() + ", certificateValidFrom=" + this.getCertificateValidFrom() + ", certificateValidTo=" + this.getCertificateValidTo() + ", timestampAuthority=" + this.getTimestampAuthority() + ", errorCode=" + this.getErrorCode() + ", errorMessage=" + this.getErrorMessage() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
