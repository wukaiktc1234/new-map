package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@TableName("tray")
@Schema(description = "托盘实体")
public class Tray {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tray_code")
    @Schema(description = "托盘码（底部二维码）", example = "TRAY001")
    private String trayCode;
    @TableField("tray_name")
    @Schema(description = "托盘名称", example = "1号托盘")
    private String trayName;
    @TableField("tray_type")
    @Schema(description = "托盘类型：standard-标准, large-大号, small-小号")
    private String trayType;
    @TableField("status")
    @Schema(description = "5状态机：idle-空闲, bound-POS已绑定, making-后厨制作中, ready-后厨已出餐, served-已取餐, cleaning-清洁中, damaged-损坏")
    private String status;
    @TableField("current_order_id")
    @Schema(description = "当前绑定订单ID")
    private String currentOrderId;
    @TableField("current_kitchen_order_id")
    @Schema(description = "当前绑定后厨订单ID")
    private Long currentKitchenOrderId;
    @TableField("bind_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;
    @TableField("last_use_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后使用时间")
    private LocalDateTime lastUseTime;
    @TableField("use_count")
    @Schema(description = "使用次数")
    private Integer useCount;
    @TableField("store_id")
    @Schema(description = "所属门店ID")
    private Long storeId;
    @TableField("store_name")
    @Schema(description = "所属门店名称")
    private String storeName;
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    @TableField("last_state_change_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近状态变更时间（用于防抖判断）")
    private LocalDateTime lastStateChangeTime;
    @TableField("last_scan_device_id")
    @Schema(description = "最近扫码设备ID")
    private Long lastScanDeviceId;
    @TableField("last_scan_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近扫码时间（用于防抖判断）")
    private LocalDateTime lastScanTime;
    @TableField("camera_snapshot_url")
    @Schema(description = "最近摄像头拍照URL")
    private String cameraSnapshotUrl;
    @TableField("camera_snapshot_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近摄像头拍照时间")
    private LocalDateTime cameraSnapshotTime;
    @TableField("yolo_verified")
    @Schema(description = "YOLO识别结果：0-未识别, 1-有餐, 2-无餐")
    private Integer yoloVerified;
    @TableField("yolo_verify_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "YOLO识别时间")
    private LocalDateTime yoloVerifyTime;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updateBy;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记：0-未删除, 1-已删除")
    private Integer deleted;

    public Tray() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTrayCode() {
        return this.trayCode;
    }

    public String getTrayName() {
        return this.trayName;
    }

    public String getTrayType() {
        return this.trayType;
    }

    public String getStatus() {
        return this.status;
    }

    public String getCurrentOrderId() {
        return this.currentOrderId;
    }

    public Long getCurrentKitchenOrderId() {
        return this.currentKitchenOrderId;
    }

    public LocalDateTime getBindTime() {
        return this.bindTime;
    }

    public LocalDateTime getLastUseTime() {
        return this.lastUseTime;
    }

    public Integer getUseCount() {
        return this.useCount;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getLastStateChangeTime() {
        return this.lastStateChangeTime;
    }

    public Long getLastScanDeviceId() {
        return this.lastScanDeviceId;
    }

    public LocalDateTime getLastScanTime() {
        return this.lastScanTime;
    }

    public String getCameraSnapshotUrl() {
        return this.cameraSnapshotUrl;
    }

    public LocalDateTime getCameraSnapshotTime() {
        return this.cameraSnapshotTime;
    }

    public Integer getYoloVerified() {
        return this.yoloVerified;
    }

    public LocalDateTime getYoloVerifyTime() {
        return this.yoloVerifyTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTrayCode(final String trayCode) {
        this.trayCode = trayCode;
    }

    public void setTrayName(final String trayName) {
        this.trayName = trayName;
    }

    public void setTrayType(final String trayType) {
        this.trayType = trayType;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setCurrentOrderId(final String currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public void setCurrentKitchenOrderId(final Long currentKitchenOrderId) {
        this.currentKitchenOrderId = currentKitchenOrderId;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setBindTime(final LocalDateTime bindTime) {
        this.bindTime = bindTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setLastUseTime(final LocalDateTime lastUseTime) {
        this.lastUseTime = lastUseTime;
    }

    public void setUseCount(final Integer useCount) {
        this.useCount = useCount;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setLastStateChangeTime(final LocalDateTime lastStateChangeTime) {
        this.lastStateChangeTime = lastStateChangeTime;
    }

    public void setLastScanDeviceId(final Long lastScanDeviceId) {
        this.lastScanDeviceId = lastScanDeviceId;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setLastScanTime(final LocalDateTime lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    public void setCameraSnapshotUrl(final String cameraSnapshotUrl) {
        this.cameraSnapshotUrl = cameraSnapshotUrl;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCameraSnapshotTime(final LocalDateTime cameraSnapshotTime) {
        this.cameraSnapshotTime = cameraSnapshotTime;
    }

    public void setYoloVerified(final Integer yoloVerified) {
        this.yoloVerified = yoloVerified;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setYoloVerifyTime(final LocalDateTime yoloVerifyTime) {
        this.yoloVerifyTime = yoloVerifyTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Tray)) return false;
        final Tray other = (Tray) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$currentKitchenOrderId = this.getCurrentKitchenOrderId();
        final java.lang.Object other$currentKitchenOrderId = other.getCurrentKitchenOrderId();
        if (this$currentKitchenOrderId == null ? other$currentKitchenOrderId != null : !this$currentKitchenOrderId.equals(other$currentKitchenOrderId)) return false;
        final java.lang.Object this$useCount = this.getUseCount();
        final java.lang.Object other$useCount = other.getUseCount();
        if (this$useCount == null ? other$useCount != null : !this$useCount.equals(other$useCount)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$trayCode = this.getTrayCode();
        final java.lang.Object other$trayCode = other.getTrayCode();
        if (this$trayCode == null ? other$trayCode != null : !this$trayCode.equals(other$trayCode)) return false;
        final java.lang.Object this$trayName = this.getTrayName();
        final java.lang.Object other$trayName = other.getTrayName();
        if (this$trayName == null ? other$trayName != null : !this$trayName.equals(other$trayName)) return false;
        final java.lang.Object this$trayType = this.getTrayType();
        final java.lang.Object other$trayType = other.getTrayType();
        if (this$trayType == null ? other$trayType != null : !this$trayType.equals(other$trayType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$currentOrderId = this.getCurrentOrderId();
        final java.lang.Object other$currentOrderId = other.getCurrentOrderId();
        if (this$currentOrderId == null ? other$currentOrderId != null : !this$currentOrderId.equals(other$currentOrderId)) return false;
        final java.lang.Object this$bindTime = this.getBindTime();
        final java.lang.Object other$bindTime = other.getBindTime();
        if (this$bindTime == null ? other$bindTime != null : !this$bindTime.equals(other$bindTime)) return false;
        final java.lang.Object this$lastUseTime = this.getLastUseTime();
        final java.lang.Object other$lastUseTime = other.getLastUseTime();
        if (this$lastUseTime == null ? other$lastUseTime != null : !this$lastUseTime.equals(other$lastUseTime)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
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
        return other instanceof Tray;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $currentKitchenOrderId = this.getCurrentKitchenOrderId();
        result = result * PRIME + ($currentKitchenOrderId == null ? 43 : $currentKitchenOrderId.hashCode());
        final java.lang.Object $useCount = this.getUseCount();
        result = result * PRIME + ($useCount == null ? 43 : $useCount.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $trayCode = this.getTrayCode();
        result = result * PRIME + ($trayCode == null ? 43 : $trayCode.hashCode());
        final java.lang.Object $trayName = this.getTrayName();
        result = result * PRIME + ($trayName == null ? 43 : $trayName.hashCode());
        final java.lang.Object $trayType = this.getTrayType();
        result = result * PRIME + ($trayType == null ? 43 : $trayType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $currentOrderId = this.getCurrentOrderId();
        result = result * PRIME + ($currentOrderId == null ? 43 : $currentOrderId.hashCode());
        final java.lang.Object $bindTime = this.getBindTime();
        result = result * PRIME + ($bindTime == null ? 43 : $bindTime.hashCode());
        final java.lang.Object $lastUseTime = this.getLastUseTime();
        result = result * PRIME + ($lastUseTime == null ? 43 : $lastUseTime.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
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

    @java.lang.Override
    public java.lang.String toString() {
        return "Tray(id=" + this.getId() + ", trayCode=" + this.getTrayCode() + ", trayName=" + this.getTrayName() + ", trayType=" + this.getTrayType() + ", status=" + this.getStatus() + ", currentOrderId=" + this.getCurrentOrderId() + ", currentKitchenOrderId=" + this.getCurrentKitchenOrderId() + ", bindTime=" + this.getBindTime() + ", lastUseTime=" + this.getLastUseTime() + ", useCount=" + this.getUseCount() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
