package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 收货留证照片表实体
 * 现场拍照证据链：人工关联物料 / 拒收或质检不合格
 */
@TableName("receipt_evidence")
@Schema(description = "收货留证照片表实体")
public class ReceiptEvidence {

    @TableId(value = "evidence_id", type = IdType.AUTO)
    @Schema(description = "证据主键ID")
    private Long evidenceId;

    @TableField("confirmation_id")
    @Schema(description = "关联收货确认单ID")
    private Long confirmationId;

    @TableField("confirmation_item_id")
    @Schema(description = "关联收货确认明细ID")
    private Long confirmationItemId;

    @TableField("arrival_item_id")
    @Schema(description = "关联到货明细ID")
    private Long arrivalItemId;

    @TableField("evidence_type")
    @Schema(description = "证据类型：MANUAL_MATCH-人工关联 / QC_REJECT-拒收或不合格")
    private String evidenceType;

    @TableField("photo_url")
    @Schema(description = "照片访问地址")
    private String photoUrl;

    @TableField("photo_taken_at")
    @Schema(description = "照片拍摄时间（Exif）")
    private String photoTakenAt;

    @TableField("device_model")
    @Schema(description = "拍摄设备型号（Exif）")
    private String deviceModel;

    @TableField("gps")
    @Schema(description = "定位信息")
    private String gps;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public Long getConfirmationItemId() {
        return confirmationItemId;
    }

    public void setConfirmationItemId(Long confirmationItemId) {
        this.confirmationItemId = confirmationItemId;
    }

    public Long getArrivalItemId() {
        return arrivalItemId;
    }

    public void setArrivalItemId(Long arrivalItemId) {
        this.arrivalItemId = arrivalItemId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoTakenAt() {
        return photoTakenAt;
    }

    public void setPhotoTakenAt(String photoTakenAt) {
        this.photoTakenAt = photoTakenAt;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getGps() {
        return gps;
    }

    public void setGps(String gps) {
        this.gps = gps;
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
