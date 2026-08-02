package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 召回分析查询DTO
 * 用于反向召回影响范围分析查询
 */
@Schema(description = "召回分析查询请求")
public class RecallAnalyzeQueryDTO {

    /** 批次号（可选） */
    @Schema(description = "批次号", example = "BATCH20260420")
    private String batchNo;

    /** 供应商ID（可选） */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 目标名称（可选，模糊搜索） */
    @Schema(description = "目标名称（模糊搜索）")
    private String targetName;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
}
