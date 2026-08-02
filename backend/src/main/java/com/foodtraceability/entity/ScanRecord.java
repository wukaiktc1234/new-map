package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 扫码记录实体类
 * @author example
 * @since 2026-01-08
 */
@TableName("scan_record")
public class ScanRecord {
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 扫描的代码
     */
    @TableField("scan_code")
    private String scanCode;
    /**
     * 代码类型：TRACE_CODE（追溯码）、PRODUCT_CODE（产品码）、ORDER_CODE（订单码）
     */
    @TableField("code_type")
    private String codeType;
    /**
     * 设备ID
     */
    @TableField("device_id")
    private Long deviceId;
    /**
     * 设备编号
     */
    @TableField("device_code")
    private String deviceCode;
    /**
     * 扫码时间
     */
    @TableField("scan_time")
    private LocalDateTime scanTime;
    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;
    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Long operatorId;
    /**
     * 扫码位置
     */
    @TableField("scan_location")
    private String scanLocation;
    /**
     * 扫码结果：SUCCESS（成功）、FAILED（失败）、INVALID_CODE（无效代码）
     */
    @TableField("scan_result")
    private String scanResult;
    /**
     * 失败原因
     */
    @TableField("failure_reason")
    private String failureReason;
    /**
     * 业务类型：OUTBOUND（出库）、INBOUND（入库）、CHECK（盘点）、SALES（销售）
     */
    @TableField("business_type")
    private String businessType;
    /**
     * 关联业务ID
     */
    @TableField("business_id")
    private Long businessId;
    /**
     * 处理状态：PENDING（待处理）、PROCESSED（已处理）、ERROR（处理错误）
     */
    @TableField("process_status")
    private String processStatus;
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

    public String getScanCode() {
        return scanCode;
    }

    public void setScanCode(String scanCode) {
        this.scanCode = scanCode;
    }

    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public LocalDateTime getScanTime() {
        return scanTime;
    }

    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getScanLocation() {
        return scanLocation;
    }

    public void setScanLocation(String scanLocation) {
        this.scanLocation = scanLocation;
    }

    public String getScanResult() {
        return scanResult;
    }

    public void setScanResult(String scanResult) {
        this.scanResult = scanResult;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(String processStatus) {
        this.processStatus = processStatus;
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

    public ScanRecord() {
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScanRecord(id=" + this.getId() + ", scanCode=" + this.getScanCode() + ", codeType=" + this.getCodeType() + ", deviceId=" + this.getDeviceId() + ", deviceCode=" + this.getDeviceCode() + ", scanTime=" + this.getScanTime() + ", operator=" + this.getOperator() + ", operatorId=" + this.getOperatorId() + ", scanLocation=" + this.getScanLocation() + ", scanResult=" + this.getScanResult() + ", failureReason=" + this.getFailureReason() + ", businessType=" + this.getBusinessType() + ", businessId=" + this.getBusinessId() + ", processStatus=" + this.getProcessStatus() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScanRecord)) return false;
        final ScanRecord other = (ScanRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$deviceId = this.getDeviceId();
        final java.lang.Object other$deviceId = other.getDeviceId();
        if (this$deviceId == null ? other$deviceId != null : !this$deviceId.equals(other$deviceId)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$scanCode = this.getScanCode();
        final java.lang.Object other$scanCode = other.getScanCode();
        if (this$scanCode == null ? other$scanCode != null : !this$scanCode.equals(other$scanCode)) return false;
        final java.lang.Object this$codeType = this.getCodeType();
        final java.lang.Object other$codeType = other.getCodeType();
        if (this$codeType == null ? other$codeType != null : !this$codeType.equals(other$codeType)) return false;
        final java.lang.Object this$deviceCode = this.getDeviceCode();
        final java.lang.Object other$deviceCode = other.getDeviceCode();
        if (this$deviceCode == null ? other$deviceCode != null : !this$deviceCode.equals(other$deviceCode)) return false;
        final java.lang.Object this$scanTime = this.getScanTime();
        final java.lang.Object other$scanTime = other.getScanTime();
        if (this$scanTime == null ? other$scanTime != null : !this$scanTime.equals(other$scanTime)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        final java.lang.Object this$scanLocation = this.getScanLocation();
        final java.lang.Object other$scanLocation = other.getScanLocation();
        if (this$scanLocation == null ? other$scanLocation != null : !this$scanLocation.equals(other$scanLocation)) return false;
        final java.lang.Object this$scanResult = this.getScanResult();
        final java.lang.Object other$scanResult = other.getScanResult();
        if (this$scanResult == null ? other$scanResult != null : !this$scanResult.equals(other$scanResult)) return false;
        final java.lang.Object this$failureReason = this.getFailureReason();
        final java.lang.Object other$failureReason = other.getFailureReason();
        if (this$failureReason == null ? other$failureReason != null : !this$failureReason.equals(other$failureReason)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        final java.lang.Object this$processStatus = this.getProcessStatus();
        final java.lang.Object other$processStatus = other.getProcessStatus();
        if (this$processStatus == null ? other$processStatus != null : !this$processStatus.equals(other$processStatus)) return false;
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
        return other instanceof ScanRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deviceId = this.getDeviceId();
        result = result * PRIME + ($deviceId == null ? 43 : $deviceId.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $scanCode = this.getScanCode();
        result = result * PRIME + ($scanCode == null ? 43 : $scanCode.hashCode());
        final java.lang.Object $codeType = this.getCodeType();
        result = result * PRIME + ($codeType == null ? 43 : $codeType.hashCode());
        final java.lang.Object $deviceCode = this.getDeviceCode();
        result = result * PRIME + ($deviceCode == null ? 43 : $deviceCode.hashCode());
        final java.lang.Object $scanTime = this.getScanTime();
        result = result * PRIME + ($scanTime == null ? 43 : $scanTime.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        final java.lang.Object $scanLocation = this.getScanLocation();
        result = result * PRIME + ($scanLocation == null ? 43 : $scanLocation.hashCode());
        final java.lang.Object $scanResult = this.getScanResult();
        result = result * PRIME + ($scanResult == null ? 43 : $scanResult.hashCode());
        final java.lang.Object $failureReason = this.getFailureReason();
        result = result * PRIME + ($failureReason == null ? 43 : $failureReason.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $processStatus = this.getProcessStatus();
        result = result * PRIME + ($processStatus == null ? 43 : $processStatus.hashCode());
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
