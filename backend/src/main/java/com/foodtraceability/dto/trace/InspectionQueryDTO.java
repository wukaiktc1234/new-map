package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.foodtraceability.dto.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 检验记录查询DTO
 * 用于检验记录列表分页查询
 */
@Schema(description = "检验记录查询条件")
public class InspectionQueryDTO extends PageQuery {

    /** 检验编号 */
    @Schema(description = "检验编号")
    private String inspectionNo;

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

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验类型：INCOMING/PROCESS/FINAL */
    @Schema(description = "检验类型")
    private String inspectionType;

    /** 检验结果：QUALIFIED/UNQUALIFIED/CONDITIONAL */
    @Schema(description = "检验结果")
    private String inspectionResult;

    /** 检验员姓名 */
    @Schema(description = "检验员姓名")
    private String inspectorName;

    /** 查询开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "查询开始时间")
    private LocalDateTime startTime;

    /** 查询结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "查询结束时间")
    private LocalDateTime endTime;

    // Getter和Setter方法

    public String getInspectionNo() { return inspectionNo; }
    public void setInspectionNo(String inspectionNo) { this.inspectionNo = inspectionNo; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getInspectionType() { return inspectionType; }
    public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
    public String getInspectionResult() { return inspectionResult; }
    public void setInspectionResult(String inspectionResult) { this.inspectionResult = inspectionResult; }
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
