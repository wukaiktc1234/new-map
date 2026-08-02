package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 申诉实体类
 *
 * 对应数据库表 appeals
 * 支持处罚申诉(penalty)和投诉举报(complaint)两种类型
 */
@TableName("appeals")
public class Appeal {

    /** 申诉ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private String appealId;

    /** 申诉类型：penalty=处罚申诉 / complaint=投诉举报 */
    private String type;

    /** 是否匿名 */
    private Boolean anonymousFlag;

    /** 申诉人员ID */
    private String employeeId;

    /** 关联处罚单号（仅penalty模式） */
    private String targetDecisionId;

    /** 状态：pending/processing/resolved/closed/withdrawn */
    private String status;

    /** 申诉标题 */
    private String title;

    /** 详细描述 */
    private String description;

    /** 期望结果 */
    private String expectedResult;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除 / 1=已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public String getAppealId() { return appealId; }
    public void setAppealId(String appealId) { this.appealId = appealId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getAnonymousFlag() { return anonymousFlag; }
    public void setAnonymousFlag(Boolean anonymousFlag) { this.anonymousFlag = anonymousFlag; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getTargetDecisionId() { return targetDecisionId; }
    public void setTargetDecisionId(String targetDecisionId) { this.targetDecisionId = targetDecisionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
