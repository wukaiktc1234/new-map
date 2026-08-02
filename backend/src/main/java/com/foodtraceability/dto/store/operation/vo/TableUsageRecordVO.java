package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 桌台使用记录视图对象VO
 * 用于返回给前端的桌台使用记录信息
 *
 * <h2>金额字段说明</h2>
 * finalAmount为分（Long），finalAmountYuan为元（String），
 * 由DataConverter从数据库的分转换而来
 */
@Schema(description = "桌台使用记录视图对象")
public class TableUsageRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单ID */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 订单号 */
    @Schema(description = "订单号")
    private String orderNumber;

    /** 桌台ID */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 桌台编码 */
    @Schema(description = "桌台编码")
    private String tableCode;

    /** 桌台名称 */
    @Schema(description = "桌台名称")
    private String tableName;

    /** 订单类型编码 */
    @Schema(description = "订单类型编码")
    private Integer orderType;

    /** 订单类型名称 */
    @Schema(description = "订单类型名称")
    private String orderTypeName;

    /** 订单来源 */
    @Schema(description = "订单来源")
    private String orderSource;

    /** 订单来源名称 */
    @Schema(description = "订单来源名称")
    private String orderSourceName;

    /** 用餐人数 */
    @Schema(description = "用餐人数")
    private Integer diningPeopleCount;

    /** 实付金额(分) */
    @Schema(description = "实付金额(分)")
    private Long finalAmount;

    /** 实付金额(元) */
    @Schema(description = "实付金额(元)")
    private String finalAmountYuan;

    /** 订单状态编码 */
    @Schema(description = "订单状态编码")
    private Integer orderStatus;

    /** 订单状态名称 */
    @Schema(description = "订单状态名称")
    private String orderStatusName;

    /** 下单时间 */
    @Schema(description = "下单时间")
    private String createTime;

    /** 完成时间 */
    @Schema(description = "完成时间")
    private String completeTime;

    /** 用餐时长(分钟) */
    @Schema(description = "用餐时长(分钟)")
    private Long durationMinutes;

    // ==================== Getter & Setter ====================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
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

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public String getOrderTypeName() {
        return orderTypeName;
    }

    public void setOrderTypeName(String orderTypeName) {
        this.orderTypeName = orderTypeName;
    }

    public String getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(String orderSource) {
        this.orderSource = orderSource;
    }

    public String getOrderSourceName() {
        return orderSourceName;
    }

    public void setOrderSourceName(String orderSourceName) {
        this.orderSourceName = orderSourceName;
    }

    public Integer getDiningPeopleCount() {
        return diningPeopleCount;
    }

    public void setDiningPeopleCount(Integer diningPeopleCount) {
        this.diningPeopleCount = diningPeopleCount;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Long finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getFinalAmountYuan() {
        return finalAmountYuan;
    }

    public void setFinalAmountYuan(String finalAmountYuan) {
        this.finalAmountYuan = finalAmountYuan;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatusName() {
        return orderStatusName;
    }

    public void setOrderStatusName(String orderStatusName) {
        this.orderStatusName = orderStatusName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(String completeTime) {
        this.completeTime = completeTime;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
