package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 标准成本卡创建DTO
 */
@Schema(description = "标准成本卡创建请求")
public class StandardCostCardCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", required = true)
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "菜品名称", required = true)
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100位")
    private String dishName;

    @Schema(description = "标准成本（单位：分）", required = true)
    @NotNull(message = "标准成本不能为空")
    private Long standardCost;

    @Schema(description = "损耗系数（如1.05）")
    private BigDecimal lossCoefficient;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public Long getStandardCost() {
        return standardCost;
    }

    public void setStandardCost(Long standardCost) {
        this.standardCost = standardCost;
    }

    public BigDecimal getLossCoefficient() {
        return lossCoefficient;
    }

    public void setLossCoefficient(BigDecimal lossCoefficient) {
        this.lossCoefficient = lossCoefficient;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
