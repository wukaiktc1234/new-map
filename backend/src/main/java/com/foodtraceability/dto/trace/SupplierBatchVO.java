package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商批次VO
 * 用于供应商追溯中的批次信息展示
 */
@Schema(description = "供应商批次信息")
public class SupplierBatchVO {

    /** 追溯码ID */
    @Schema(description = "追溯码ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 物料ID */
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "入库时间")
    private LocalDateTime inboundTime;

    /** 过期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    /** 状态 */
    @Schema(description = "状态")
    private String status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

    // Getter和Setter方法

    public Long getTraceCodeId() { return traceCodeId; }
    public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDateTime getInboundTime() { return inboundTime; }
    public void setInboundTime(LocalDateTime inboundTime) { this.inboundTime = inboundTime; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
}
