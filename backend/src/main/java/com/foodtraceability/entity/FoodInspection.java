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
 * 检验记录实体类
 * 记录入库检验、加工检验、成品检验等全流程检验数据
 */
@TableName("food_inspection")
@Schema(description = "检验记录实体")
public class FoodInspection {

    /** 主键ID */
    @TableId(value = "inspection_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long inspectionId;

    /** 检验编号（唯一） */
    @TableField("inspection_no")
    @Schema(description = "检验编号", example = "INS20260624001")
    private String inspectionNo;

    /** 关联追溯码ID */
    @TableField("trace_code_id")
    @Schema(description = "关联追溯码ID")
    private Long traceCodeId;

    /** 追溯码（冗余便于查询） */
    @TableField("trace_code")
    @Schema(description = "追溯码")
    private String traceCode;

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

    /** 物料ID */
    @TableField("material_id")
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;

    /** 检验类型：INCOMING入库检验/PROCESS加工检验/FINAL成品检验 */
    @TableField("inspection_type")
    @Schema(description = "检验类型", example = "INCOMING")
    private String inspectionType;

    /** 检验结果：QUALIFIED合格/UNQUALIFIED不合格/CONDITIONAL有条件合格 */
    @TableField("inspection_result")
    @Schema(description = "检验结果", example = "QUALIFIED")
    private String inspectionResult;

    /** 检验项目 */
    @TableField("inspection_item")
    @Schema(description = "检验项目")
    private String inspectionItem;

    /** 检验值 */
    @TableField("inspection_value")
    @Schema(description = "检验值")
    private String inspectionValue;

    /** 标准值 */
    @TableField("standard_value")
    @Schema(description = "标准值")
    private String standardValue;

    /** 检验单位 */
    @TableField("inspection_unit")
    @Schema(description = "检验单位")
    private String inspectionUnit;

    /** 检验员ID */
    @TableField("inspector_id")
    @Schema(description = "检验员ID")
    private Long inspectorId;

    /** 检验员姓名 */
    @TableField("inspector_name")
    @Schema(description = "检验员姓名")
    private String inspectorName;

    /** 检验时间 */
    @TableField("inspection_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "检验时间")
    private LocalDateTime inspectionTime;

    /** 检验地点 */
    @TableField("inspection_location")
    @Schema(description = "检验地点")
    private String inspectionLocation;

    /** 检验报告URL */
    @TableField("report_url")
    @Schema(description = "检验报告URL")
    private String reportUrl;

    /** 检验项明细（JSON数组） */
    @TableField("inspection_items")
    @Schema(description = "检验项明细（JSON数组）")
    private String inspectionItems;

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

    public Long getInspectionId() {
        return inspectionId;
    }

    public void setInspectionId(Long inspectionId) {
        this.inspectionId = inspectionId;
    }

    public String getInspectionNo() {
        return inspectionNo;
    }

    public void setInspectionNo(String inspectionNo) {
        this.inspectionNo = inspectionNo;
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

    public String getInspectionType() {
        return inspectionType;
    }

    public void setInspectionType(String inspectionType) {
        this.inspectionType = inspectionType;
    }

    public String getInspectionResult() {
        return inspectionResult;
    }

    public void setInspectionResult(String inspectionResult) {
        this.inspectionResult = inspectionResult;
    }

    public String getInspectionItem() {
        return inspectionItem;
    }

    public void setInspectionItem(String inspectionItem) {
        this.inspectionItem = inspectionItem;
    }

    public String getInspectionValue() {
        return inspectionValue;
    }

    public void setInspectionValue(String inspectionValue) {
        this.inspectionValue = inspectionValue;
    }

    public String getStandardValue() {
        return standardValue;
    }

    public void setStandardValue(String standardValue) {
        this.standardValue = standardValue;
    }

    public String getInspectionUnit() {
        return inspectionUnit;
    }

    public void setInspectionUnit(String inspectionUnit) {
        this.inspectionUnit = inspectionUnit;
    }

    public Long getInspectorId() {
        return inspectorId;
    }

    public void setInspectorId(Long inspectorId) {
        this.inspectorId = inspectorId;
    }

    public String getInspectorName() {
        return inspectorName;
    }

    public void setInspectorName(String inspectorName) {
        this.inspectorName = inspectorName;
    }

    public LocalDateTime getInspectionTime() {
        return inspectionTime;
    }

    public void setInspectionTime(LocalDateTime inspectionTime) {
        this.inspectionTime = inspectionTime;
    }

    public String getInspectionLocation() {
        return inspectionLocation;
    }

    public void setInspectionLocation(String inspectionLocation) {
        this.inspectionLocation = inspectionLocation;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getInspectionItems() {
        return inspectionItems;
    }

    public void setInspectionItems(String inspectionItems) {
        this.inspectionItems = inspectionItems;
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
