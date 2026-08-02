package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 财务预警查询DTO
 */
@Schema(description = "财务预警查询条件")
public class FinanceWarningQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "预警类型：BUDGET_OVER、AR_OVERDUE、INVENTORY_OVER、CASH_FLOW_RISK、TAX_RISK")
    private String warningType;

    @Schema(description = "预警级别：LOW、MEDIUM、HIGH、URGENT")
    private String warningLevel;

    @Schema(description = "状态：UNHANDLED(未处理)、HANDLING(处理中)、RESOLVED(已解决)")
    private String status;

    @Schema(description = "起始日期（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "结束日期（yyyy-MM-dd）")
    private String endDate;

    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer size = 10;

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public Integer getCurrent() { return current; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
