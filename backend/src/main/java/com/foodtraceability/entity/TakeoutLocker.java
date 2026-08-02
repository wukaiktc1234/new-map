package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 外卖取餐柜实体类
 * @author example
 * @since 2026-01-08
 */
@TableName("takeout_locker")
public class TakeoutLocker {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 取餐柜编号
     */
    @TableField("locker_code")
    private String lockerCode;
    /**
     * 取餐柜名称
     */
    @TableField("locker_name")
    private String lockerName;
    /**
     * 取餐柜类型：STANDARD（标准型）、PREMIUM（高级型）、CUSTOM（定制型）
     */
    @TableField("locker_type")
    private String lockerType;
    /**
     * 设备IP地址
     */
    @TableField("device_ip")
    private String deviceIp;
    /**
     * 设备端口
     */
    @TableField("device_port")
    private Integer devicePort;
    /**
     * 取餐柜状态：ONLINE（在线）、OFFLINE（离线）、MAINTENANCE（维护中）
     */
    @TableField("status")
    private String status;
    /**
     * 连接方式：TCP（网络）、SERIAL（串口）、BLUETOOTH（蓝牙）
     */
    @TableField("connection_type")
    private String connectionType;
    /**
     * 总格子数
     */
    @TableField("total_slots")
    private Integer totalSlots;
    /**
     * 可用格子数
     */
    @TableField("available_slots")
    private Integer availableSlots;
    /**
     * 取餐柜位置
     */
    @TableField("location")
    private String location;
    /**
     * 最后操作时间
     */
    @TableField("last_operation_time")
    private LocalDateTime lastOperationTime;
    /**
     * 今日使用次数
     */
    @TableField("today_usage_count")
    private Integer todayUsageCount;
    /**
     * 管理员
     */
    @TableField("manager")
    private String manager;
    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLockerCode() {
        return lockerCode;
    }

    public void setLockerCode(String lockerCode) {
        this.lockerCode = lockerCode;
    }

    public String getLockerName() {
        return lockerName;
    }

    public void setLockerName(String lockerName) {
        this.lockerName = lockerName;
    }

    public String getLockerType() {
        return lockerType;
    }

    public void setLockerType(String lockerType) {
        this.lockerType = lockerType;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public Integer getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(Integer devicePort) {
        this.devicePort = devicePort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLastOperationTime() {
        return lastOperationTime;
    }

    public void setLastOperationTime(LocalDateTime lastOperationTime) {
        this.lastOperationTime = lastOperationTime;
    }

    public Integer getTodayUsageCount() {
        return todayUsageCount;
    }

    public void setTodayUsageCount(Integer todayUsageCount) {
        this.todayUsageCount = todayUsageCount;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public TakeoutLocker() {
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TakeoutLocker(id=" + this.getId() + ", lockerCode=" + this.getLockerCode() + ", lockerName=" + this.getLockerName() + ", lockerType=" + this.getLockerType() + ", deviceIp=" + this.getDeviceIp() + ", devicePort=" + this.getDevicePort() + ", status=" + this.getStatus() + ", connectionType=" + this.getConnectionType() + ", totalSlots=" + this.getTotalSlots() + ", availableSlots=" + this.getAvailableSlots() + ", location=" + this.getLocation() + ", lastOperationTime=" + this.getLastOperationTime() + ", todayUsageCount=" + this.getTodayUsageCount() + ", manager=" + this.getManager() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TakeoutLocker)) return false;
        final TakeoutLocker other = (TakeoutLocker) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$devicePort = this.getDevicePort();
        final java.lang.Object other$devicePort = other.getDevicePort();
        if (this$devicePort == null ? other$devicePort != null : !this$devicePort.equals(other$devicePort)) return false;
        final java.lang.Object this$totalSlots = this.getTotalSlots();
        final java.lang.Object other$totalSlots = other.getTotalSlots();
        if (this$totalSlots == null ? other$totalSlots != null : !this$totalSlots.equals(other$totalSlots)) return false;
        final java.lang.Object this$availableSlots = this.getAvailableSlots();
        final java.lang.Object other$availableSlots = other.getAvailableSlots();
        if (this$availableSlots == null ? other$availableSlots != null : !this$availableSlots.equals(other$availableSlots)) return false;
        final java.lang.Object this$todayUsageCount = this.getTodayUsageCount();
        final java.lang.Object other$todayUsageCount = other.getTodayUsageCount();
        if (this$todayUsageCount == null ? other$todayUsageCount != null : !this$todayUsageCount.equals(other$todayUsageCount)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$lockerCode = this.getLockerCode();
        final java.lang.Object other$lockerCode = other.getLockerCode();
        if (this$lockerCode == null ? other$lockerCode != null : !this$lockerCode.equals(other$lockerCode)) return false;
        final java.lang.Object this$lockerName = this.getLockerName();
        final java.lang.Object other$lockerName = other.getLockerName();
        if (this$lockerName == null ? other$lockerName != null : !this$lockerName.equals(other$lockerName)) return false;
        final java.lang.Object this$lockerType = this.getLockerType();
        final java.lang.Object other$lockerType = other.getLockerType();
        if (this$lockerType == null ? other$lockerType != null : !this$lockerType.equals(other$lockerType)) return false;
        final java.lang.Object this$deviceIp = this.getDeviceIp();
        final java.lang.Object other$deviceIp = other.getDeviceIp();
        if (this$deviceIp == null ? other$deviceIp != null : !this$deviceIp.equals(other$deviceIp)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$connectionType = this.getConnectionType();
        final java.lang.Object other$connectionType = other.getConnectionType();
        if (this$connectionType == null ? other$connectionType != null : !this$connectionType.equals(other$connectionType)) return false;
        final java.lang.Object this$location = this.getLocation();
        final java.lang.Object other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) return false;
        final java.lang.Object this$lastOperationTime = this.getLastOperationTime();
        final java.lang.Object other$lastOperationTime = other.getLastOperationTime();
        if (this$lastOperationTime == null ? other$lastOperationTime != null : !this$lastOperationTime.equals(other$lastOperationTime)) return false;
        final java.lang.Object this$manager = this.getManager();
        final java.lang.Object other$manager = other.getManager();
        if (this$manager == null ? other$manager != null : !this$manager.equals(other$manager)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TakeoutLocker;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $devicePort = this.getDevicePort();
        result = result * PRIME + ($devicePort == null ? 43 : $devicePort.hashCode());
        final java.lang.Object $totalSlots = this.getTotalSlots();
        result = result * PRIME + ($totalSlots == null ? 43 : $totalSlots.hashCode());
        final java.lang.Object $availableSlots = this.getAvailableSlots();
        result = result * PRIME + ($availableSlots == null ? 43 : $availableSlots.hashCode());
        final java.lang.Object $todayUsageCount = this.getTodayUsageCount();
        result = result * PRIME + ($todayUsageCount == null ? 43 : $todayUsageCount.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $lockerCode = this.getLockerCode();
        result = result * PRIME + ($lockerCode == null ? 43 : $lockerCode.hashCode());
        final java.lang.Object $lockerName = this.getLockerName();
        result = result * PRIME + ($lockerName == null ? 43 : $lockerName.hashCode());
        final java.lang.Object $lockerType = this.getLockerType();
        result = result * PRIME + ($lockerType == null ? 43 : $lockerType.hashCode());
        final java.lang.Object $deviceIp = this.getDeviceIp();
        result = result * PRIME + ($deviceIp == null ? 43 : $deviceIp.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $connectionType = this.getConnectionType();
        result = result * PRIME + ($connectionType == null ? 43 : $connectionType.hashCode());
        final java.lang.Object $location = this.getLocation();
        result = result * PRIME + ($location == null ? 43 : $location.hashCode());
        final java.lang.Object $lastOperationTime = this.getLastOperationTime();
        result = result * PRIME + ($lastOperationTime == null ? 43 : $lastOperationTime.hashCode());
        final java.lang.Object $manager = this.getManager();
        result = result * PRIME + ($manager == null ? 43 : $manager.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        return result;
    }
}
