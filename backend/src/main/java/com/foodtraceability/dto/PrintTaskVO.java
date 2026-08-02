package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印任务VO
 */
@Schema(description = "打印任务视图对象")
public class PrintTaskVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskCode;

    @Schema(description = "目标打印机ID")
    private Long deviceId;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "任务类型")
    private Integer taskType;

    @Schema(description = "任务类型名称")
    private String taskTypeName;

    @Schema(description = "打印内容")
    private String contentJson;

    @Schema(description = "打印状态")
    private Integer printStatus;

    @Schema(description = "打印状态名称")
    private String printStatusName;

    @Schema(description = "已重试次数")
    private Integer retryCount;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建用户ID")
    private Long createUserId;

    @Schema(description = "打印时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime printTime;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }

    public String getTaskTypeName() { return taskTypeName; }
    public void setTaskTypeName(String taskTypeName) { this.taskTypeName = taskTypeName; }

    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }

    public Integer getPrintStatus() { return printStatus; }
    public void setPrintStatus(Integer printStatus) { this.printStatus = printStatus; }

    public String getPrintStatusName() { return printStatusName; }
    public void setPrintStatusName(String printStatusName) { this.printStatusName = printStatusName; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getCreateUserId() { return createUserId; }
    public void setCreateUserId(Long createUserId) { this.createUserId = createUserId; }

    public LocalDateTime getPrintTime() { return printTime; }
    public void setPrintTime(LocalDateTime printTime) { this.printTime = printTime; }

    public LocalDateTime getCompleteTime() { return completeTime; }
    public void setCompleteTime(LocalDateTime completeTime) { this.completeTime = completeTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
