package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 检验记录VO - 前端展示对象
 * 包含检验记录所有展示字段及中文名称映射
 */
@Schema(description = "检验记录信息")
public class InspectionVO {

    /** 检验记录ID */
    @Schema(description = "检验记录ID")
    private Long inspectionId;

    /** 检验编号 */
    @Schema(description = "检验编号")
    private String inspectionNo;

    /** 关联追溯码ID */
    @Schema(description = "关联追溯码ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 物料ID */
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验类型：INCOMING/PROCESS/FINAL */
    @Schema(description = "检验类型")
    private String inspectionType;

    /** 检验类型中文名 */
    @Schema(description = "检验类型中文名")
    private String inspectionTypeName;

    /** 检验结果：QUALIFIED/UNQUALIFIED/CONDITIONAL */
    @Schema(description = "检验结果")
    private String inspectionResult;

    /** 检验结果中文名 */
    @Schema(description = "检验结果中文名")
    private String inspectionResultName;

    /** 检验项目 */
    @Schema(description = "检验项目")
    private String inspectionItem;

    /** 检验值 */
    @Schema(description = "检验值")
    private String inspectionValue;

    /** 标准值 */
    @Schema(description = "标准值")
    private String standardValue;

    /** 检验单位 */
    @Schema(description = "检验单位")
    private String inspectionUnit;

    /** 检验员ID */
    @Schema(description = "检验员ID")
    private Long inspectorId;

    /** 检验员姓名 */
    @Schema(description = "检验员姓名")
    private String inspectorName;

    /** 检验时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "检验时间")
    private LocalDateTime inspectionTime;

    /** 检验地点 */
    @Schema(description = "检验地点")
    private String inspectionLocation;

    /** 检验报告URL */
    @Schema(description = "检验报告URL")
    private String reportUrl;

    /** 检验项明细（JSON数组字符串） */
    @Schema(description = "检验项明细（JSON数组字符串）")
    private String inspectionItems;

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

    public Long getInspectionId() { return inspectionId; }
    public void setInspectionId(Long inspectionId) { this.inspectionId = inspectionId; }
    public String getInspectionNo() { return inspectionNo; }
    public void setInspectionNo(String inspectionNo) { this.inspectionNo = inspectionNo; }
    public Long getTraceCodeId() { return traceCodeId; }
    public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
    public String getInspectionTypeName() { return inspectionTypeName; }
    public void setInspectionTypeName(String inspectionTypeName) { this.inspectionTypeName = inspectionTypeName; }
    public String getInspectionResult() { return inspectionResult; }
    public void setInspectionResult(String inspectionResult) { this.inspectionResult = inspectionResult; }
    public String getInspectionResultName() { return inspectionResultName; }
    public void setInspectionResultName(String inspectionResultName) { this.inspectionResultName = inspectionResultName; }
    public String getInspectionItem() { return inspectionItem; }
    public void setInspectionItem(String inspectionItem) { this.inspectionItem = inspectionItem; }
    public String getInspectionValue() { return inspectionValue; }
    public void setInspectionValue(String inspectionValue) { this.inspectionValue = inspectionValue; }
    public String getStandardValue() { return standardValue; }
    public void setStandardValue(String standardValue) { this.standardValue = standardValue; }
    public String getInspectionUnit() { return inspectionUnit; }
    public void setInspectionUnit(String inspectionUnit) { this.inspectionUnit = inspectionUnit; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public LocalDateTime getInspectionTime() { return inspectionTime; }
    public void setInspectionTime(LocalDateTime inspectionTime) { this.inspectionTime = inspectionTime; }
    public String getInspectionLocation() { return inspectionLocation; }
    public void setInspectionLocation(String inspectionLocation) { this.inspectionLocation = inspectionLocation; }
    public String getReportUrl() { return reportUrl; }
    public void setReportUrl(String reportUrl) { this.reportUrl = reportUrl; }
    public String getInspectionItems() { return inspectionItems; }
    public void setInspectionItems(String inspectionItems) { this.inspectionItems = inspectionItems; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
