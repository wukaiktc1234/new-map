package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 申诉处理日志实体类
 *
 * 对应数据库表 appeal_process_logs
 * 记录申诉的每次状态变更操作
 */
@TableName("appeal_process_logs")
public class AppealProcessLog {

    /** 日志ID（数据库自增生成） */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 关联申诉ID */
    private String appealId;

    /** 操作类型：submit/accept/process/resolve/close/withdraw/reject */
    private String action;

    /** 操作人ID */
    private String operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 处理意见/备注 */
    private String comment;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;


    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getAppealId() { return appealId; }
    public void setAppealId(String appealId) { this.appealId = appealId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
