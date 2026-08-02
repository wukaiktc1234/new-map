package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 打印任务查询DTO
 */
@Schema(description = "打印任务查询条件")
public class PrintTaskQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务编号（模糊查询） */
    @Schema(description = "任务编号")
    private String taskCode;

    /** 目标打印机ID */
    @Schema(description = "目标打印机ID")
    private Long deviceId;

    /** 任务类型 */
    @Schema(description = "任务类型")
    private Integer taskType;

    /** 打印状态：0待打印 1打印中 2已完成 3失败 */
    @Schema(description = "打印状态")
    private Integer printStatus;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    // ==================== Getter & Setter 方法 ====================

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public Integer getTaskType() { return taskType; }
    public void setTaskType(Integer taskType) { this.taskType = taskType; }

    public Integer getPrintStatus() { return printStatus; }
    public void setPrintStatus(Integer printStatus) { this.printStatus = printStatus; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
