package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验签结果DTO
 * 
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 符合GB/T 38540-2020《信息安全技术 安全电子签章密码技术规范》
 */
@Schema(description = "验签结果")
public class SignatureVerifyResultDTO {
    @Schema(description = "验签状态：0-未验签，1-验签通过，2-验签失败")
    private Integer status;
    @Schema(description = "状态描述")
    private String statusDescription;
    @Schema(description = "签名类型：XML-DSig、OFD签章、PKCS7")
    private String signType;
    @Schema(description = "签名算法：RSA-SHA256、SM2等")
    private String signAlgorithm;
    @Schema(description = "签名者")
    private String signer;
    @Schema(description = "签名时间")
    private String signTime;
    @Schema(description = "证书颁发者")
    private String certificateIssuer;
    @Schema(description = "证书序列号")
    private String certificateSerial;
    @Schema(description = "证书有效期起始")
    private String certificateValidFrom;
    @Schema(description = "证书有效期截止")
    private String certificateValidTo;
    @Schema(description = "时间戳服务机构")
    private String timestampAuthority;
    @Schema(description = "时间戳令牌")
    private String timestampToken;
    @Schema(description = "错误信息")
    private String errorMessage;

    public static SignatureVerifyResultDTO success() {
        SignatureVerifyResultDTO result = new SignatureVerifyResultDTO();
        result.setStatus(1);
        result.setStatusDescription("验签通过");
        return result;
    }

    public static SignatureVerifyResultDTO failed(String errorMessage) {
        SignatureVerifyResultDTO result = new SignatureVerifyResultDTO();
        result.setStatus(2);
        result.setStatusDescription("验签失败");
        result.setErrorMessage(errorMessage);
        return result;
    }

    public SignatureVerifyResultDTO() {
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getStatusDescription() {
        return this.statusDescription;
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

    public String getSignTime() {
        return this.signTime;
    }

    public String getCertificateIssuer() {
        return this.certificateIssuer;
    }

    public String getCertificateSerial() {
        return this.certificateSerial;
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

    public String getTimestampToken() {
        return this.timestampToken;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setStatusDescription(final String statusDescription) {
        this.statusDescription = statusDescription;
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

    public void setSignTime(final String signTime) {
        this.signTime = signTime;
    }

    public void setCertificateIssuer(final String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public void setCertificateSerial(final String certificateSerial) {
        this.certificateSerial = certificateSerial;
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

    public void setTimestampToken(final String timestampToken) {
        this.timestampToken = timestampToken;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SignatureVerifyResultDTO)) return false;
        final SignatureVerifyResultDTO other = (SignatureVerifyResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$statusDescription = this.getStatusDescription();
        final java.lang.Object other$statusDescription = other.getStatusDescription();
        if (this$statusDescription == null ? other$statusDescription != null : !this$statusDescription.equals(other$statusDescription)) return false;
        final java.lang.Object this$signType = this.getSignType();
        final java.lang.Object other$signType = other.getSignType();
        if (this$signType == null ? other$signType != null : !this$signType.equals(other$signType)) return false;
        final java.lang.Object this$signAlgorithm = this.getSignAlgorithm();
        final java.lang.Object other$signAlgorithm = other.getSignAlgorithm();
        if (this$signAlgorithm == null ? other$signAlgorithm != null : !this$signAlgorithm.equals(other$signAlgorithm)) return false;
        final java.lang.Object this$signer = this.getSigner();
        final java.lang.Object other$signer = other.getSigner();
        if (this$signer == null ? other$signer != null : !this$signer.equals(other$signer)) return false;
        final java.lang.Object this$signTime = this.getSignTime();
        final java.lang.Object other$signTime = other.getSignTime();
        if (this$signTime == null ? other$signTime != null : !this$signTime.equals(other$signTime)) return false;
        final java.lang.Object this$certificateIssuer = this.getCertificateIssuer();
        final java.lang.Object other$certificateIssuer = other.getCertificateIssuer();
        if (this$certificateIssuer == null ? other$certificateIssuer != null : !this$certificateIssuer.equals(other$certificateIssuer)) return false;
        final java.lang.Object this$certificateSerial = this.getCertificateSerial();
        final java.lang.Object other$certificateSerial = other.getCertificateSerial();
        if (this$certificateSerial == null ? other$certificateSerial != null : !this$certificateSerial.equals(other$certificateSerial)) return false;
        final java.lang.Object this$certificateValidFrom = this.getCertificateValidFrom();
        final java.lang.Object other$certificateValidFrom = other.getCertificateValidFrom();
        if (this$certificateValidFrom == null ? other$certificateValidFrom != null : !this$certificateValidFrom.equals(other$certificateValidFrom)) return false;
        final java.lang.Object this$certificateValidTo = this.getCertificateValidTo();
        final java.lang.Object other$certificateValidTo = other.getCertificateValidTo();
        if (this$certificateValidTo == null ? other$certificateValidTo != null : !this$certificateValidTo.equals(other$certificateValidTo)) return false;
        final java.lang.Object this$timestampAuthority = this.getTimestampAuthority();
        final java.lang.Object other$timestampAuthority = other.getTimestampAuthority();
        if (this$timestampAuthority == null ? other$timestampAuthority != null : !this$timestampAuthority.equals(other$timestampAuthority)) return false;
        final java.lang.Object this$timestampToken = this.getTimestampToken();
        final java.lang.Object other$timestampToken = other.getTimestampToken();
        if (this$timestampToken == null ? other$timestampToken != null : !this$timestampToken.equals(other$timestampToken)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SignatureVerifyResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $statusDescription = this.getStatusDescription();
        result = result * PRIME + ($statusDescription == null ? 43 : $statusDescription.hashCode());
        final java.lang.Object $signType = this.getSignType();
        result = result * PRIME + ($signType == null ? 43 : $signType.hashCode());
        final java.lang.Object $signAlgorithm = this.getSignAlgorithm();
        result = result * PRIME + ($signAlgorithm == null ? 43 : $signAlgorithm.hashCode());
        final java.lang.Object $signer = this.getSigner();
        result = result * PRIME + ($signer == null ? 43 : $signer.hashCode());
        final java.lang.Object $signTime = this.getSignTime();
        result = result * PRIME + ($signTime == null ? 43 : $signTime.hashCode());
        final java.lang.Object $certificateIssuer = this.getCertificateIssuer();
        result = result * PRIME + ($certificateIssuer == null ? 43 : $certificateIssuer.hashCode());
        final java.lang.Object $certificateSerial = this.getCertificateSerial();
        result = result * PRIME + ($certificateSerial == null ? 43 : $certificateSerial.hashCode());
        final java.lang.Object $certificateValidFrom = this.getCertificateValidFrom();
        result = result * PRIME + ($certificateValidFrom == null ? 43 : $certificateValidFrom.hashCode());
        final java.lang.Object $certificateValidTo = this.getCertificateValidTo();
        result = result * PRIME + ($certificateValidTo == null ? 43 : $certificateValidTo.hashCode());
        final java.lang.Object $timestampAuthority = this.getTimestampAuthority();
        result = result * PRIME + ($timestampAuthority == null ? 43 : $timestampAuthority.hashCode());
        final java.lang.Object $timestampToken = this.getTimestampToken();
        result = result * PRIME + ($timestampToken == null ? 43 : $timestampToken.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SignatureVerifyResultDTO(status=" + this.getStatus() + ", statusDescription=" + this.getStatusDescription() + ", signType=" + this.getSignType() + ", signAlgorithm=" + this.getSignAlgorithm() + ", signer=" + this.getSigner() + ", signTime=" + this.getSignTime() + ", certificateIssuer=" + this.getCertificateIssuer() + ", certificateSerial=" + this.getCertificateSerial() + ", certificateValidFrom=" + this.getCertificateValidFrom() + ", certificateValidTo=" + this.getCertificateValidTo() + ", timestampAuthority=" + this.getTimestampAuthority() + ", timestampToken=" + this.getTimestampToken() + ", errorMessage=" + this.getErrorMessage() + ")";
    }
}
