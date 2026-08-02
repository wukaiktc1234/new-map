package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 创建订单请求DTO
 * 包含订单基本信息和明细列表
 */
@Schema(description = "创建订单请求")
public class OrderCreateDTO {

    /** 订单类型：1堂食 2外卖 3自提 4打包 */
    @NotNull(message = "订单类型不能为空")
    @Min(value = 1, message = "订单类型无效")
    @Max(value = 4, message = "订单类型无效")
    @Schema(description = "订单类型: 1堂食 2外卖 3自提 4打包", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderType;

    /** 订单来源：1收银台 2小程序 3第三方平台 */
    @Schema(description = "订单来源: 1收银台 2小程序 3第三方平台", example = "1")
    private Integer orderSource = 1;

    /** 门店ID（关联 stores_new.store_id），多店场景必填 */
    @NotNull(message = "门店ID不能为空")
    @Min(value = 1, message = "门店ID无效")
    @Schema(description = "门店ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long storeId;

    /** 会员ID */
    @Schema(description = "会员ID")
    private Long customerId;

    /** 顾客姓名（外卖必填） */
    @Schema(description = "顾客姓名")
    private String customerName;

    /** 顾客电话（外卖必填） */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "顾客电话")
    private String customerPhone;

    /** 桌台ID（堂食必填） */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 用餐人数 */
    @Min(value = 1, message = "用餐人数至少1人")
    @Max(value = 99, message = "用餐人数不能超过99人")
    @Schema(description = "用餐人数", example = "4")
    private Integer diningPeopleCount;

    /** 外卖配送地址 */
    @Schema(description = "外卖配送地址")
    private String deliveryAddress;

    /** 期望送达时间（外卖） */
    @Schema(description = "期望送达时间")
    private String expectedTime;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500字")
    @Schema(description = "备注")
    private String remark;

    /** 订单明细列表 */
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    @Schema(description = "订单明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemCreateDTO> items;

    // Getter和Setter方法
    public Integer getOrderType() { return orderType; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public Integer getOrderSource() { return orderSource; }
    public void setOrderSource(Integer orderSource) { this.orderSource = orderSource; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Integer getDiningPeopleCount() { return diningPeopleCount; }
    public void setDiningPeopleCount(Integer diningPeopleCount) { this.diningPeopleCount = diningPeopleCount; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getExpectedTime() { return expectedTime; }
    public void setExpectedTime(String expectedTime) { this.expectedTime = expectedTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<OrderItemCreateDTO> getItems() { return items; }
    public void setItems(List<OrderItemCreateDTO> items) { this.items = items; }

    /**
     * 订单明细创建DTO（内部类）
     */
    @Schema(description = "订单明细")
    public static class OrderItemCreateDTO {

        /** 产品类型：1单品 2套餐 */
        @NotNull(message = "产品类型不能为空")
        @Min(value = 1, message = "产品类型无效")
        @Max(value = 2, message = "产品类型无效")
        @Schema(description = "产品类型: 1单品 2套餐", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer productType;

        /** 单品/套餐ID（根据productType决定含义） */
        @NotNull(message = "产品ID不能为空")
        @Min(value = 1, message = "产品ID无效")
        @Schema(description = "产品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        /** 商品名称（冗余，方便查询） */
        @NotBlank(message = "商品名称不能为空")
        @Schema(description = "商品名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String productName;

        /** 规格 */
        @Schema(description = "规格", example = "大份")
        private String specification;

        /** 单价（分） */
        @NotNull(message = "单价不能为空")
        @Min(value = 0, message = "单价不能为负数")
        @Schema(description = "单价（分）", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long unitPrice;

        /** 数量 */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        @Max(value = 99, message = "单次点餐数量不能超过99")
        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        /** 单项优惠（分） */
        @Min(value = 0, message = "优惠金额不能为负数")
        @Schema(description = "单项优惠（分）")
        private Long discountAmount = 0L;

        /** 备注 */
        @Size(max = 200, message = "备注长度不能超过200字")
        @Schema(description = "备注", example = "不要辣")
        private String remark;

        // Getter和Setter
        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public Long getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Long getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
