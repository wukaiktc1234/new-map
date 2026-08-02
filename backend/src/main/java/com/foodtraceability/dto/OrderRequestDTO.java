package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * POS订单创建请求DTO
 */
@Schema(description = "POS订单创建请求DTO")
public class OrderRequestDTO {
    @Schema(description = "桌号")
    @Min(value = 1, message = "桌号必须大于0")
    @Max(value = 9999, message = "桌号不能超过9999")
    private Integer tableNumber;

    @Schema(description = "订单项列表")
    @NotNull(message = "订单项列表不能为空")
    @Size(min = 1, max = 50, message = "订单项数量必须在1-50之间")
    @Valid
    private List<OrderItemDTO> items;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "支付方式")
    private String paymentMethod;
    @Schema(description = "用户ID（已登录用户）")
    private String userId;
    @Schema(description = "微信openid（未登录用户标识）")
    private String openid;
    @Schema(description = "订单类型：dinein-堂食，pickup-自提，takeout-外卖")
    private String orderType;
    @Schema(description = "联系人姓名")
    private String contactName;
    @Schema(description = "联系人电话")
    private String contactPhone;
    @Schema(description = "配送地址")
    private String deliveryAddress;
    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
    @Schema(description = "幂等性键，用于防止重复提交订单")
    @Size(max = 128, message = "幂等性键长度不能超过128个字符")
    private String idempotencyKey;
    @Schema(description = "门店ID（POS端所属门店，不传则默认中心旗舰店S001）")
    private String storeId;

    public OrderRequestDTO() {
    }

    public Integer getTableNumber() {
        return this.tableNumber;
    }

    public List<OrderItemDTO> getItems() {
        return this.items;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getOpenid() {
        return this.openid;
    }

    public String getOrderType() {
        return this.orderType;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getIdempotencyKey() {
        return this.idempotencyKey;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public void setTableNumber(final Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setItems(final List<OrderItemDTO> items) {
        this.items = items;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setOpenid(final String openid) {
        this.openid = openid;
    }

    public void setOrderType(final String orderType) {
        this.orderType = orderType;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setDeliveryAddress(final String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setIdempotencyKey(final String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderRequestDTO)) return false;
        final OrderRequestDTO other = (OrderRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$openid = this.getOpenid();
        final java.lang.Object other$openid = other.getOpenid();
        if (this$openid == null ? other$openid != null : !this$openid.equals(other$openid)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        final java.lang.Object this$deliveryAddress = this.getDeliveryAddress();
        final java.lang.Object other$deliveryAddress = other.getDeliveryAddress();
        if (this$deliveryAddress == null ? other$deliveryAddress != null : !this$deliveryAddress.equals(other$deliveryAddress)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $openid = this.getOpenid();
        result = result * PRIME + ($openid == null ? 43 : $openid.hashCode());
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        final java.lang.Object $deliveryAddress = this.getDeliveryAddress();
        result = result * PRIME + ($deliveryAddress == null ? 43 : $deliveryAddress.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderRequestDTO(tableNumber=" + this.getTableNumber() + ", items=" + this.getItems() + ", totalAmount=" + this.getTotalAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", userId=" + this.getUserId() + ", openid=" + this.getOpenid() + ", orderType=" + this.getOrderType() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ", deliveryAddress=" + this.getDeliveryAddress() + ", remark=" + this.getRemark() + ")";
    }
}
