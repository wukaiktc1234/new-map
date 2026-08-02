package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 申诉附件实体类
 *
 * 对应数据库表 appeal_attachments
 * 存储申诉时上传的证据文件信息
 */
@TableName("appeal_attachments")
public class AppealAttachment {

    /** 附件ID（数据库自增生成） */
    @TableId(type = IdType.AUTO)
    private Long attachmentId;

    /** 关联申诉ID */
    private String appealId;

    /** 文件名 */
    private String fileName;

    /** 文件访问URL */
    private String fileUrl;

    /** 文件MIME类型 */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }

    public String getAppealId() { return appealId; }
    public void setAppealId(String appealId) { this.appealId = appealId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
