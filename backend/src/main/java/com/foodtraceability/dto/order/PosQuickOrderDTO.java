package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * POS收银快速下单请求DTO
 * 简化版下单接口，用于收银台快速操作
 */
@Schema(description = "POS快速下单请求")
public class PosQuickOrderDTO {

    /** 订单类型：1堂食 2外卖 3自提 4打包 */
    @NotNull(message = "订单类型不能为空")
    @Schema(description = "订单类型: 1堂食 2外卖 3自提 4打包", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer orderType;

    /** 门店ID（关联 stores_new.store_id），多店场景必填 */
    @NotNull(message = "门店ID不能为空")
    @Min(value = 1, message = "门店ID无效")
    @Schema(description = "门店ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long storeId;

    /** 桌台ID（堂食时使用） */
    @Schema(description = "桌台ID")
    private Long tableId;

    /** 用餐人数 */
    @Schema(description = "用餐人数", example = "4")
    private Integer diningPeopleCount;

    /** 顾客电话（外卖时使用） */
    @Schema(description = "顾客电话")
    private String customerPhone;

    /** 会员手机号或卡号（用于识别会员） */
    @Schema(description = "会员标识")
    private String memberIdentify;

    /** 快速点餐商品列表 */
    @NotEmpty(message = "商品列表不能为空")
    @Valid
    @Schema(description = "商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<QuickItem> items;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /**
     * 快速点餐项
     */
    @Schema(description = "快速点餐项")
    public static class QuickItem {
        /** 菜品/套餐ID */
        @NotNull(message = "商品ID不能为空")
        @Schema(description = "菜品/套餐ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        /** 类型：1菜品 2套餐 */
        @NotNull(message = "商品类型不能为空")
        @Schema(description = "类型: 1菜品 2套餐", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer productType;

        /** 数量 */
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;

        /** 规格（可选） */
        @Schema(description = "规格")
        private String specification;

        /** 备注（如"不要辣"） */
        @Schema(description = "备注")
        private String remark;

        // Getter和Setter
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getProductType() { return productType; }
        public void setProductType(Integer productType) { this.productType = productType; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    // Getter和Setter方法
    public Integer getOrderType() { return orderType; }
    public void setOrderType(Integer orderType) { this.orderType = orderType; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public Integer getDiningPeopleCount() { return diningPeopleCount; }
    public void setDiningPeopleCount(Integer diningPeopleCount) { this.diningPeopleCount = diningPeopleCount; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getMemberIdentify() { return memberIdentify; }
    public void setMemberIdentify(String memberIdentify) { this.memberIdentify = memberIdentify; }
    public List<QuickItem> getItems() { return items; }
    public void setItems(List<QuickItem> items) { this.items = items; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
