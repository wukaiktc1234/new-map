package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单查询条件DTO
 * 用于多条件筛选订单
 */
@Schema(description = "订单查询条件")
public class OrderQueryDTO {

    /** 当前页码 */
    @Schema(description = "当前页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    /** 订单编号（模糊搜索） */
    @Schema(description = "订单编号")
    private String orderCode;

    /** 订单类型 */
    @Schema(description = "订单类型: 1堂食 2外卖 3自提 4打包")
    private Integer orderType;

    /** 订单状态 */
    @Schema(description = "订单状态: 0待确认 1已确认 2已完成 3已取消 4部分退款 5全额退款 6待评价")
    private Integer orderStatus;

    /** 支付状态 */
    @Schema(description = "支付状态: 0未支付 1部分支付 2已支付 3已退款")
    private Integer paymentStatus;

    /** 订单来源 */
    @Schema(description = "订单来源: 1收银台 2小程序 3第三方平台")
    private Integer orderSource;

    /** 会员ID */
    @Schema(description = "会员ID")
    private Long customerId;

    /** 顾客姓名（模糊搜索） */
    @Schema(description = "顾客姓名（模糊搜索）")
    private String customerName;

    /** 顾客手机号 */
    @Schema(description = "顾客手机号")
    private String customerPhone;

    /** 桌台ID */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 收银员ID */
    @Schema(description = "收银员ID")
    private Long cashierUserId;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    /** 最小金额（分） */
    @Schema(description = "最小金额（分）")
    private Long minAmount;

    /** 最大金额（分） */
    @Schema(description = "最大金额（分）")
    private Long maxAmount;

    /** 门店ID（用于按门店筛选订单） */
    @Schema(description = "门店ID")
    private String storeId;

    // Getter和Setter方法
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Integer getOrderType() { return orderType; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public Integer getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(Integer paymentStatus) { this.paymentStatus = paymentStatus; }
    public Integer getOrderSource() { return orderSource; }
    public void setOrderSource(Integer orderSource) { this.orderSource = orderSource; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Long getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(Long cashierUserId) { this.cashierUserId = cashierUserId; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Long getMinAmount() { return minAmount; }
    public void setMinAmount(Long minAmount) { this.minAmount = minAmount; }
    public Long getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Long maxAmount) { this.maxAmount = maxAmount; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
}
