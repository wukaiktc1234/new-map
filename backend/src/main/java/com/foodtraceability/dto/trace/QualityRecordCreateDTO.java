package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 质量记录创建DTO
 * 用于创建质量检测数据及异常处理记录
 */
@Schema(description = "质量记录创建请求")
public class QualityRecordCreateDTO {

    /** 记录编号（自动生成可省略） */
    @Schema(description = "记录编号", example = "QR20260624001")
    private String recordNo;

    /** 关联追溯码ID */
    @Schema(description = "关联追溯码ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 关联质量标准ID */
    @Schema(description = "关联质量标准ID")
    private Long standardId;

    /** 物料ID */
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @NotBlank(message = "物料名称不能为空")
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验数据JSON对象 */
    @NotBlank(message = "检验数据不能为空")
    @Schema(description = "检验数据JSON对象")
    private String inspectionData;

    /** 异常级别：NORMAL正常/WARNING警告/CRITICAL严重 */
    @NotBlank(message = "异常级别不能为空")
    @Schema(description = "异常级别", example = "NORMAL")
    private String abnormalLevel;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // Getter和Setter方法

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public Long getTraceCodeId() { return traceCodeId; }
    public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getStandardId() { return standardId; }
    public void setStandardId(Long standardId) { this.standardId = standardId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getInspectionData() { return inspectionData; }
    public void setInspectionData(String inspectionData) { this.inspectionData = inspectionData; }
    public String getAbnormalLevel() { return abnormalLevel; }
    public void setAbnormalLevel(String abnormalLevel) { this.abnormalLevel = abnormalLevel; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
