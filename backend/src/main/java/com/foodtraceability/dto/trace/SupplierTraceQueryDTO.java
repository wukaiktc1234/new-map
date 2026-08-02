package com.foodtraceability.dto.trace;

import com.foodtraceability.dto.PageQuery;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 供应商追溯查询DTO
 * 用于查询指定供应商的追溯信息（批次/检验/召回）
 */
@Schema(description = "供应商追溯查询条件")
public class SupplierTraceQueryDTO extends PageQuery {

    /** 供应商ID */
    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 查询开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "查询开始日期")
    private LocalDate startDate;

    /** 查询结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "查询结束日期")
    private LocalDate endDate;

    // Getter和Setter方法

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
