package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 打印任务创建DTO
 */
@Schema(description = "打印任务创建请求")
public class PrintTaskCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标打印机ID */
    @NotNull(message = "目标打印机ID不能为空")
    @Schema(description = "目标打印机ID", required = true)
    private Long deviceId;

    /** 任务类型：1小票 2标签 3报表 4厨房单 */
    @NotNull(message = "任务类型不能为空")
    @Schema(description = "任务类型", required = true)
    private Integer taskType;

    /** 打印内容JSON/HTML/template */
    @NotBlank(message = "打印内容不能为空")
    @Schema(description = "打印内容", required = true)
    private String contentJson;

    /** 最大重试次数 */
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    // ==================== Getter & Setter 方法 ====================

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }

    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }

    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
}
