package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 桌台视图对象VO
 * 用于返回给前端的桌台信息
 */
@Schema(description = "桌台视图对象")
public class DiningTableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 桌台ID */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 桌台编码 */
    @Schema(description = "桌台编码")
    private String tableCode;

    /** 桌台名称 */
    @Schema(description = "桌台名称")
    private String tableName;

    /** 区域ID */
    @Schema(description = "区域ID")
    private Long areaId;

    /** 桌台类型编码 */
    @Schema(description = "桌台类型编码")
    private Integer tableType;

    /** 桌台类型名称 */
    @Schema(description = "桌台类型名称")
    private String tableTypeName;

    /** 座位数 */
    @Schema(description = "座位数")
    private Integer seatsCount;

    /** 最少建议人数 */
    @Schema(description = "最少建议人数")
    private Integer minPeople;

    /** 最多建议人数 */
    @Schema(description = "最多建议人数")
    private Integer maxPeople;

    /** 状态编码 */
    @Schema(description = "状态编码")
    private Integer status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 当前关联订单ID */
    @Schema(description = "当前关联订单ID")
    private String currentOrderId;

    /** 桌台二维码 */
    @Schema(description = "桌台二维码")
    private String qrCode;

    /** 排序权重 */
    @Schema(description = "排序权重")
    private Integer sortOrder;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private String createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private String updateTime;

    // ==================== Getter & Setter ====================

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Integer getTableType() {
        return tableType;
    }

    public void setTableType(Integer tableType) {
        this.tableType = tableType;
    }

    public String getTableTypeName() {
        return tableTypeName;
    }

    public void setTableTypeName(String tableTypeName) {
        this.tableTypeName = tableTypeName;
    }

    public Integer getSeatsCount() {
        return seatsCount;
    }

    public void setSeatsCount(Integer seatsCount) {
        this.seatsCount = seatsCount;
    }

    public Integer getMinPeople() {
        return minPeople;
    }

    public void setMinPeople(Integer minPeople) {
        this.minPeople = minPeople;
    }

    public Integer getMaxPeople() {
        return maxPeople;
    }

    public void setMaxPeople(Integer maxPeople) {
        this.maxPeople = maxPeople;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getCurrentOrderId() {
        return currentOrderId;
    }

    public void setCurrentOrderId(String currentOrderId) {
        this.currentOrderId = currentOrderId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
