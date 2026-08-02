package com.foodtraceability.dto;

/**
 * 库存查询条件DTO
 */
public class InventoryQueryDTO {
    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 物料名称（模糊查询）
     */
    private String materialName;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 库存状态（1:正常 2:预警 3:过期 4:冻结）
     */
    private Integer status;

    /**
     * 是否只查询低库存
     */
    private Boolean lowStockOnly;

    /**
     * 是否只查询即将过期
     */
    private Boolean expiringSoon;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    // Getter和Setter方法
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getLowStockOnly() {
        return lowStockOnly;
    }

    public void setLowStockOnly(Boolean lowStockOnly) {
        this.lowStockOnly = lowStockOnly;
    }

    public Boolean getExpiringSoon() {
        return expiringSoon;
    }

    public void setExpiringSoon(Boolean expiringSoon) {
        this.expiringSoon = expiringSoon;
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
