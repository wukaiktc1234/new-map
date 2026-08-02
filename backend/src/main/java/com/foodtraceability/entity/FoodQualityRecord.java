package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 质量记录实体类
 * 记录质量检测数据及异常处理全过程
 */
@TableName("food_quality_record")
@Schema(description = "质量记录实体")
public class FoodQualityRecord {

    /** 主键ID */
    @TableId(value = "quality_record_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long qualityRecordId;

    /** 记录编号（唯一） */
    @TableField("record_no")
    @Schema(description = "记录编号", example = "QR20260624001")
    private String recordNo;

    /** 关联追溯码ID */
    @TableField("trace_code_id")
    @Schema(description = "关联追溯码ID")
    private Long traceCodeId;

    /** 追溯码（冗余） */
    @TableField("trace_code")
    @Schema(description = "追溯码")
    private String traceCode;

    /** 批次号 */
    @TableField("batch_no")
    @Schema(description = "批次号")
    private String batchNo;

    /** 关联质量标准ID */
    @TableField("standard_id")
    @Schema(description = "关联质量标准ID")
    private Long standardId;

    /** 冗余标准名称 */
    @TableField("standard_name")
    @Schema(description = "冗余标准名称")
    private String standardName;

    /** 物料ID */
    @TableField("material_id")
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验数据JSON对象 */
    @TableField("inspection_data")
    @Schema(description = "检验数据JSON对象")
    private String inspectionData;

    /** 异常级别：NORMAL正常/WARNING警告/CRITICAL严重 */
    @TableField("abnormal_level")
    @Schema(description = "异常级别", example = "NORMAL")
    private String abnormalLevel;

    /** 处理状态：PENDING待处理/PROCESSING处理中/RESOLVED已解决/CLOSED已关闭 */
    @TableField("handling_status")
    @Schema(description = "处理状态", example = "PENDING")
    private String handlingStatus;

    /** 处理结果 */
    @TableField("handling_result")
    @Schema(description = "处理结果")
    private String handlingResult;

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

    /** 根本原因 */
    @TableField("root_cause")
    @Schema(description = "根本原因")
    private String rootCause;

    /** 受影响追溯码列表（JSON数组） */
    @TableField("affected_trace_codes")
    @Schema(description = "受影响追溯码列表（JSON数组）")
    private String affectedTraceCodes;

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

    public Long getQualityRecordId() {
        return qualityRecordId;
    }

    public void setQualityRecordId(Long qualityRecordId) {
        this.qualityRecordId = qualityRecordId;
    }

    public String getRecordNo() {
        return recordNo;
    }

    public void setRecordNo(String recordNo) {
        this.recordNo = recordNo;
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

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getStandardId() {
        return standardId;
    }

    public void setStandardId(Long standardId) {
        this.standardId = standardId;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
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

    public String getInspectionData() {
        return inspectionData;
    }

    public void setInspectionData(String inspectionData) {
        this.inspectionData = inspectionData;
    }

    public String getAbnormalLevel() {
        return abnormalLevel;
    }

    public void setAbnormalLevel(String abnormalLevel) {
        this.abnormalLevel = abnormalLevel;
    }

    public String getHandlingStatus() {
        return handlingStatus;
    }

    public void setHandlingStatus(String handlingStatus) {
        this.handlingStatus = handlingStatus;
    }

    public String getHandlingResult() {
        return handlingResult;
    }

    public void setHandlingResult(String handlingResult) {
        this.handlingResult = handlingResult;
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

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getAffectedTraceCodes() {
        return affectedTraceCodes;
    }

    public void setAffectedTraceCodes(String affectedTraceCodes) {
        this.affectedTraceCodes = affectedTraceCodes;
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
