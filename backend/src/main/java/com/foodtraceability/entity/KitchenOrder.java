package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 后厨订单实体类
 * 用于管理后厨的订单制作流程
 */
@TableName("kitchen_order")
@Schema(description = "后厨订单实体")
public class KitchenOrder {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("kitchen_order_id")
    @Schema(description = "后厨订单ID", example = "KO20250101001")
    private String kitchenOrderId;
    @TableField("order_id")
    @Schema(description = "订单ID", example = "O20250101001")
    private String orderId;
    @TableField("order_number")
    @Schema(description = "订单编号", example = "ORD202501010001")
    private String orderNumber;
    @TableField(exist = false)
    @Schema(description = "取餐号", example = "A001")
    private String pickupNumber;
    @TableField(exist = false)
    @Schema(description = "取餐码（外卖使用）", example = "1234")
    private String pickupCode;
    @TableField("order_type")
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提", example = "0")
    private Integer orderType;
    @TableField("table_number")
    @Schema(description = "桌号（堂食时使用）", example = "A01")
    private String tableNumber;
    @TableField("dish_items")
    @Schema(description = "菜品列表（JSON数组）")
    private String dishItems;
    @TableField("total_dishes")
    @Schema(description = "菜品总数", example = "3")
    private Integer totalDishes;
    @TableField(exist = false)
    @Schema(description = "订单总金额（来自orders表）", example = "88.50")
    private java.math.BigDecimal totalAmount;
    @TableField(exist = false)
    @Schema(description = "支付方式（来自orders表）", example = "现金")
    private String paymentMethod;
    @TableField("priority")
    @Schema(description = "优先级：0-普通, 1-加急, 2-特急", example = "0")
    private Integer priority;
    @TableField("status")
    @Schema(description = "状态：pending-待制作, received-已接单, making-制作中, completed-已完成, served-已出餐, cancelled-已取消", example = "pending")
    private String status;
    @TableField("receive_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "接单时间")
    private LocalDateTime receiveTime;
    @TableField("make_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始制作时间")
    private LocalDateTime makeStartTime;
    @TableField("make_complete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "制作完成时间")
    private LocalDateTime makeCompleteTime;
    @TableField("serve_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "出餐时间")
    private LocalDateTime serveTime;
    @TableField("cancel_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;
    @TableField("cancel_reason")
    @Schema(description = "取消原因")
    private String cancelReason;
    @TableField("chef_id")
    @Schema(description = "制作人员ID")
    private Long chefId;
    @TableField("chef_name")
    @Schema(description = "制作人员姓名", example = "李师傅")
    private String chefName;
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;
    @TableField("store_name")
    @Schema(description = "门店名称", example = "总店")
    private String storeName;
    @TableField("material_consumed")
    @Schema(description = "原料是否已扣减：0-未扣减, 1-已扣减", example = "0")
    private Integer materialConsumed;
    @TableField("material_consume_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "原料扣减时间")
    private LocalDateTime materialConsumeTime;
    @TableField("material_locked")
    @Schema(description = "原料是否已锁定：0-未锁定，1-已锁定")
    private Integer materialLocked;
    @TableField("material_lock_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "原料锁定时间")
    private LocalDateTime materialLockTime;
    @TableField("food_trace_codes")
    @Schema(description = "生成的食品追溯码列表（JSON数组）")
    private String foodTraceCodes;
    @TableField("tray_id")
    @Schema(description = "托盘ID")
    private Long trayId;
    @TableField("tray_code")
    @Schema(description = "托盘码", example = "TRAY001")
    private String trayCode;
    @TableField("tray_bind_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "托盘绑定时间")
    private LocalDateTime trayBindTime;
    @TableField("bound_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "托盘绑定时间（POS绑定阶段，5状态机专用）")
    private LocalDateTime boundTime;
    @TableField("making_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "后厨开始制作时间（后厨一次扫码）")
    private LocalDateTime makingStartTime;
    @TableField("ready_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "后厨出餐时间（后厨二次扫码）")
    private LocalDateTime readyTime;
    @TableField("served_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "顾客取餐时间（取餐口确认）")
    private LocalDateTime servedTime;
    @TableField("last_camera_snapshot_url")
    @Schema(description = "最新摄像头拍照URL")
    private String lastCameraSnapshotUrl;
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createBy;
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updateBy;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记：0-未删除, 1-已删除")
    private Integer deleted;

    public KitchenOrder() {
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

    public String getPickupNumber() {
        return this.pickupNumber;
    }

    public String getPickupCode() {
        return this.pickupCode;
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

    public java.math.BigDecimal getTotalAmount() {
        return this.totalAmount;
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

    public Long getTrayId() {
        return this.trayId;
    }

    public String getTrayCode() {
        return this.trayCode;
    }

    public LocalDateTime getTrayBindTime() {
        return this.trayBindTime;
    }

    public LocalDateTime getBoundTime() {
        return this.boundTime;
    }

    public LocalDateTime getMakingStartTime() {
        return this.makingStartTime;
    }

    public LocalDateTime getReadyTime() {
        return this.readyTime;
    }

    public LocalDateTime getServedTime() {
        return this.servedTime;
    }

    public String getLastCameraSnapshotUrl() {
        return this.lastCameraSnapshotUrl;
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

    public Integer getDeleted() {
        return this.deleted;
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

    public void setPickupNumber(final String pickupNumber) {
        this.pickupNumber = pickupNumber;
    }

    public void setPickupCode(final String pickupCode) {
        this.pickupCode = pickupCode;
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

    public void setTotalAmount(final java.math.BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setReceiveTime(final LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMakeStartTime(final LocalDateTime makeStartTime) {
        this.makeStartTime = makeStartTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMakeCompleteTime(final LocalDateTime makeCompleteTime) {
        this.makeCompleteTime = makeCompleteTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setServeTime(final LocalDateTime serveTime) {
        this.serveTime = serveTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMaterialConsumeTime(final LocalDateTime materialConsumeTime) {
        this.materialConsumeTime = materialConsumeTime;
    }

    public void setMaterialLocked(final Integer materialLocked) {
        this.materialLocked = materialLocked;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMaterialLockTime(final LocalDateTime materialLockTime) {
        this.materialLockTime = materialLockTime;
    }

    public void setFoodTraceCodes(final String foodTraceCodes) {
        this.foodTraceCodes = foodTraceCodes;
    }

    public void setTrayId(final Long trayId) {
        this.trayId = trayId;
    }

    public void setTrayCode(final String trayCode) {
        this.trayCode = trayCode;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setTrayBindTime(final LocalDateTime trayBindTime) {
        this.trayBindTime = trayBindTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setBoundTime(final LocalDateTime boundTime) {
        this.boundTime = boundTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMakingStartTime(final LocalDateTime makingStartTime) {
        this.makingStartTime = makingStartTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setReadyTime(final LocalDateTime readyTime) {
        this.readyTime = readyTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setServedTime(final LocalDateTime servedTime) {
        this.servedTime = servedTime;
    }

    public void setLastCameraSnapshotUrl(final String lastCameraSnapshotUrl) {
        this.lastCameraSnapshotUrl = lastCameraSnapshotUrl;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof KitchenOrder)) return false;
        final KitchenOrder other = (KitchenOrder) o;
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
        final java.lang.Object this$trayId = this.getTrayId();
        final java.lang.Object other$trayId = other.getTrayId();
        if (this$trayId == null ? other$trayId != null : !this$trayId.equals(other$trayId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$pickupNumber = this.getPickupNumber();
        final java.lang.Object other$pickupNumber = other.getPickupNumber();
        if (this$pickupNumber == null ? other$pickupNumber != null : !this$pickupNumber.equals(other$pickupNumber)) return false;
        final java.lang.Object this$pickupCode = this.getPickupCode();
        final java.lang.Object other$pickupCode = other.getPickupCode();
        if (this$pickupCode == null ? other$pickupCode != null : !this$pickupCode.equals(other$pickupCode)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$dishItems = this.getDishItems();
        final java.lang.Object other$dishItems = other.getDishItems();
        if (this$dishItems == null ? other$dishItems != null : !this$dishItems.equals(other$dishItems)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
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
        final java.lang.Object this$trayCode = this.getTrayCode();
        final java.lang.Object other$trayCode = other.getTrayCode();
        if (this$trayCode == null ? other$trayCode != null : !this$trayCode.equals(other$trayCode)) return false;
        final java.lang.Object this$trayBindTime = this.getTrayBindTime();
        final java.lang.Object other$trayBindTime = other.getTrayBindTime();
        if (this$trayBindTime == null ? other$trayBindTime != null : !this$trayBindTime.equals(other$trayBindTime)) return false;
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
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof KitchenOrder;
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
        final java.lang.Object $trayId = this.getTrayId();
        result = result * PRIME + ($trayId == null ? 43 : $trayId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $pickupNumber = this.getPickupNumber();
        result = result * PRIME + ($pickupNumber == null ? 43 : $pickupNumber.hashCode());
        final java.lang.Object $pickupCode = this.getPickupCode();
        result = result * PRIME + ($pickupCode == null ? 43 : $pickupCode.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $dishItems = this.getDishItems();
        result = result * PRIME + ($dishItems == null ? 43 : $dishItems.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
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
        final java.lang.Object $trayCode = this.getTrayCode();
        result = result * PRIME + ($trayCode == null ? 43 : $trayCode.hashCode());
        final java.lang.Object $trayBindTime = this.getTrayBindTime();
        result = result * PRIME + ($trayBindTime == null ? 43 : $trayBindTime.hashCode());
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
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "KitchenOrder(id=" + this.getId() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", pickupNumber=" + this.getPickupNumber() + ", pickupCode=" + this.getPickupCode() + ", orderType=" + this.getOrderType() + ", tableNumber=" + this.getTableNumber() + ", dishItems=" + this.getDishItems() + ", totalDishes=" + this.getTotalDishes() + ", totalAmount=" + this.getTotalAmount() + ", paymentMethod=" + this.getPaymentMethod() + ", priority=" + this.getPriority() + ", status=" + this.getStatus() + ", receiveTime=" + this.getReceiveTime() + ", makeStartTime=" + this.getMakeStartTime() + ", makeCompleteTime=" + this.getMakeCompleteTime() + ", serveTime=" + this.getServeTime() + ", cancelTime=" + this.getCancelTime() + ", cancelReason=" + this.getCancelReason() + ", chefId=" + this.getChefId() + ", chefName=" + this.getChefName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", materialConsumed=" + this.getMaterialConsumed() + ", materialConsumeTime=" + this.getMaterialConsumeTime() + ", materialLocked=" + this.getMaterialLocked() + ", materialLockTime=" + this.getMaterialLockTime() + ", foodTraceCodes=" + this.getFoodTraceCodes() + ", trayId=" + this.getTrayId() + ", trayCode=" + this.getTrayCode() + ", trayBindTime=" + this.getTrayBindTime() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
