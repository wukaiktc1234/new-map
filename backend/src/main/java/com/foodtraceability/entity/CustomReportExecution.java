package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Date;

/**
 * 自定义报表执行历史实体类
 * 用于存储自定义报表的执行记录
 */
@TableName("custom_report_executions")
public class CustomReportExecution {

    /**
     * 执行ID
     */
    @TableId(type = IdType.AUTO)
    private Long executionId;

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 执行人ID
     */
    private Long executedBy;

    /**
     * 执行状态
     * 1运行中 2成功 3失败
     */
    private Integer executionStatus;

    /**
     * 查询结果JSON
     */
    private String resultDataJson;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 耗时毫秒
     */
    private Long executionTimeMs;

    /**
     * 执行时间
     */
    private Date executedAt;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // getter和setter方法
    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(Long executedBy) {
        this.executedBy = executedBy;
    }

    public Integer getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(Integer executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getResultDataJson() {
        return resultDataJson;
    }

    public void setResultDataJson(String resultDataJson) {
        this.resultDataJson = resultDataJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Date getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Date executedAt) {
        this.executedAt = executedAt;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
