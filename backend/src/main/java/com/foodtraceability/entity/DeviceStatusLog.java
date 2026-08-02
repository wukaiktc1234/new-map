package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 设备状态日志实体类
 * 对应数据库表 device_status_logs
 */
@TableName("device_status_logs")
@Schema(description = "设备状态日志实体")
public class DeviceStatusLog {

    /** 日志ID */
    @TableId(value = "log_id", type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long logId;

    /** 设备ID */
    @TableField("device_id")
    @Schema(description = "设备ID")
    private Long deviceId;

    /** 原状态 */
    @TableField("old_status")
    @Schema(description = "原状态")
    private Integer oldStatus;

    /** 新状态 */
    @TableField("new_status")
    @Schema(description = "新状态")
    private Integer newStatus;

    /** 事件类型：1上线 2离线 3故障 4恢复 5配置变更 */
    @TableField("event_type")
    @Schema(description = "事件类型")
    private Integer eventType;

    /** 日志消息 */
    @TableField("message")
    @Schema(description = "日志消息")
    private String message;

    /** 额外数据JSON */
    @TableField("extra_data")
    @Schema(description = "额外数据")
    private String extraData;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Integer oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Integer getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Integer newStatus) {
        this.newStatus = newStatus;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExtraData() {
        return extraData;
    }

    public void setExtraData(String extraData) {
        this.extraData = extraData;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
