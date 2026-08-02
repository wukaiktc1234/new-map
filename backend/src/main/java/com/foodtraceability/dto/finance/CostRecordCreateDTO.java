package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成本记录创建DTO
 */
public class CostRecordCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成本类型
     * 1-食材成本 2-人工成本 3-租金成本 4-水电成本 5-折旧成本 6-包装成本 7-其他成本
     */
    @NotNull(message = "成本类型不能为空")
    @Min(value = 1, message = "成本类型无效")
    @Max(value = 7, message = "成本类型无效")
    private Integer costType;

    /** 成本归属期间，如2026-04 */
    @NotBlank(message = "成本归属期间不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "成本归属期间格式必须为 YYYY-MM")
    private String period;

    /** 成本中心/门店ID */
    private Long costCenterId;

    /** 金额（单位：分） */
    @NotNull(message = "金额不能为空")
    @Min(value = 0, message = "金额不能为负数")
    private Long amount;

    /** 数量 */
    @DecimalMin(value = "0", message = "数量不能为负数")
    private BigDecimal quantity;

    /** 单价 */
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal unitPrice;

    /**
     * 计算方式
     * 1-实际发生 2-分摊 3-预估
     */
    @Min(value = 1, message = "计算方式无效")
    @Max(value = 3, message = "计算方式无效")
    private Integer calculationMethod;

    /** 关联凭证ID */
    private Long relatedVoucherId;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    // getter和setter方法
    public Integer getCostType() { return costType; }
    public void setCostType(Integer costType) { this.costType = costType; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long costCenterId) { this.costCenterId = costCenterId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(Integer calculationMethod) { this.calculationMethod = calculationMethod; }
    public Long getRelatedVoucherId() { return relatedVoucherId; }
    public void setRelatedVoucherId(Long relatedVoucherId) { this.relatedVoucherId = relatedVoucherId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
