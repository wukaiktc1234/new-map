package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 临期预警VO - 前端展示对象
 * 包含临期预警所有字段及中文名称映射
 */
@Schema(description = "临期预警信息")
public class ExpiryAlertVO {

    /** 预警记录ID */
    @Schema(description = "预警记录ID")
    private Long alertId;

    /** 追溯码ID */
    @Schema(description = "追溯码ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 追溯类型：MATERIAL原料/FOOD食品 */
    @Schema(description = "追溯类型")
    private String traceType;

    /** 追溯类型中文名 */
    @Schema(description = "追溯类型中文名")
    private String traceTypeName;

    /** 物料ID */
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 生产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 过期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    /** 剩余天数（可为负数，表示已过期） */
    @Schema(description = "剩余天数（可为负数）")
    private Integer remainingDays;

    /** 预警级别：RED红/YELLOW黄/GREEN绿 */
    @Schema(description = "预警级别")
    private String alertLevel;

    /** 预警级别中文名：红色/黄色/绿色 */
    @Schema(description = "预警级别中文名")
    private String alertLevelName;

    /** 处理状态：PENDING/SCRAPPED/RETURNED/RESOLVED */
    @Schema(description = "处理状态")
    private String handlingStatus;

    /** 处理状态中文名 */
    @Schema(description = "处理状态中文名")
    private String handlingStatusName;

    /** 处理动作 */
    @Schema(description = "处理动作")
    private String handlingAction;

    /** 处理人ID */
    @Schema(description = "处理人ID")
    private Long handledById;

    /** 处理人姓名 */
    @Schema(description = "处理人姓名")
    private String handledByName;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private LocalDateTime handledTime;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // Getter和Setter方法

    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public Long getTraceCodeId() { return traceCodeId; }
    public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getTraceType() { return traceType; }
    public void setTraceType(String traceType) { this.traceType = traceType; }
    public String getTraceTypeName() { return traceTypeName; }
    public void setTraceTypeName(String traceTypeName) { this.traceTypeName = traceTypeName; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Integer remainingDays) { this.remainingDays = remainingDays; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getAlertLevelName() { return alertLevelName; }
    public void setAlertLevelName(String alertLevelName) { this.alertLevelName = alertLevelName; }
    public String getHandlingStatus() { return handlingStatus; }
    public void setHandlingStatus(String handlingStatus) { this.handlingStatus = handlingStatus; }
    public String getHandlingStatusName() { return handlingStatusName; }
    public void setHandlingStatusName(String handlingStatusName) { this.handlingStatusName = handlingStatusName; }
    public String getHandlingAction() { return handlingAction; }
    public void setHandlingAction(String handlingAction) { this.handlingAction = handlingAction; }
    public Long getHandledById() { return handledById; }
    public void setHandledById(Long handledById) { this.handledById = handledById; }
    public String getHandledByName() { return handledByName; }
    public void setHandledByName(String handledByName) { this.handledByName = handledByName; }
    public LocalDateTime getHandledTime() { return handledTime; }
    public void setHandledTime(LocalDateTime handledTime) { this.handledTime = handledTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
