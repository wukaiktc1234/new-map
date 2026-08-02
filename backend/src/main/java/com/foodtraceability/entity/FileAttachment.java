package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("file_attachment")
@Schema(description = "文件附件实体")
public class FileAttachment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "附件ID")
    private Long attachmentId;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private String tenantId;
    @TableField("business_type")
    @Schema(description = "业务类型: voucher/invoice/product/avatar/document/general")
    private String businessType;
    @TableField("business_id")
    @Schema(description = "关联业务ID")
    private String businessId;
    @TableField("original_name")
    @Schema(description = "原始文件名")
    private String originalName;
    @TableField("stored_name")
    @Schema(description = "存储文件名(UUID)")
    private String storedName;
    @TableField("file_path")
    @Schema(description = "存储相对路径")
    private String filePath;
    @TableField("file_extension")
    @Schema(description = "文件扩展名")
    private String fileExtension;
    @TableField("content_type")
    @Schema(description = "MIME类型")
    private String contentType;
    @TableField("file_size")
    @Schema(description = "文件大小(字节)")
    private Long fileSize;
    @TableField("file_hash_md5")
    @Schema(description = "MD5哈希值")
    private String fileHashMd5;
    @TableField("file_hash_sha256")
    @Schema(description = "SHA256哈希值")
    private String fileHashSha256;
    @TableField("storage_type")
    @Schema(description = "存储类型: LOCAL/OSS/S3/MINIO")
    private String storageType;
    @TableField("thumbnail_path")
    @Schema(description = "缩略图路径")
    private String thumbnailPath;
    @TableField("width")
    @Schema(description = "图片宽度")
    private Integer width;
    @TableField("height")
    @Schema(description = "图片高度")
    private Integer height;
    @TableField("upload_user_id")
    @Schema(description = "上传用户ID")
    private String uploadUserId;
    @TableField("upload_username")
    @Schema(description = "上传用户名")
    private String uploadUsername;
    @TableField("download_count")
    @Schema(description = "下载次数")
    private Long downloadCount;
    @TableField("status")
    @Schema(description = "状态: active/deleted/archived")
    private String status;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;

    public FileAttachment() {
    }

    public Long getAttachmentId() {
        return this.attachmentId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public String getBusinessType() {
        return this.businessType;
    }

    public String getBusinessId() {
        return this.businessId;
    }

    public String getOriginalName() {
        return this.originalName;
    }

    public String getStoredName() {
        return this.storedName;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public String getFileHashMd5() {
        return this.fileHashMd5;
    }

    public String getFileHashSha256() {
        return this.fileHashSha256;
    }

    public String getStorageType() {
        return this.storageType;
    }

    public String getThumbnailPath() {
        return this.thumbnailPath;
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public String getUploadUserId() {
        return this.uploadUserId;
    }

    public String getUploadUsername() {
        return this.uploadUsername;
    }

    public Long getDownloadCount() {
        return this.downloadCount;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    public void setAttachmentId(final Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public void setBusinessId(final String businessId) {
        this.businessId = businessId;
    }

    public void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    public void setStoredName(final String storedName) {
        this.storedName = storedName;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public void setFileExtension(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileHashMd5(final String fileHashMd5) {
        this.fileHashMd5 = fileHashMd5;
    }

    public void setFileHashSha256(final String fileHashSha256) {
        this.fileHashSha256 = fileHashSha256;
    }

    public void setStorageType(final String storageType) {
        this.storageType = storageType;
    }

    public void setThumbnailPath(final String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setWidth(final Integer width) {
        this.width = width;
    }

    public void setHeight(final Integer height) {
        this.height = height;
    }

    public void setUploadUserId(final String uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public void setUploadUsername(final String uploadUsername) {
        this.uploadUsername = uploadUsername;
    }

    public void setDownloadCount(final Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeletedAt(final LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FileAttachment)) return false;
        final FileAttachment other = (FileAttachment) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$attachmentId = this.getAttachmentId();
        final java.lang.Object other$attachmentId = other.getAttachmentId();
        if (this$attachmentId == null ? other$attachmentId != null : !this$attachmentId.equals(other$attachmentId)) return false;
        final java.lang.Object this$fileSize = this.getFileSize();
        final java.lang.Object other$fileSize = other.getFileSize();
        if (this$fileSize == null ? other$fileSize != null : !this$fileSize.equals(other$fileSize)) return false;
        final java.lang.Object this$width = this.getWidth();
        final java.lang.Object other$width = other.getWidth();
        if (this$width == null ? other$width != null : !this$width.equals(other$width)) return false;
        final java.lang.Object this$height = this.getHeight();
        final java.lang.Object other$height = other.getHeight();
        if (this$height == null ? other$height != null : !this$height.equals(other$height)) return false;
        final java.lang.Object this$downloadCount = this.getDownloadCount();
        final java.lang.Object other$downloadCount = other.getDownloadCount();
        if (this$downloadCount == null ? other$downloadCount != null : !this$downloadCount.equals(other$downloadCount)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$originalName = this.getOriginalName();
        final java.lang.Object other$originalName = other.getOriginalName();
        if (this$originalName == null ? other$originalName != null : !this$originalName.equals(other$originalName)) return false;
        final java.lang.Object this$storedName = this.getStoredName();
        final java.lang.Object other$storedName = other.getStoredName();
        if (this$storedName == null ? other$storedName != null : !this$storedName.equals(other$storedName)) return false;
        final java.lang.Object this$filePath = this.getFilePath();
        final java.lang.Object other$filePath = other.getFilePath();
        if (this$filePath == null ? other$filePath != null : !this$filePath.equals(other$filePath)) return false;
        final java.lang.Object this$fileExtension = this.getFileExtension();
        final java.lang.Object other$fileExtension = other.getFileExtension();
        if (this$fileExtension == null ? other$fileExtension != null : !this$fileExtension.equals(other$fileExtension)) return false;
        final java.lang.Object this$contentType = this.getContentType();
        final java.lang.Object other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType)) return false;
        final java.lang.Object this$fileHashMd5 = this.getFileHashMd5();
        final java.lang.Object other$fileHashMd5 = other.getFileHashMd5();
        if (this$fileHashMd5 == null ? other$fileHashMd5 != null : !this$fileHashMd5.equals(other$fileHashMd5)) return false;
        final java.lang.Object this$fileHashSha256 = this.getFileHashSha256();
        final java.lang.Object other$fileHashSha256 = other.getFileHashSha256();
        if (this$fileHashSha256 == null ? other$fileHashSha256 != null : !this$fileHashSha256.equals(other$fileHashSha256)) return false;
        final java.lang.Object this$storageType = this.getStorageType();
        final java.lang.Object other$storageType = other.getStorageType();
        if (this$storageType == null ? other$storageType != null : !this$storageType.equals(other$storageType)) return false;
        final java.lang.Object this$thumbnailPath = this.getThumbnailPath();
        final java.lang.Object other$thumbnailPath = other.getThumbnailPath();
        if (this$thumbnailPath == null ? other$thumbnailPath != null : !this$thumbnailPath.equals(other$thumbnailPath)) return false;
        final java.lang.Object this$uploadUserId = this.getUploadUserId();
        final java.lang.Object other$uploadUserId = other.getUploadUserId();
        if (this$uploadUserId == null ? other$uploadUserId != null : !this$uploadUserId.equals(other$uploadUserId)) return false;
        final java.lang.Object this$uploadUsername = this.getUploadUsername();
        final java.lang.Object other$uploadUsername = other.getUploadUsername();
        if (this$uploadUsername == null ? other$uploadUsername != null : !this$uploadUsername.equals(other$uploadUsername)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$deletedAt = this.getDeletedAt();
        final java.lang.Object other$deletedAt = other.getDeletedAt();
        if (this$deletedAt == null ? other$deletedAt != null : !this$deletedAt.equals(other$deletedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FileAttachment;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $attachmentId = this.getAttachmentId();
        result = result * PRIME + ($attachmentId == null ? 43 : $attachmentId.hashCode());
        final java.lang.Object $fileSize = this.getFileSize();
        result = result * PRIME + ($fileSize == null ? 43 : $fileSize.hashCode());
        final java.lang.Object $width = this.getWidth();
        result = result * PRIME + ($width == null ? 43 : $width.hashCode());
        final java.lang.Object $height = this.getHeight();
        result = result * PRIME + ($height == null ? 43 : $height.hashCode());
        final java.lang.Object $downloadCount = this.getDownloadCount();
        result = result * PRIME + ($downloadCount == null ? 43 : $downloadCount.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $originalName = this.getOriginalName();
        result = result * PRIME + ($originalName == null ? 43 : $originalName.hashCode());
        final java.lang.Object $storedName = this.getStoredName();
        result = result * PRIME + ($storedName == null ? 43 : $storedName.hashCode());
        final java.lang.Object $filePath = this.getFilePath();
        result = result * PRIME + ($filePath == null ? 43 : $filePath.hashCode());
        final java.lang.Object $fileExtension = this.getFileExtension();
        result = result * PRIME + ($fileExtension == null ? 43 : $fileExtension.hashCode());
        final java.lang.Object $contentType = this.getContentType();
        result = result * PRIME + ($contentType == null ? 43 : $contentType.hashCode());
        final java.lang.Object $fileHashMd5 = this.getFileHashMd5();
        result = result * PRIME + ($fileHashMd5 == null ? 43 : $fileHashMd5.hashCode());
        final java.lang.Object $fileHashSha256 = this.getFileHashSha256();
        result = result * PRIME + ($fileHashSha256 == null ? 43 : $fileHashSha256.hashCode());
        final java.lang.Object $storageType = this.getStorageType();
        result = result * PRIME + ($storageType == null ? 43 : $storageType.hashCode());
        final java.lang.Object $thumbnailPath = this.getThumbnailPath();
        result = result * PRIME + ($thumbnailPath == null ? 43 : $thumbnailPath.hashCode());
        final java.lang.Object $uploadUserId = this.getUploadUserId();
        result = result * PRIME + ($uploadUserId == null ? 43 : $uploadUserId.hashCode());
        final java.lang.Object $uploadUsername = this.getUploadUsername();
        result = result * PRIME + ($uploadUsername == null ? 43 : $uploadUsername.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $deletedAt = this.getDeletedAt();
        result = result * PRIME + ($deletedAt == null ? 43 : $deletedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FileAttachment(attachmentId=" + this.getAttachmentId() + ", tenantId=" + this.getTenantId() + ", businessType=" + this.getBusinessType() + ", businessId=" + this.getBusinessId() + ", originalName=" + this.getOriginalName() + ", storedName=" + this.getStoredName() + ", filePath=" + this.getFilePath() + ", fileExtension=" + this.getFileExtension() + ", contentType=" + this.getContentType() + ", fileSize=" + this.getFileSize() + ", fileHashMd5=" + this.getFileHashMd5() + ", fileHashSha256=" + this.getFileHashSha256() + ", storageType=" + this.getStorageType() + ", thumbnailPath=" + this.getThumbnailPath() + ", width=" + this.getWidth() + ", height=" + this.getHeight() + ", uploadUserId=" + this.getUploadUserId() + ", uploadUsername=" + this.getUploadUsername() + ", downloadCount=" + this.getDownloadCount() + ", status=" + this.getStatus() + ", deleted=" + this.getDeleted() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deletedAt=" + this.getDeletedAt() + ")";
    }
}
