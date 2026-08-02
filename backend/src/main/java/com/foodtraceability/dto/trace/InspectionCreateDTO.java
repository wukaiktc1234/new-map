package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 检验记录创建DTO
 * 用于创建入库检验、加工检验、成品检验等记录
 */
@Schema(description = "检验记录创建请求")
public class InspectionCreateDTO {

    /** 检验编号（自动生成可省略） */
    @Schema(description = "检验编号", example = "INS20260624001")
    private String inspectionNo;

    /** 关联追溯码ID */
    @Schema(description = "关联追溯码ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 批次号 */
    @NotBlank(message = "批次号不能为空")
    @Schema(description = "批次号", example = "BATCH20260624001")
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
    @NotBlank(message = "物料名称不能为空")
    @Schema(description = "物料名称", example = "牛肉")
    private String materialName;

    /** 检验类型：INCOMING入库检验/PROCESS加工检验/FINAL成品检验 */
    @NotBlank(message = "检验类型不能为空")
    @Schema(description = "检验类型", example = "INCOMING")
    private String inspectionType;

    /** 检验结果：QUALIFIED合格/UNQUALIFIED不合格/CONDITIONAL有条件合格 */
    @NotBlank(message = "检验结果不能为空")
    @Schema(description = "检验结果", example = "QUALIFIED")
    private String inspectionResult;

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
    @NotNull(message = "检验时间不能为空")
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

    // Getter和Setter方法

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
    public String getInspectionResult() { return inspectionResult; }
    public void setInspectionResult(String inspectionResult) { this.inspectionResult = inspectionResult; }
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
}
