package com.foodtraceability.dto;

/**
 * 申诉附件视图对象（VO）
 */
public class AppealAttachmentVO {

    /** 附件ID */
    private String attachmentId;

    /** 文件名 */
    private String fileName;

    /** 文件类型(MIME) */
    private String fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件访问URL */
    private String fileUrl;

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
