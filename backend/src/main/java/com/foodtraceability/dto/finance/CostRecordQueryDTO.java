package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 成本记录查询DTO
 */
@Schema(description = "成本记录查询条件")
public class CostRecordQueryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码")
    private Integer current;

    @Schema(description = "每页条数")
    private Integer size;

    @Schema(description = "成本类型: 1食材 2人工 3租金 4水电 5折旧 6包装 7其他")
    private Integer costType;

    @Schema(description = "成本归属期间，如 2026-04")
    private String period;

    @Schema(description = "起始期间")
    private String startPeriod;

    @Schema(description = "结束期间")
    private String endPeriod;

    @Schema(description = "成本中心/门店ID")
    private Long costCenterId;

    @Schema(description = "计算方式: 1实际发生 2分摊 3预估")
    private Integer calculationMethod;

    public Integer getCostType() { return costType; }
    public void setCostType(Integer costType) { this.costType = costType; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getStartPeriod() { return startPeriod; }
    public void setStartPeriod(String startPeriod) { this.startPeriod = startPeriod; }
    public String getEndPeriod() { return endPeriod; }
    public void setEndPeriod(String endPeriod) { this.endPeriod = endPeriod; }
    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }
    public Integer getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(Integer calculationMethod) { this.calculationMethod = calculationMethod; }
    public Integer getCurrent() { return current != null ? current : 1; }
    public void setCurrent(Integer current) { this.current = current; }
    public Integer getSize() { return size != null ? size : 10; }
    public void setSize(Integer size) { this.size = size; }
}
