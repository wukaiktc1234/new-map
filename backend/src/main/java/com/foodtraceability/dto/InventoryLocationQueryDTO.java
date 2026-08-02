package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 库位查询DTO
 */
public class InventoryLocationQueryDTO {

    /**
     * 所属仓库ID
     */
    private Long warehouseId;

    /**
     * 库位编码（模糊搜索）
     */
    private String locationCode;

    /**
     * 库位类型（1:货架 2:地面 3:冷藏区 4:冷冻区）
     */
    private Integer locationType;

    /**
     * 状态（1:启用 0:停用）
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页条数
     */
    private Integer size = 10;

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

    public Integer getLocationType() {
        return locationType;
    }

    public void setLocationType(Integer locationType) {
        this.locationType = locationType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
