package com.foodtraceability.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 库位更新DTO
 */
public class InventoryLocationUpdateDTO {

    /**
     * 库位名称
     */
    @Size(max = 100, message = "库位名称长度不能超过100个字符")
    private String locationName;

    /**
     * 库位类型（1:货架 2:地面 3:冷藏区 4:冷冻区）
     */
    private Integer locationType;

    /**
     * 最大容量
     */
    private BigDecimal maxCapacity;

    /**
     * 状态（1:启用 0:停用）
     */
    private Integer status;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Integer getLocationType() {
        return locationType;
    }

    public void setLocationType(Integer locationType) {
        this.locationType = locationType;
    }

    public BigDecimal getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(BigDecimal maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
