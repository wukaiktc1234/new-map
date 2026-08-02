package com.foodtraceability.dto.trace;

import com.foodtraceability.dto.PageQuery;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 临期预警查询DTO
 * 用于临期预警记录列表分页查询
 */
@Schema(description = "临期预警查询条件")
public class ExpiryAlertQueryDTO extends PageQuery {

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 追溯类型：MATERIAL原料/FOOD食品 */
    @Schema(description = "追溯类型")
    private String traceType;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 预警级别：RED红/YELLOW黄/GREEN绿 */
    @Schema(description = "预警级别")
    private String alertLevel;

    /** 处理状态：PENDING/SCRAPPED/RETURNED/RESOLVED */
    @Schema(description = "处理状态")
    private String handlingStatus;

    /** 过期日期开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期开始")
    private LocalDate expiryDateStart;

    /** 过期日期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "过期日期结束")
    private LocalDate expiryDateEnd;

    // Getter和Setter方法

    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public String getTraceType() { return traceType; }
    public void setTraceType(String traceType) { this.traceType = traceType; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public String getHandlingStatus() { return handlingStatus; }
    public void setHandlingStatus(String handlingStatus) { this.handlingStatus = handlingStatus; }
    public LocalDate getExpiryDateStart() { return expiryDateStart; }
    public void setExpiryDateStart(LocalDate expiryDateStart) { this.expiryDateStart = expiryDateStart; }
    public LocalDate getExpiryDateEnd() { return expiryDateEnd; }
    public void setExpiryDateEnd(LocalDate expiryDateEnd) { this.expiryDateEnd = expiryDateEnd; }
}
