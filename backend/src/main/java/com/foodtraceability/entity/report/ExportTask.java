package com.foodtraceability.entity.report;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报表导出任务实体类
 * 对应数据库表 export_tasks
 */
@TableName("export_tasks")
public class ExportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @TableId(value = "task_id", type = IdType.AUTO)
    private Long taskId;

    /** 任务编号 */
    @TableField("task_no")
    private String taskNo;

    /**
     * 任务类型
     * 1-Excel 2-PDF
     */
    @TableField("task_type")
    private Integer taskType;

    /**
     * 报表类型
     * 1-日报 2-周报 3-月报 4-季报 5-年报 6-利润分析
     */
    @TableField("report_type")
    private Integer reportType;

    /** 报表参数JSON */
    @TableField("report_params_json")
    private String reportParamsJson;

    /** 文件名 */
    @TableField("file_name")
    private String fileName;

    /** 文件存储路径 */
    @TableField("file_path")
    private String filePath;

    /** 文件大小（字节） */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 状态
     * 0-待处理 1-处理中 2-成功 3-失败
     */
    @TableField("status")
    private Integer status;

    /** 错误信息 */
    @TableField("error_message")
    private String errorMessage;

    /** 导出数据行数 */
    @TableField("row_count")
    private Integer rowCount;

    /** 创建人ID */
    @TableField("created_by")
    private Long createdBy;

    /** 完成时间 */
    @TableField("completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Integer getTaskType() {
        return taskType;
    }

    public void setTaskType(Integer taskType) {
        this.taskType = taskType;
    }

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public String getReportParamsJson() {
        return reportParamsJson;
    }

    public void setReportParamsJson(String reportParamsJson) {
        this.reportParamsJson = reportParamsJson;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
