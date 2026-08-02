package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 会计期间创建DTO
 */
@Schema(description = "会计期间创建请求")
public class AccountingPeriodCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "期间编码，如2026-01", required = true)
    @NotBlank(message = "期间编码不能为空")
    @Size(max = 20, message = "期间编码长度不能超过20位")
    private String periodCode;

    @Schema(description = "期间名称，如2026年1月", required = true)
    @NotBlank(message = "期间名称不能为空")
    @Size(max = 100, message = "期间名称长度不能超过100位")
    private String periodName;

    @Schema(description = "期间类型：1-月度 2-季度 3-年度", required = true)
    @NotNull(message = "期间类型不能为空")
    private Integer periodType;

    @Schema(description = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public Integer getPeriodType() {
        return periodType;
    }

    public void setPeriodType(Integer periodType) {
        this.periodType = periodType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
