package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商追溯VO - 前端展示对象
 * 用于展示供应商的批次、检验、召回等追溯信息汇总
 */
@Schema(description = "供应商追溯信息")
public class SupplierTraceVO {

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 总批次数 */
    @Schema(description = "总批次数")
    private Integer totalBatches;

    /** 总检验次数 */
    @Schema(description = "总检验次数")
    private Integer totalInspections;

    /** 合格率（百分比） */
    @Schema(description = "合格率（百分比）")
    private BigDecimal qualificationRate;

    /** 召回次数 */
    @Schema(description = "召回次数")
    private Integer totalRecalls;

    /** 批次列表 */
    @Schema(description = "批次列表")
    private List<SupplierBatchVO> batches;

    /** 检验记录列表 */
    @Schema(description = "检验记录列表")
    private List<InspectionVO> inspections;

    /** 召回记录列表 */
    @Schema(description = "召回记录列表")
    private List<RecallQueryResultVO> recalls;

    // Getter和Setter方法

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Integer getTotalBatches() { return totalBatches; }
    public void setTotalBatches(Integer totalBatches) { this.totalBatches = totalBatches; }
    public Integer getTotalInspections() { return totalInspections; }
    public void setTotalInspections(Integer totalInspections) { this.totalInspections = totalInspections; }
    public BigDecimal getQualificationRate() { return qualificationRate; }
    public void setQualificationRate(BigDecimal qualificationRate) { this.qualificationRate = qualificationRate; }
    public Integer getTotalRecalls() { return totalRecalls; }
    public void setTotalRecalls(Integer totalRecalls) { this.totalRecalls = totalRecalls; }
    public List<SupplierBatchVO> getBatches() { return batches; }
    public void setBatches(List<SupplierBatchVO> batches) { this.batches = batches; }
    public List<InspectionVO> getInspections() { return inspections; }
    public void setInspections(List<InspectionVO> inspections) { this.inspections = inspections; }
    public List<RecallQueryResultVO> getRecalls() { return recalls; }
    public void setRecalls(List<RecallQueryResultVO> recalls) { this.recalls = recalls; }
}
