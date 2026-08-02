package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 后厨订单完整信息DTO
 * 包含kitchen_order和orders表的关联数据
 */
@Schema(description = "后厨订单完整信息DTO")
public class KitchenOrderFullDTO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "后厨订单ID")
    private String kitchenOrderId;
    @Schema(description = "订单ID（关联orders表）")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提")
    private Integer orderType;
    @Schema(description = "桌号")
    private String tableNumber;
    @Schema(description = "菜品列表JSON")
    private String dishItems;
    @Schema(description = "菜品总数")
    private Integer totalDishes;
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;
    @Schema(description = "实际支付金额")
    private BigDecimal actualAmount;
    @Schema(description = "支付方式")
    private String paymentMethod;
    @Schema(description = "优先级")
    private Integer priority;
    @Schema(description = "后厨状态")
    private String status;
    @Schema(description = "接单时间")
    private LocalDateTime receiveTime;
    @Schema(description = "开始制作时间")
    private LocalDateTime makeStartTime;
    @Schema(description = "制作完成时间")
    private LocalDateTime makeCompleteTime;
    @Schema(description = "出餐时间")
    private LocalDateTime serveTime;
    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;
    @Schema(description = "取消原因")
    private String cancelReason;
    @Schema(description = "厨师ID")
    private Long chefId;
    @Schema(description = "厨师姓名")
    private String chefName;
    @Schema(description = "门店ID")
    private Long storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "原料是否已扣减")
    private Integer materialConsumed;
    @Schema(description = "原料扣减时间")
    private LocalDateTime materialConsumeTime;
    @Schema(description = "原料是否已锁定")
    private Integer materialLocked;
    @Schema(description = "原料锁定时间")
    private LocalDateTime materialLockTime;
    @Schema(description = "食品追溯码列表")
    private String foodTraceCodes;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "创建人")
    private String createBy;
    @Schema(description = "更新人")
    private String updateBy;
    @Schema(description = "订单状态（来自orders表）")
    private Integer orderStatus;
    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;
    @Schema(description = "订单备注")
    private String orderRemarks;
    @Schema(description = "联系人姓名")
    private String contactName;
    @Schema(description = "联系人电话")
    private String contactPhone;

    public KitchenOrderFullDTO() {
    }

    public Long getId() {
        return this.id;
    }

    public String getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public String getDishItems() {
        return this.dishItems;
    }

    public Integer getTotalDishes() {
        return this.totalDishes;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public BigDecimal getActualAmount() {
        return this.actualAmount;
    }

    public String getPaymentMethod() {
        return this.paymentMethod;
    }

    public Integer getPriority() {
        return this.priority;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getReceiveTime() {
        return this.receiveTime;
    }

    public LocalDateTime getMakeStartTime() {
        return this.makeStartTime;
    }

    public LocalDateTime getMakeCompleteTime() {
        return this.makeCompleteTime;
    }

    public LocalDateTime getServeTime() {
        return this.serveTime;
    }

    public LocalDateTime getCancelTime() {
        return this.cancelTime;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }

    public Long getChefId() {
        return this.chefId;
    }

    public String getChefName() {
        return this.chefName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public Integer getMaterialConsumed() {
        return this.materialConsumed;
    }

    public LocalDateTime getMaterialConsumeTime() {
        return this.materialConsumeTime;
    }

    public Integer getMaterialLocked() {
        return this.materialLocked;
    }

    public LocalDateTime getMaterialLockTime() {
        return this.materialLockTime;
    }

    public String getFoodTraceCodes() {
        return this.foodTraceCodes;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public Integer getOrderStatus() {
        return this.orderStatus;
    }

    public LocalDateTime getPaymentTime() {
        return this.paymentTime;
    }

    public String getOrderRemarks() {
        return this.orderRemarks;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setKitchenOrderId(final String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setDishItems(final String dishItems) {
        this.dishItems = dishItems;
    }

    public void setTotalDishes(final Integer totalDishes) {
        this.totalDishes = totalDishes;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setActualAmount(final BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setReceiveTime(final LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    public void setMakeStartTime(final LocalDateTime makeStartTime) {
        this.makeStartTime = makeStartTime;
    }

    public void setMakeCompleteTime(final LocalDateTime makeCompleteTime) {
        this.makeCompleteTime = makeCompleteTime;
    }

    public void setServeTime(final LocalDateTime serveTime) {
        this.serveTime = serveTime;
    }

    public void setCancelTime(final LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public void setCancelReason(final String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setChefId(final Long chefId) {
        this.chefId = chefId;
    }

    public void setChefName(final String chefName) {
        this.chefName = chefName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setMaterialConsumed(final Integer materialConsumed) {
        this.materialConsumed = materialConsumed;
    }

    public void setMaterialConsumeTime(final LocalDateTime materialConsumeTime) {
        this.materialConsumeTime = materialConsumeTime;
    }

    public void setMaterialLocked(final Integer materialLocked) {
        this.materialLocked = materialLocked;
    }

    public void setMaterialLockTime(final LocalDateTime materialLockTime) {
        this.materialLockTime = materialLockTime;
    }

    public void setFoodTraceCodes(final String foodTraceCodes) {
        this.foodTraceCodes = foodTraceCodes;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setOrderStatus(final Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void setPaymentTime(final LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public void setOrderRemarks(final String orderRemarks) {
        this.orderRemarks = orderRemarks;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof KitchenOrderFullDTO)) return false;
        final KitchenOrderFullDTO other = (KitchenOrderFullDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$totalDishes = this.getTotalDishes();
        final java.lang.Object other$totalDishes = other.getTotalDishes();
        if (this$totalDishes == null ? other$totalDishes != null : !this$totalDishes.equals(other$totalDishes)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$chefId = this.getChefId();
        final java.lang.Object other$chefId = other.getChefId();
        if (this$chefId == null ? other$chefId != null : !this$chefId.equals(other$chefId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$materialConsumed = this.getMaterialConsumed();
        final java.lang.Object other$materialConsumed = other.getMaterialConsumed();
        if (this$materialConsumed == null ? other$materialConsumed != null : !this$materialConsumed.equals(other$materialConsumed)) return false;
        final java.lang.Object this$materialLocked = this.getMaterialLocked();
        final java.lang.Object other$materialLocked = other.getMaterialLocked();
        if (this$materialLocked == null ? other$materialLocked != null : !this$materialLocked.equals(other$materialLocked)) return false;
        final java.lang.Object this$orderStatus = this.getOrderStatus();
        final java.lang.Object other$orderStatus = other.getOrderStatus();
        if (this$orderStatus == null ? other$orderStatus != null : !this$orderStatus.equals(other$orderStatus)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$dishItems = this.getDishItems();
        final java.lang.Object other$dishItems = other.getDishItems();
        if (this$dishItems == null ? other$dishItems != null : !this$dishItems.equals(other$dishItems)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$actualAmount = this.getActualAmount();
        final java.lang.Object other$actualAmount = other.getActualAmount();
        if (this$actualAmount == null ? other$actualAmount != null : !this$actualAmount.equals(other$actualAmount)) return false;
        final java.lang.Object this$paymentMethod = this.getPaymentMethod();
        final java.lang.Object other$paymentMethod = other.getPaymentMethod();
        if (this$paymentMethod == null ? other$paymentMethod != null : !this$paymentMethod.equals(other$paymentMethod)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$receiveTime = this.getReceiveTime();
        final java.lang.Object other$receiveTime = other.getReceiveTime();
        if (this$receiveTime == null ? other$receiveTime != null : !this$receiveTime.equals(other$receiveTime)) return false;
        final java.lang.Object this$makeStartTime = this.getMakeStartTime();
        final java.lang.Object other$makeStartTime = other.getMakeStartTime();
        if (this$makeStartTime == null ? other$makeStartTime != null : !this$makeStartTime.equals(other$makeStartTime)) return false;
        final java.lang.Object this$makeCompleteTime = this.getMakeCompleteTime();
        final java.lang.Object other$makeCompleteTime = other.getMakeCompleteTime();
        if (this$makeCompleteTime == null ? other$makeCompleteTime != null : !this$makeCompleteTime.equals(other$makeCompleteTime)) return false;
        final java.lang.Object this$serveTime = this.getServeTime();
        final java.lang.Object other$serveTime = other.getServeTime();
        if (this$serveTime == null ? other$serveTime != null : !this$serveTime.equals(other$serveTime)) return false;
        final java.lang.Object this$cancelTime = this.getCancelTime();
        final java.lang.Object other$cancelTime = other.getCancelTime();
        if (this$cancelTime == null ? other$cancelTime != null : !this$cancelTime.equals(other$cancelTime)) return false;
        final java.lang.Object this$cancelReason = this.getCancelReason();
        final java.lang.Object other$cancelReason = other.getCancelReason();
        if (this$cancelReason == null ? other$cancelReason != null : !this$cancelReason.equals(other$cancelReason)) return false;
        final java.lang.Object this$chefName = this.getChefName();
        final java.lang.Object other$chefName = other.getChefName();
        if (this$chefName == null ? other$chefName != null : !this$chefName.equals(other$chefName)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$materialConsumeTime = this.getMaterialConsumeTime();
        final java.lang.Object other$materialConsumeTime = other.getMaterialConsumeTime();
        if (this$materialConsumeTime == null ? other$materialConsumeTime != null : !this$materialConsumeTime.equals(other$materialConsumeTime)) return false;
        final java.lang.Object this$materialLockTime = this.getMaterialLockTime();
        final java.lang.Object other$materialLockTime = other.getMaterialLockTime();
        if (this$materialLockTime == null ? other$materialLockTime != null : !this$materialLockTime.equals(other$materialLockTime)) return false;
        final java.lang.Object this$foodTraceCodes = this.getFoodTraceCodes();
        final java.lang.Object other$foodTraceCodes = other.getFoodTraceCodes();
        if (this$foodTraceCodes == null ? other$foodTraceCodes != null : !this$foodTraceCodes.equals(other$foodTraceCodes)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$paymentTime = this.getPaymentTime();
        final java.lang.Object other$paymentTime = other.getPaymentTime();
        if (this$paymentTime == null ? other$paymentTime != null : !this$paymentTime.equals(other$paymentTime)) return false;
        final java.lang.Object this$orderRemarks = this.getOrderRemarks();
        final java.lang.Object other$orderRemarks = other.getOrderRemarks();
        if (this$orderRemarks == null ? other$orderRemarks != null : !this$orderRemarks.equals(other$orderRemarks)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof KitchenOrderFullDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $totalDishes = this.getTotalDishes();
        result = result * PRIME + ($totalDishes == null ? 43 : $totalDishes.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $chefId = this.getChefId();
        result = result * PRIME + ($chefId == null ? 43 : $chefId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $materialConsumed = this.getMaterialConsumed();
        result = result * PRIME + ($materialConsumed == null ? 43 : $materialConsumed.hashCode());
        final java.lang.Object $materialLocked = this.getMaterialLocked();
        result = result * PRIME + ($materialLocked == null ? 43 : $materialLocked.hashCode());
        final java.lang.Object $orderStatus = this.getOrderStatus();
        result = result * PRIME + ($orderStatus == null ? 43 : $orderStatus.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $dishItems = this.getDishItems();
        result = result * PRIME + ($dishItems == null ? 43 : $dishItems.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $actualAmount = this.getActualAmount();
        result = result * PRIME + ($actualAmount == null ? 43 : $actualAmount.hashCode());
        final java.lang.Object $paymentMethod = this.getPaymentMethod();
        result = result * PRIME + ($paymentMethod == null ? 43 : $paymentMethod.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $receiveTime = this.getReceiveTime();
        result = result * PRIME + ($receiveTime == null ? 43 : $receiveTime.hashCode());
        final java.lang.Object $makeStartTime = this.getMakeStartTime();
        result = result * PRIME + ($makeStartTime == null ? 43 : $makeStartTime.hashCode());
        final java.lang.Object $makeCompleteTime = this.getMakeCompleteTime();
        result = result * PRIME + ($makeCompleteTime == null ? 43 : $makeCompleteTime.hashCode());
        final java.lang.Object $serveTime = this.getServeTime();
        result = result * PRIME + ($serveTime == null ? 43 : $serveTime.hashCode());
        final java.lang.Object $cancelTime = this.getCancelTime();
        result = result * PRIME + ($cancelTime == null ? 43 : $cancelTime.hashCode());
        final java.lang.Object $cancelReason = this.getCancelReason();
        result = result * PRIME + ($cancelReason == null ? 43 : $cancelReason.hashCode());
        final java.lang.Object $chefName = this.getChefName();
        result = result * PRIME + ($chefName == null ? 43 : $chefName.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $materialConsumeTime = this.getMaterialConsumeTime();
        result = result * PRIME + ($materialConsumeTime == null ? 43 : $materialConsumeTime.hashCode());
        final java.lang.Object $materialLockTime = this.getMaterialLockTime();
        result = result * PRIME + ($materialLockTime == null ? 43 : $materialLockTime.hashCode());
        final java.lang.Object $foodTraceCodes = this.getFoodTraceCodes();
        result = result * PRIME + ($foodTraceCodes == null ? 43 : $foodTraceCodes.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $paymentTime = this.getPaymentTime();
        result = result * PRIME + ($paymentTime == null ? 43 : $paymentTime.hashCode());
        final java.lang.Object $orderRemarks = this.getOrderRemarks();
        result = result * PRIME + ($orderRemarks == null ? 43 : $orderRemarks.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "KitchenOrderFullDTO(id=" + this.getId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", dishItems=" + this.getDishItems() + ", totalDishes=" + this.getTotalDishes() + ", totalAmount=" + this.getTotalAmount() + ", actualAmount=" + this.getActualAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", priority=" + this.getPriority() + ", status=" + this.getStatus() + ", receiveTime=" + this.getReceiveTime() + ", makeStartTime=" + this.getMakeStartTime() + ", makeCompleteTime=" + this.getMakeCompleteTime() + ", serveTime=" + this.getServeTime() + ", cancelTime=" + this.getCancelTime() + ", cancelReason=" + this.getCancelReason() + ", chefId=" + this.getChefId() + ", chefName=" + this.getChefName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", materialConsumed=" + this.getMaterialConsumed() + ", materialConsumeTime=" + this.getMaterialConsumeTime() + ", materialLocked=" + this.getMaterialLocked() + ", materialLockTime=" + this.getMaterialLockTime() + ", foodTraceCodes=" + this.getFoodTraceCodes() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", orderStatus=" + this.getOrderStatus() + ", paymentTime=" + this.getPaymentTime() + ", orderRemarks=" + this.getOrderRemarks() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ")";
    }
}
