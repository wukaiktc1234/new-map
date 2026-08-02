package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 临期预警记录实体类
 * 记录物料/食品的临期预警及处理全过程
 */
@TableName("expiry_alert_record")
@Schema(description = "临期预警记录实体")
public class ExpiryAlertRecord {

    /** 主键ID */
    @TableId(value = "alert_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long alertId;

    /** 追溯码ID */
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID")
    private Long traceCodeId;

    /** 追溯码（冗余） */
    @TableField("trace_code")
    @Schema(description = "追溯码")
    private String traceCode;

    /** 追溯类型：MATERIAL原料/FOOD食品 */
    @TableField("trace_type")
    @Schema(description = "追溯类型", example = "MATERIAL")
    private String traceType;

    /** 物料ID */
    @TableField("material_id")
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;

    /** 批次号 */
    @TableField("batch_no")
    @Schema(description = "批次号")
    private String batchNo;

    /** 供应商ID */
    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @TableField("supplier_name")
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 生产日期 */
    @TableField("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 过期日期 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    /** 剩余天数（可为负数，表示已过期） */
    @TableField("remaining_days")
    @Schema(description = "剩余天数（可为负数）")
    private Integer remainingDays;

    /** 预警级别：RED红/YELLOW黄/GREEN绿 */
    @TableField("alert_level")
    @Schema(description = "预警级别", example = "YELLOW")
    private String alertLevel;

    /** 处理状态：PENDING待处理/SCRAPPED已报损/RETURNED已退货/RESOLVED已处理 */
    @TableField("handling_status")
    @Schema(description = "处理状态", example = "PENDING")
    private String handlingStatus;

    /** 处理动作 */
    @TableField("handling_action")
    @Schema(description = "处理动作")
    private String handlingAction;

    /** 处理人ID */
    @TableField("handled_by_id")
    @Schema(description = "处理人ID")
    private Long handledById;

    /** 处理人姓名 */
    @TableField("handled_by_name")
    @Schema(description = "处理人姓名")
    private String handledByName;

    /** 处理时间 */
    @TableField("handled_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getTraceCodeId() {
        return traceCodeId;
    }

    public void setTraceCodeId(Long traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public String getTraceCode() {
        return traceCode;
    }

    public void setTraceCode(String traceCode) {
        this.traceCode = traceCode;
    }

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Integer getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(Integer remainingDays) {
        this.remainingDays = remainingDays;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getHandlingStatus() {
        return handlingStatus;
    }

    public void setHandlingStatus(String handlingStatus) {
        this.handlingStatus = handlingStatus;
    }

    public String getHandlingAction() {
        return handlingAction;
    }

    public void setHandlingAction(String handlingAction) {
        this.handlingAction = handlingAction;
    }

    public Long getHandledById() {
        return handledById;
    }

    public void setHandledById(Long handledById) {
        this.handledById = handledById;
    }

    public String getHandledByName() {
        return handledByName;
    }

    public void setHandledByName(String handledByName) {
        this.handledByName = handledByName;
    }

    public LocalDateTime getHandledTime() {
        return handledTime;
    }

    public void setHandledTime(LocalDateTime handledTime) {
        this.handledTime = handledTime;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
