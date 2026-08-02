package com.foodtraceability.dto;

public class UploadProgressDTO {
    private String uploadId;
    private String stage;
    private int progress;
    private String message;
    private String detail;
    private Long voucherId;
    private String voucherNo;
    private String voucherType;
    private String status;
    private String errorMessage;
    private Long timestamp;

    public static UploadProgressDTO create(String uploadId, String stage, int progress, String message) {
        return UploadProgressDTO.builder().uploadId(uploadId).stage(stage).progress(progress).message(message).timestamp(System.currentTimeMillis()).build();
    }

    public static UploadProgressDTO create(String uploadId, String stage, int progress, String message, String detail) {
        return UploadProgressDTO.builder().uploadId(uploadId).stage(stage).progress(progress).message(message).detail(detail).timestamp(System.currentTimeMillis()).build();
    }

    public static final String STAGE_INIT = "init";
    public static final String STAGE_UPLOAD = "upload";
    public static final String STAGE_VALIDATE = "validate";
    public static final String STAGE_PARSE = "parse";
    public static final String STAGE_QR_CODE = "qr_code";
    public static final String STAGE_OCR = "ocr";
    public static final String STAGE_SAVE = "save";
    public static final String STAGE_VERIFY = "verify";
    public static final String STAGE_COMPLETE = "complete";
    public static final String STAGE_ERROR = "error";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_DUPLICATE = "duplicate";
    public static final String STATUS_ERROR = "error";


    public static class UploadProgressDTOBuilder {
        private String uploadId;
        private String stage;
        private int progress;
        private String message;
        private String detail;
        private Long voucherId;
        private String voucherNo;
        private String voucherType;
        private String status;
        private String errorMessage;
        private Long timestamp;

        UploadProgressDTOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder uploadId(final String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder stage(final String stage) {
            this.stage = stage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder progress(final int progress) {
            this.progress = progress;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder detail(final String detail) {
            this.detail = detail;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder voucherId(final Long voucherId) {
            this.voucherId = voucherId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder voucherNo(final String voucherNo) {
            this.voucherNo = voucherNo;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder voucherType(final String voucherType) {
            this.voucherType = voucherType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public UploadProgressDTO.UploadProgressDTOBuilder timestamp(final Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public UploadProgressDTO build() {
            return new UploadProgressDTO(this.uploadId, this.stage, this.progress, this.message, this.detail, this.voucherId, this.voucherNo, this.voucherType, this.status, this.errorMessage, this.timestamp);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "UploadProgressDTO.UploadProgressDTOBuilder(uploadId=" + this.uploadId + ", stage=" + this.stage + ", progress=" + this.progress + ", message=" + this.message + ", detail=" + this.detail + ", voucherId=" + this.voucherId + ", voucherNo=" + this.voucherNo + ", voucherType=" + this.voucherType + ", status=" + this.status + ", errorMessage=" + this.errorMessage + ", timestamp=" + this.timestamp + ")";
        }
    }

    public static UploadProgressDTO.UploadProgressDTOBuilder builder() {
        return new UploadProgressDTO.UploadProgressDTOBuilder();
    }

    public String getUploadId() {
        return this.uploadId;
    }

    public String getStage() {
        return this.stage;
    }

    public int getProgress() {
        return this.progress;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDetail() {
        return this.detail;
    }

    public Long getVoucherId() {
        return this.voucherId;
    }

    public String getVoucherNo() {
        return this.voucherNo;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public String getStatus() {
        return this.status;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setUploadId(final String uploadId) {
        this.uploadId = uploadId;
    }

    public void setStage(final String stage) {
        this.stage = stage;
    }

    public void setProgress(final int progress) {
        this.progress = progress;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setDetail(final String detail) {
        this.detail = detail;
    }

    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    public void setVoucherNo(final String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UploadProgressDTO)) return false;
        final UploadProgressDTO other = (UploadProgressDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getProgress() != other.getProgress()) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$timestamp = this.getTimestamp();
        final java.lang.Object other$timestamp = other.getTimestamp();
        if (this$timestamp == null ? other$timestamp != null : !this$timestamp.equals(other$timestamp)) return false;
        final java.lang.Object this$uploadId = this.getUploadId();
        final java.lang.Object other$uploadId = other.getUploadId();
        if (this$uploadId == null ? other$uploadId != null : !this$uploadId.equals(other$uploadId)) return false;
        final java.lang.Object this$stage = this.getStage();
        final java.lang.Object other$stage = other.getStage();
        if (this$stage == null ? other$stage != null : !this$stage.equals(other$stage)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$detail = this.getDetail();
        final java.lang.Object other$detail = other.getDetail();
        if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) return false;
        final java.lang.Object this$voucherNo = this.getVoucherNo();
        final java.lang.Object other$voucherNo = other.getVoucherNo();
        if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UploadProgressDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getProgress();
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $timestamp = this.getTimestamp();
        result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
        final java.lang.Object $uploadId = this.getUploadId();
        result = result * PRIME + ($uploadId == null ? 43 : $uploadId.hashCode());
        final java.lang.Object $stage = this.getStage();
        result = result * PRIME + ($stage == null ? 43 : $stage.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $detail = this.getDetail();
        result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "UploadProgressDTO(uploadId=" + this.getUploadId() + ", stage=" + this.getStage() + ", progress=" + this.getProgress() + ", message=" + this.getMessage() + ", detail=" + this.getDetail() + ", voucherId=" + this.getVoucherId() + ", voucherNo=" + this.getVoucherNo() + ", voucherType=" + this.getVoucherType() + ", status=" + this.getStatus() + ", errorMessage=" + this.getErrorMessage() + ", timestamp=" + this.getTimestamp() + ")";
    }

    public UploadProgressDTO() {
    }

    public UploadProgressDTO(final String uploadId, final String stage, final int progress, final String message, final String detail, final Long voucherId, final String voucherNo, final String voucherType, final String status, final String errorMessage, final Long timestamp) {
        this.uploadId = uploadId;
        this.stage = stage;
        this.progress = progress;
        this.message = message;
        this.detail = detail;
        this.voucherId = voucherId;
        this.voucherNo = voucherNo;
        this.voucherType = voucherType;
        this.status = status;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }
}
