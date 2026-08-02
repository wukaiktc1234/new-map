package com.foodtraceability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉列表视图对象（VO）
 *
 * 用于列表查询返回，不含详细描述和日志
 */
public class AppealVO {

    /** 申诉ID */
    private String appealId;

    /** 申诉类型 */
    private String type;

    /** 是否匿名 */
    private Boolean anonymousFlag;

    /** 状态 */
    private String status;

    /** 标题 */
    private String title;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 附件数量 */
    private Integer attachmentCount;

    public String getAppealId() {
        return appealId;
    }

    public void setAppealId(String appealId) {
        this.appealId = appealId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getAnonymousFlag() {
        return anonymousFlag;
    }

    public void setAnonymousFlag(Boolean anonymousFlag) {
        this.anonymousFlag = anonymousFlag;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
}
