package com.foodtraceability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 库存增加DTO
 * 用于库存增加操作的请求参数
 */
public class IncreaseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 物料ID */
    @NotNull(message = "物料ID不能为空")
    private Long materialId;

    /** 变动数量 */
    @NotNull(message = "变动数量不能为空")
    @Min(value = 1, message = "变动数量必须大于0")
    private Integer quantity;

    /** 备注 */
    private String remark;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
