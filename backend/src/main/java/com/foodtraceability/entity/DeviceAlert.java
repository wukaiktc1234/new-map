package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 设备告警实体类
 * 对应数据库表 device_alerts
 */
@TableName("device_alerts")
@Schema(description = "设备告警实体")
public class DeviceAlert {

    /** 告警ID */
    @TableId(value = "alert_id", type = IdType.AUTO)
    @Schema(description = "告警ID")
    private Long alertId;

    /** 设备ID */
    @TableField("device_id")
    @Schema(description = "设备ID")
    private Long deviceId;

    /** 告警类型：1离线超时 2纸张缺 3碳带缺 4故障 5维护提醒 */
    @TableField("alert_type")
    @Schema(description = "告警类型")
    private Integer alertType;

    /** 告警级别：1信息 2警告 3严重 4紧急 */
    @TableField("alert_level")
    @Schema(description = "告警级别")
    private Integer alertLevel;

    /** 告警消息 */
    @TableField("alert_message")
    @Schema(description = "告警消息")
    private String alertMessage;

    /** 是否已处理 */
    @TableField("is_handled")
    @Schema(description = "是否已处理")
    private Boolean isHandled;

    /** 处理时间 */
    @TableField("handle_time")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    /** 处理结果 */
    @TableField("handle_result")
    @Schema(description = "处理结果")
    private String handleResult;

    /** 处理人ID */
    @TableField("handle_user_id")
    @Schema(description = "处理人ID")
    private Long handleUserId;

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

    /** 设备类型（冗余字段） */
    @TableField("device_type")
    @Schema(description = "设备类型")
    private Integer deviceType;

    /** 状态：0未处理 1处理中 2已解决 3已忽略 */
    @TableField("alert_status")
    @Schema(description = "告警状态")
    private Integer alertStatus;

    /** 触发时间 */
    @TableField("trigger_time")
    @Schema(description = "触发时间")
    private LocalDateTime triggerTime;

    /** 解决时间 */
    @TableField("resolve_time")
    @Schema(description = "解决时间")
    private LocalDateTime resolveTime;

    /** 设备状态快照 */
    @TableField("device_status_snapshot")
    @Schema(description = "设备状态快照")
    private String deviceStatusSnapshot;

    /** 设备名称（冗余） */
    @TableField("device_name")
    @Schema(description = "设备名称")
    private String deviceName;

    /** 所属门店ID（兼容性字段，非数据库映射） */
    @Schema(description = "所属门店ID", hidden = true)
    private transient Long storeId;

    /** 最后检查时间（兼容性字段，非数据库映射） */
    @Schema(description = "最后检查时间", hidden = true)
    private transient LocalDateTime lastCheckTime;

    // ==================== Getter & Setter 方法 ====================

    public Long getId() {
        return alertId;
    }

    public void setId(Long id) {
        this.alertId = id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getAlertType() {
        return alertType;
    }

    public void setAlertType(Integer alertType) {
        this.alertType = alertType;
    }

    public Integer getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(Integer alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public Boolean getIsHandled() {
        return isHandled;
    }

    public void setIsHandled(Boolean isHandled) {
        this.isHandled = isHandled;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
    }

    public Long getHandleUserId() {
        return handleUserId;
    }

    public void setHandleUserId(Long handleUserId) {
        this.handleUserId = handleUserId;
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

    public Integer getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getStatus() {
        return alertStatus;
    }

    public void setStatus(Integer status) {
        this.alertStatus = status;
    }

    /** 告警状态字段的标准 getter（用于 MyBatis Plus Lambda 引用，对应 alert_status 列） */
    public Integer getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(Integer alertStatus) {
        this.alertStatus = alertStatus;
    }

    public LocalDateTime getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(LocalDateTime triggerTime) {
        this.triggerTime = triggerTime;
    }

    public LocalDateTime getResolveTime() {
        return resolveTime;
    }

    public void setResolveTime(LocalDateTime resolveTime) {
        this.resolveTime = resolveTime;
    }

    public String getDeviceStatus() {
        return deviceStatusSnapshot;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatusSnapshot = deviceStatus;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getAlertDescription() {
        return alertMessage;
    }

    public void setAlertDescription(String alertDescription) {
        this.alertMessage = alertDescription;
    }

    // ==================== storeId / lastCheckTime 兼容性方法 ====================

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
}
