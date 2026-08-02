package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 质量记录VO - 前端展示对象
 * 包含质量记录所有字段及中文名称映射
 */
@Schema(description = "质量记录信息")
public class QualityRecordVO {

    /** 质量记录ID */
    @Schema(description = "质量记录ID")
    private Long qualityRecordId;

    /** 记录编号 */
    @Schema(description = "记录编号")
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

    /** 关联标准名称 */
    @Schema(description = "关联标准名称")
    private String standardName;

    /** 物料ID */
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验数据JSON对象 */
    @Schema(description = "检验数据JSON对象")
    private String inspectionData;

    /** 异常级别：NORMAL/WARNING/CRITICAL */
    @Schema(description = "异常级别")
    private String abnormalLevel;

    /** 异常级别中文名 */
    @Schema(description = "异常级别中文名")
    private String abnormalLevelName;

    /** 处理状态：PENDING/PROCESSING/RESOLVED/CLOSED */
    @Schema(description = "处理状态")
    private String handlingStatus;

    /** 处理状态中文名 */
    @Schema(description = "处理状态中文名")
    private String handlingStatusName;

    /** 处理结果 */
    @Schema(description = "处理结果")
    private String handlingResult;

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

    /** 根本原因 */
    @Schema(description = "根本原因")
    private String rootCause;

    /** 受影响追溯码列表（JSON数组） */
    @Schema(description = "受影响追溯码列表（JSON数组）")
    private String affectedTraceCodes;

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

    public Long getQualityRecordId() { return qualityRecordId; }
    public void setQualityRecordId(Long qualityRecordId) { this.qualityRecordId = qualityRecordId; }
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
    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getInspectionData() { return inspectionData; }
    public void setInspectionData(String inspectionData) { this.inspectionData = inspectionData; }
    public String getAbnormalLevel() { return abnormalLevel; }
    public void setAbnormalLevel(String abnormalLevel) { this.abnormalLevel = abnormalLevel; }
    public String getAbnormalLevelName() { return abnormalLevelName; }
    public void setAbnormalLevelName(String abnormalLevelName) { this.abnormalLevelName = abnormalLevelName; }
    public String getHandlingStatus() { return handlingStatus; }
    public void setHandlingStatus(String handlingStatus) { this.handlingStatus = handlingStatus; }
    public String getHandlingStatusName() { return handlingStatusName; }
    public void setHandlingStatusName(String handlingStatusName) { this.handlingStatusName = handlingStatusName; }
    public String getHandlingResult() { return handlingResult; }
    public void setHandlingResult(String handlingResult) { this.handlingResult = handlingResult; }
    public Long getHandledById() { return handledById; }
    public void setHandledById(Long handledById) { this.handledById = handledById; }
    public String getHandledByName() { return handledByName; }
    public void setHandledByName(String handledByName) { this.handledByName = handledByName; }
    public LocalDateTime getHandledTime() { return handledTime; }
    public void setHandledTime(LocalDateTime handledTime) { this.handledTime = handledTime; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getAffectedTraceCodes() { return affectedTraceCodes; }
    public void setAffectedTraceCodes(String affectedTraceCodes) { this.affectedTraceCodes = affectedTraceCodes; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
