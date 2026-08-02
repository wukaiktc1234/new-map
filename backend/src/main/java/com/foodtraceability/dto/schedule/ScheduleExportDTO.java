package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 排班导出请求DTO
 */
@Schema(description = "排班导出请求")
public class ScheduleExportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案ID */
    @Schema(description = "排班方案ID")
    private String planId;

    /** 导出格式（excel/csv） */
    @Schema(description = "导出格式: excel/csv", example = "excel")
    private String format = "excel";

    /** 导出范围（all/conflicts/swap） */
    @Schema(description = "导出范围: all/conflicts/swap", example = "all")
    private String scope = "all";

    /** 开始日期（可选） */
    @Schema(description = "开始日期 (YYYY-MM-DD)")
    private String startDate;

    /** 结束日期（可选） */
    @Schema(description = "结束日期 (YYYY-MM-DD)")
    private String endDate;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
