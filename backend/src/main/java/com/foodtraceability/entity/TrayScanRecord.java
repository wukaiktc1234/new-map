package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 托盘扫码记录实体
 * <p>每次扫码（KITCHEN_IN/KITCHEN_OUT/SERVE）全量留痕，用于防抖判断、摄像头联动、YOLO 预留</p>
 */
@TableName("tray_scan_record")
@Schema(description = "托盘扫码记录")
public class TrayScanRecord {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("tray_id")
    @Schema(description = "托盘ID")
    private Long trayId;

    @TableField("tray_code")
    @Schema(description = "托盘码", example = "TRAY001")
    private String trayCode;

    @TableField("kitchen_order_id")
    @Schema(description = "关联后厨订单ID")
    private String kitchenOrderId;

    @TableField("scan_device_id")
    @Schema(description = "扫码设备ID")
    private Long scanDeviceId;

    @TableField("scan_device_code")
    @Schema(description = "扫码设备编码")
    private String scanDeviceCode;

    @TableField("scan_type")
    @Schema(description = "扫码类型：KITCHEN_IN-后厨一次扫码, KITCHEN_OUT-后厨二次扫码, SERVE-取餐口确认")
    private String scanType;

    @TableField("from_status")
    @Schema(description = "扫码前托盘状态")
    private String fromStatus;

    @TableField("to_status")
    @Schema(description = "扫码后托盘状态")
    private String toStatus;

    @TableField("scan_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "扫码时间")
    private LocalDateTime scanTime;

    @TableField("prev_scan_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "上一次同类型扫码时间（用于防抖判断）")
    private LocalDateTime prevScanTime;

    @TableField("is_debounced")
    @Schema(description = "本次扫码是否被防抖忽略：0-正常处理, 1-被防抖忽略")
    private Integer isDebounced;

    @TableField("camera_snapshot_url")
    @Schema(description = "摄像头拍照URL（出餐时联动）")
    private String cameraSnapshotUrl;

    @TableField("yolo_result")
    @Schema(description = "YOLO识别结果（预留）：has_food-有餐, no_food-无餐, unknown-不确定")
    private String yoloResult;

    @TableField("yolo_confidence")
    @Schema(description = "YOLO识别置信度（0-1）")
    private BigDecimal yoloConfidence;

    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private Long operatorId;

    @TableField("operator_name")
    @Schema(description = "操作人姓名")
    private String operatorName;

    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;

    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记：0-未删除, 1-已删除")
    private Integer deleted;

    public TrayScanRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTrayId() {
        return this.trayId;
    }

    public String getTrayCode() {
        return this.trayCode;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public Long getScanDeviceId() {
        return this.scanDeviceId;
    }

    public String getScanDeviceCode() {
        return this.scanDeviceCode;
    }

    public String getScanType() {
        return this.scanType;
    }

    public String getFromStatus() {
        return this.fromStatus;
    }

    public String getToStatus() {
        return this.toStatus;
    }

    public LocalDateTime getScanTime() {
        return this.scanTime;
    }

    public LocalDateTime getPrevScanTime() {
        return this.prevScanTime;
    }

    public Integer getIsDebounced() {
        return this.isDebounced;
    }

    public String getCameraSnapshotUrl() {
        return this.cameraSnapshotUrl;
    }

    public String getYoloResult() {
        return this.yoloResult;
    }

    public BigDecimal getYoloConfidence() {
        return this.yoloConfidence;
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public String getOperatorName() {
        return this.operatorName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTrayId(final Long trayId) {
        this.trayId = trayId;
    }

    public void setTrayCode(final String trayCode) {
        this.trayCode = trayCode;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setScanDeviceId(final Long scanDeviceId) {
        this.scanDeviceId = scanDeviceId;
    }

    public void setScanDeviceCode(final String scanDeviceCode) {
        this.scanDeviceCode = scanDeviceCode;
    }

    public void setScanType(final String scanType) {
        this.scanType = scanType;
    }

    public void setFromStatus(final String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public void setToStatus(final String toStatus) {
        this.toStatus = toStatus;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setScanTime(final LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setPrevScanTime(final LocalDateTime prevScanTime) {
        this.prevScanTime = prevScanTime;
    }

    public void setIsDebounced(final Integer isDebounced) {
        this.isDebounced = isDebounced;
    }

    public void setCameraSnapshotUrl(final String cameraSnapshotUrl) {
        this.cameraSnapshotUrl = cameraSnapshotUrl;
    }

    public void setYoloResult(final String yoloResult) {
        this.yoloResult = yoloResult;
    }

    public void setYoloConfidence(final BigDecimal yoloConfidence) {
        this.yoloConfidence = yoloConfidence;
    }

    public void setOperatorId(final Long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorName(final String operatorName) {
        this.operatorName = operatorName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }
}
