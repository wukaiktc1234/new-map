package com.foodtraceability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 盘点单创建请求DTO
 */
public class InventoryCheckCreateDTO {
    /**
     * 仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    /**
     * 盘点类型（1:盲盘 2:明盘 3:循环盘点）
     */
    @NotNull(message = "盘点类型不能为空")
    @Min(value = 1, message = "盘点类型无效")
    @Max(value = 3, message = "盘点类型无效")
    private Integer checkType;

    /**
     * 盘点日期
     */
    @NotNull(message = "盘点日期不能为空")
    private LocalDate checkDate;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getCheckType() {
        return checkType;
    }

    public void setCheckType(Integer checkType) {
        this.checkType = checkType;
    }

    public LocalDate getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDate checkDate) {
        this.checkDate = checkDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
