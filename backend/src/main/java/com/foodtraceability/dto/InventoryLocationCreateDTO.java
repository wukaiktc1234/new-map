package com.foodtraceability.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 库位创建DTO
 */
public class InventoryLocationCreateDTO {

    /**
     * 所属仓库ID
     */
    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    /**
     * 库位编码（唯一）
     */
    @NotBlank(message = "库位编码不能为空")
    @Size(max = 32, message = "库位编码长度不能超过32个字符")
    private String locationCode;

    /**
     * 库位名称
     */
    @NotBlank(message = "库位名称不能为空")
    @Size(max = 100, message = "库位名称长度不能超过100个字符")
    private String locationName;

    /**
     * 库位类型（1:货架 2:地面 3:冷藏区 4:冷冻区）
     */
    @NotNull(message = "库位类型不能为空")
    @Min(value = 1, message = "库位类型无效")
    @Max(value = 4, message = "库位类型无效")
    private Integer locationType;

    /**
     * 最大容量
     */
    @DecimalMin(value = "0", message = "最大容量不能为负数")
    private BigDecimal maxCapacity;

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

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
