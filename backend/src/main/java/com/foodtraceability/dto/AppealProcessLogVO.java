package com.foodtraceability.dto;

import java.time.LocalDateTime;

/**
 * 申诉处理日志视图对象（VO）
 */
public class AppealProcessLogVO {

    /** 日志ID */
    private String logId;

    /** 操作类型 */
    private String action;

    /** 操作人姓名 */
    private String operatorName;

    /** 处理意见 */
    private String comment;

    /** 操作时间 */
    private LocalDateTime createTime;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
