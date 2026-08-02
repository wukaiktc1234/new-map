package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 成本记录更新DTO
 */
@Schema(description = "成本记录更新参数")
public class CostRecordUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "成本记录ID")
    @NotNull(message = "成本记录ID不能为空")
    private Long costId;

    @Schema(description = "成本类型: 1食材 2人工 3租金 4水电 5折旧 6包装 7其他")
    @Min(value = 1, message = "成本类型无效")
    @Max(value = 7, message = "成本类型无效")
    private Integer costType;

    @Schema(description = "成本归属期间，如 2026-04")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "成本归属期间格式必须为 YYYY-MM")
    private String period;

    @Schema(description = "成本中心/门店ID")
    private Long costCenterId;

    @Schema(description = "金额（单位：分）")
    @Min(value = 0, message = "金额不能为负数")
    private Long amount;

    @Schema(description = "计算方式: 1实际发生 2分摊 3预估")
    @Min(value = 1, message = "计算方式无效")
    @Max(value = 3, message = "计算方式无效")
    private Integer calculationMethod;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public Long getCostId() { return costId; }
    public void setCostId(Long costId) { this.costId = costId; }
    public Integer getCostType() { return costType; }
    public void setCostType(Integer costType) { this.costType = costType; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Integer getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(Integer calculationMethod) { this.calculationMethod = calculationMethod; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
