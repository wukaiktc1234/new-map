package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 追溯码查询DTO
 */
@Schema(description = "追溯码查询条件")
public class TraceCodeQueryDTO {

    /** 关键字搜索（追溯码/目标名称） */
    @Schema(description = "关键字搜索")
    private String keyword;

    /** 追溯类型 */
    @Schema(description = "追溯类型")
    private Integer traceType;

    /** 目标类型 */
    @Schema(description = "目标类型")
    private Integer targetType;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 状态 */
    @Schema(description = "状态")
    private Integer status;

    /** 风险等级 */
    @Schema(description = "风险等级")
    private Integer riskLevel;

    /** 生产日期开始 */
    @Schema(description = "生产日期开始")
    private LocalDate productionDateStart;

    /** 生产日期结束 */
    @Schema(description = "生产日期结束")
    private LocalDate productionDateEnd;

    /** 有效期开始 */
    @Schema(description = "有效期开始")
    private LocalDate expiryDateStart;

    /** 有效期结束 */
    @Schema(description = "有效期结束")
    private LocalDate expiryDateEnd;

    /** 创建时间开始 */
    @Schema(description = "创建时间开始")
    private LocalDateTime createTimeStart;

    /** 创建时间结束 */
    @Schema(description = "创建时间结束")
    private LocalDateTime createTimeEnd;

    /** 页码 */
    @Schema(description = "页码", example = "1")
    private Integer current = 1;

    /** 每页大小 */
    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    // Getter和Setter方法

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getTraceType() { return traceType; }
    public void setTraceType(Integer traceType) { this.traceType = traceType; }
    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
    public LocalDate getProductionDateStart() { return productionDateStart; }
    public void setProductionDateStart(LocalDate productionDateStart) { this.productionDateStart = productionDateStart; }
    public LocalDate getProductionDateEnd() { return productionDateEnd; }
    public void setProductionDateEnd(LocalDate productionDateEnd) { this.productionDateEnd = productionDateEnd; }
    public LocalDate getExpiryDateStart() { return expiryDateStart; }
    public void setExpiryDateStart(LocalDate expiryDateStart) { this.expiryDateStart = expiryDateStart; }
    public LocalDate getExpiryDateEnd() { return expiryDateEnd; }
    public void setExpiryDateEnd(LocalDate expiryDateEnd) { this.expiryDateEnd = expiryDateEnd; }
    public LocalDateTime getCreateTimeStart() { return createTimeStart; }
    public void setCreateTimeStart(LocalDateTime createTimeStart) { this.createTimeStart = createTimeStart; }
    public LocalDateTime getCreateTimeEnd() { return createTimeEnd; }
    public void setCreateTimeEnd(LocalDateTime createTimeEnd) { this.createTimeEnd = createTimeEnd; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
