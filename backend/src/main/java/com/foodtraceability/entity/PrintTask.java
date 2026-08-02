package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 打印任务实体类
 * 对应数据库表 print_tasks
 */
@TableName("print_tasks")
@Schema(description = "打印任务实体")
public class PrintTask {

    /** 任务ID */
    @TableId(value = "task_id", type = IdType.AUTO)
    @Schema(description = "任务ID")
    private Long taskId;

    /** 任务编号 */
    @TableField("task_code")
    @Schema(description = "任务编号")
    private String taskCode;

    /** 目标打印机ID */
    @TableField("device_id")
    @Schema(description = "目标打印机ID")
    private Long deviceId;

    /** 任务类型：1小票 2标签 3报表 4厨房单 */
    @TableField("task_type")
    @Schema(description = "任务类型")
    private Integer taskType;

    /** 打印内容JSON/HTML/template */
    @TableField("content_json")
    @Schema(description = "打印内容")
    private String contentJson;

    /** 打印状态：0待打印 1打印中 2已完成 3失败 */
    @TableField("print_status")
    @Schema(description = "打印状态")
    private Integer printStatus;

    /** 已重试次数 */
    @TableField("retry_count")
    @Schema(description = "已重试次数")
    private Integer retryCount;

    /** 最大重试次数 */
    @TableField("max_retry")
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    /** 错误信息 */
    @TableField("error_message")
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 创建用户ID */
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private Long createUserId;

    /** 打印时间 */
    @TableField("print_time")
    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    /** 完成时间 */
    @TableField("complete_time")
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 兼容性别名 - 状态 */
    @Schema(description = "状态（兼容性）", hidden = true)
    private transient Integer status;

    /** 文件路径 */
    @TableField("file_path")
    @Schema(description = "文件路径")
    private String filePath;

    /** 追溯码 */
    @TableField("traceability_code")
    @Schema(description = "追溯码")
    private String traceabilityCode;

    /** 产品名称 */
    @TableField("product_name")
    @Schema(description = "产品名称")
    private String productName;

    /** 设备类型（冗余字段） */
    @TableField("device_type")
    @Schema(description = "设备类型")
    private Integer deviceType;

    /** 发票数据（兼容性字段，非数据库映射，支持JSON结构） */
    @Schema(description = "发票数据", hidden = true)
    private transient Map<String, Object> invoiceData;

    // ==================== Getter & Setter 方法 ====================

    public Long getId() {
        return taskId;
    }

    public void setId(Long id) {
        this.taskId = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getTaskType() {
        return taskType;
    }

    public void setTaskType(Integer taskType) {
        this.taskType = taskType;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public Integer getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(Integer printStatus) {
        this.printStatus = printStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getPrintTime() {
        return printTime;
    }

    public void setPrintTime(LocalDateTime printTime) {
        this.printTime = printTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
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

    // ==================== 兼容性方法 ====================

    public Integer getStatus() {
        return status != null ? status : printStatus;
    }

    public void setStatus(Integer status) {
        this.status = status;
        this.printStatus = status;
    }

    public String getRemark() {
        return errorMessage;
    }

    public void setRemark(String remark) {
        this.errorMessage = remark;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContent() {
        return contentJson;
    }

    public void setContent(String content) {
        this.contentJson = content;
    }

    public String getTraceabilityCode() {
        return traceabilityCode;
    }

    public void setTraceabilityCode(String traceabilityCode) {
        this.traceabilityCode = traceabilityCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getPrintType() {
        return taskType;
    }

    public void setPrintType(Integer printType) {
        this.taskType = printType;
    }

    public String getErrorMsg() {
        return errorMessage;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMessage = errorMsg;
    }

    // ==================== invoiceData 兼容性方法 ====================

    public Map<String, Object> getInvoiceData() {
        return invoiceData;
    }

    public void setInvoiceData(Map<String, Object> invoiceData) {
        this.invoiceData = invoiceData;
    }
}
