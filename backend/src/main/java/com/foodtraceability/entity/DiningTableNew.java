package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 桌台实体类
 * 用于管理餐厅桌台信息，支持扫码点餐功能
 */
@TableName("dining_tables")
@Schema(description = "桌台实体")
public class DiningTableNew {

    /** 桌台ID，主键自增 */
    @TableId(value = "table_id", type = IdType.AUTO)
    @Schema(description = "桌台ID", example = "1")
    private Long tableId;

    /** 门店ID */
    @TableField("store_id")
    @Schema(description = "门店ID", example = "1")
    private Long storeId;

    /** 桌台编码 */
    @TableField("table_code")
    @Schema(description = "桌台编码", example = "A01")
    private String tableCode;

    /** 桌台名称 */
    @TableField("table_name")
    @Schema(description = "桌台名称", example = "大厅A01桌")
    private String tableName;

    /** 区域ID */
    @TableField("area_id")
    @Schema(description = "区域ID", example = "1")
    private Long areaId;

    /**
     * 桌台类型：
     * 1大厅 2包厢 3吧台 4户外
     */
    @TableField("table_type")
    @Schema(description = "桌台类型: 1大厅 2包厢 3吧台 4户外", example = "1")
    private Integer tableType;

    /** 座位数 */
    @TableField("seats_count")
    @Schema(description = "座位数", example = "4")
    private Integer seatsCount;

    /** 最少建议人数 */
    @TableField("min_people")
    @Schema(description = "最少建议人数", example = "1")
    private Integer minPeople;

    /** 最多建议人数 */
    @TableField("max_people")
    @Schema(description = "最多建议人数", example = "10")
    private Integer maxPeople;

    /**
     * 状态：
     * 1空闲 2用餐中 3预订 4维护中 5停用
     */
    @TableField("status")
    @Schema(description = "状态: 1空闲 2用餐中 3预订 4维护中 5停用", example = "1")
    private Integer status;

    /** 当前关联的订单ID */
    @TableField("current_order_id")
    @Schema(description = "当前关联的订单ID")
    private String currentOrderId;

    /** 桌台二维码（扫码点餐） */
    @TableField("qr_code")
    @Schema(description = "桌台二维码")
    private String qrCode;

    /** 排序权重 */
    @TableField("sort_order")
    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public Long getTableId() { return tableId; }
    public Long getStoreId() { return storeId; }
    public String getTableCode() { return tableCode; }
    public String getTableName() { return tableName; }
    public Long getAreaId() { return areaId; }
    public Integer getTableType() { return tableType; }
    public Integer getSeatsCount() { return seatsCount; }
    public Integer getMinPeople() { return minPeople; }
    public Integer getMaxPeople() { return maxPeople; }
    public Integer getStatus() { return status; }
    public String getCurrentOrderId() { return currentOrderId; }
    public String getQrCode() { return qrCode; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setTableCode(String tableCode) { this.tableCode = tableCode; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }
    public void setTableType(Integer tableType) { this.tableType = tableType; }
    public void setSeatsCount(Integer seatsCount) { this.seatsCount = seatsCount; }
    public void setMinPeople(Integer minPeople) { this.minPeople = minPeople; }
    public void setMaxPeople(Integer maxPeople) { this.maxPeople = maxPeople; }
    public void setStatus(Integer status) { this.status = status; }
    public void setCurrentOrderId(String currentOrderId) { this.currentOrderId = currentOrderId; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
