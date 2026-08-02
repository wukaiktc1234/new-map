package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 凭证类型检测结果DTO
 */
@Schema(description = "凭证类型检测结果")
public class VoucherTypeDetectResult {
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件大小(字节)")
    private Long fileSize;
    @Schema(description = "文件扩展名")
    private String fileExtension;
    @Schema(description = "MIME类型")
    private String mimeType;
    @Schema(description = "识别的凭证类型")
    private String voucherType;
    @Schema(description = "凭证子类型")
    private String subType;
    @Schema(description = "识别置信度(0-1)")
    private Double confidence;
    @Schema(description = "识别消息")
    private String message;
    @Schema(description = "XML根节点名称")
    private String xmlRootNode;
    @Schema(description = "解析方式")
    private String parseMethod;
    @Schema(description = "是否包含签名")
    private Boolean hasSignature;
    @Schema(description = "签名类型")
    private String signatureType;
    @Schema(description = "是否需要OCR识别")
    private Boolean needOcr;
    @Schema(description = "建议的解析器")
    private String suggestedParser;

    public boolean isHighConfidence() {
        return confidence != null && confidence >= 0.8;
    }

    public boolean needsManualConfirmation() {
        return confidence == null || confidence < 0.7;
    }


    public static class VoucherTypeDetectResultBuilder {
        private String fileName;
        private Long fileSize;
        private String fileExtension;
        private String mimeType;
        private String voucherType;
        private String subType;
        private Double confidence;
        private String message;
        private String xmlRootNode;
        private String parseMethod;
        private Boolean hasSignature;
        private String signatureType;
        private Boolean needOcr;
        private String suggestedParser;

        VoucherTypeDetectResultBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder fileSize(final Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder fileExtension(final String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder mimeType(final String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder voucherType(final String voucherType) {
            this.voucherType = voucherType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder subType(final String subType) {
            this.subType = subType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder confidence(final Double confidence) {
            this.confidence = confidence;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder xmlRootNode(final String xmlRootNode) {
            this.xmlRootNode = xmlRootNode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder parseMethod(final String parseMethod) {
            this.parseMethod = parseMethod;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder hasSignature(final Boolean hasSignature) {
            this.hasSignature = hasSignature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder signatureType(final String signatureType) {
            this.signatureType = signatureType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder needOcr(final Boolean needOcr) {
            this.needOcr = needOcr;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public VoucherTypeDetectResult.VoucherTypeDetectResultBuilder suggestedParser(final String suggestedParser) {
            this.suggestedParser = suggestedParser;
            return this;
        }

        public VoucherTypeDetectResult build() {
            return new VoucherTypeDetectResult(this.fileName, this.fileSize, this.fileExtension, this.mimeType, this.voucherType, this.subType, this.confidence, this.message, this.xmlRootNode, this.parseMethod, this.hasSignature, this.signatureType, this.needOcr, this.suggestedParser);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "VoucherTypeDetectResult.VoucherTypeDetectResultBuilder(fileName=" + this.fileName + ", fileSize=" + this.fileSize + ", fileExtension=" + this.fileExtension + ", mimeType=" + this.mimeType + ", voucherType=" + this.voucherType + ", subType=" + this.subType + ", confidence=" + this.confidence + ", message=" + this.message + ", xmlRootNode=" + this.xmlRootNode + ", parseMethod=" + this.parseMethod + ", hasSignature=" + this.hasSignature + ", signatureType=" + this.signatureType + ", needOcr=" + this.needOcr + ", suggestedParser=" + this.suggestedParser + ")";
        }
    }

    public static VoucherTypeDetectResult.VoucherTypeDetectResultBuilder builder() {
        return new VoucherTypeDetectResult.VoucherTypeDetectResultBuilder();
    }

    public String getFileName() {
        return this.fileName;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public String getSubType() {
        return this.subType;
    }

    public Double getConfidence() {
        return this.confidence;
    }

    public String getMessage() {
        return this.message;
    }

    public String getXmlRootNode() {
        return this.xmlRootNode;
    }

    public String getParseMethod() {
        return this.parseMethod;
    }

    public Boolean getHasSignature() {
        return this.hasSignature;
    }

    public String getSignatureType() {
        return this.signatureType;
    }

    public Boolean getNeedOcr() {
        return this.needOcr;
    }

    public String getSuggestedParser() {
        return this.suggestedParser;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileExtension(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setSubType(final String subType) {
        this.subType = subType;
    }

    public void setConfidence(final Double confidence) {
        this.confidence = confidence;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setXmlRootNode(final String xmlRootNode) {
        this.xmlRootNode = xmlRootNode;
    }

    public void setParseMethod(final String parseMethod) {
        this.parseMethod = parseMethod;
    }

    public void setHasSignature(final Boolean hasSignature) {
        this.hasSignature = hasSignature;
    }

    public void setSignatureType(final String signatureType) {
        this.signatureType = signatureType;
    }

    public void setNeedOcr(final Boolean needOcr) {
        this.needOcr = needOcr;
    }

    public void setSuggestedParser(final String suggestedParser) {
        this.suggestedParser = suggestedParser;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherTypeDetectResult)) return false;
        final VoucherTypeDetectResult other = (VoucherTypeDetectResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$fileSize = this.getFileSize();
        final java.lang.Object other$fileSize = other.getFileSize();
        if (this$fileSize == null ? other$fileSize != null : !this$fileSize.equals(other$fileSize)) return false;
        final java.lang.Object this$confidence = this.getConfidence();
        final java.lang.Object other$confidence = other.getConfidence();
        if (this$confidence == null ? other$confidence != null : !this$confidence.equals(other$confidence)) return false;
        final java.lang.Object this$hasSignature = this.getHasSignature();
        final java.lang.Object other$hasSignature = other.getHasSignature();
        if (this$hasSignature == null ? other$hasSignature != null : !this$hasSignature.equals(other$hasSignature)) return false;
        final java.lang.Object this$needOcr = this.getNeedOcr();
        final java.lang.Object other$needOcr = other.getNeedOcr();
        if (this$needOcr == null ? other$needOcr != null : !this$needOcr.equals(other$needOcr)) return false;
        final java.lang.Object this$fileName = this.getFileName();
        final java.lang.Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName)) return false;
        final java.lang.Object this$fileExtension = this.getFileExtension();
        final java.lang.Object other$fileExtension = other.getFileExtension();
        if (this$fileExtension == null ? other$fileExtension != null : !this$fileExtension.equals(other$fileExtension)) return false;
        final java.lang.Object this$mimeType = this.getMimeType();
        final java.lang.Object other$mimeType = other.getMimeType();
        if (this$mimeType == null ? other$mimeType != null : !this$mimeType.equals(other$mimeType)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$subType = this.getSubType();
        final java.lang.Object other$subType = other.getSubType();
        if (this$subType == null ? other$subType != null : !this$subType.equals(other$subType)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$xmlRootNode = this.getXmlRootNode();
        final java.lang.Object other$xmlRootNode = other.getXmlRootNode();
        if (this$xmlRootNode == null ? other$xmlRootNode != null : !this$xmlRootNode.equals(other$xmlRootNode)) return false;
        final java.lang.Object this$parseMethod = this.getParseMethod();
        final java.lang.Object other$parseMethod = other.getParseMethod();
        if (this$parseMethod == null ? other$parseMethod != null : !this$parseMethod.equals(other$parseMethod)) return false;
        final java.lang.Object this$signatureType = this.getSignatureType();
        final java.lang.Object other$signatureType = other.getSignatureType();
        if (this$signatureType == null ? other$signatureType != null : !this$signatureType.equals(other$signatureType)) return false;
        final java.lang.Object this$suggestedParser = this.getSuggestedParser();
        final java.lang.Object other$suggestedParser = other.getSuggestedParser();
        if (this$suggestedParser == null ? other$suggestedParser != null : !this$suggestedParser.equals(other$suggestedParser)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherTypeDetectResult;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $fileSize = this.getFileSize();
        result = result * PRIME + ($fileSize == null ? 43 : $fileSize.hashCode());
        final java.lang.Object $confidence = this.getConfidence();
        result = result * PRIME + ($confidence == null ? 43 : $confidence.hashCode());
        final java.lang.Object $hasSignature = this.getHasSignature();
        result = result * PRIME + ($hasSignature == null ? 43 : $hasSignature.hashCode());
        final java.lang.Object $needOcr = this.getNeedOcr();
        result = result * PRIME + ($needOcr == null ? 43 : $needOcr.hashCode());
        final java.lang.Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        final java.lang.Object $fileExtension = this.getFileExtension();
        result = result * PRIME + ($fileExtension == null ? 43 : $fileExtension.hashCode());
        final java.lang.Object $mimeType = this.getMimeType();
        result = result * PRIME + ($mimeType == null ? 43 : $mimeType.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $subType = this.getSubType();
        result = result * PRIME + ($subType == null ? 43 : $subType.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $xmlRootNode = this.getXmlRootNode();
        result = result * PRIME + ($xmlRootNode == null ? 43 : $xmlRootNode.hashCode());
        final java.lang.Object $parseMethod = this.getParseMethod();
        result = result * PRIME + ($parseMethod == null ? 43 : $parseMethod.hashCode());
        final java.lang.Object $signatureType = this.getSignatureType();
        result = result * PRIME + ($signatureType == null ? 43 : $signatureType.hashCode());
        final java.lang.Object $suggestedParser = this.getSuggestedParser();
        result = result * PRIME + ($suggestedParser == null ? 43 : $suggestedParser.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherTypeDetectResult(fileName=" + this.getFileName() + ", fileSize=" + this.getFileSize() + ", fileExtension=" + this.getFileExtension() + ", mimeType=" + this.getMimeType() + ", voucherType=" + this.getVoucherType() + ", subType=" + this.getSubType() + ", confidence=" + this.getConfidence() + ", message=" + this.getMessage() + ", xmlRootNode=" + this.getXmlRootNode() + ", parseMethod=" + this.getParseMethod() + ", hasSignature=" + this.getHasSignature() + ", signatureType=" + this.getSignatureType() + ", needOcr=" + this.getNeedOcr() + ", suggestedParser=" + this.getSuggestedParser() + ")";
    }

    public VoucherTypeDetectResult() {
    }

    public VoucherTypeDetectResult(final String fileName, final Long fileSize, final String fileExtension, final String mimeType, final String voucherType, final String subType, final Double confidence, final String message, final String xmlRootNode, final String parseMethod, final Boolean hasSignature, final String signatureType, final Boolean needOcr, final String suggestedParser) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileExtension = fileExtension;
        this.mimeType = mimeType;
        this.voucherType = voucherType;
        this.subType = subType;
        this.confidence = confidence;
        this.message = message;
        this.xmlRootNode = xmlRootNode;
        this.parseMethod = parseMethod;
        this.hasSignature = hasSignature;
        this.signatureType = signatureType;
        this.needOcr = needOcr;
        this.suggestedParser = suggestedParser;
    }
}
