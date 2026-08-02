package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 食品追溯码实体类
 * 对外使用，类似奶茶杯标签，用于追踪从制作到出餐的完整链路
 */
@TableName("food_trace_code")
@Schema(description = "食品追溯码实体")
public class FoodTraceCode {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID", example = "FTC20250101001")
    private String traceCodeId;
    @TableField("trace_code")
    @Schema(description = "追溯码（唯一编码）", example = "FTC20250101001ABC")
    private String traceCode;
    @TableField("qr_code_url")
    @Schema(description = "二维码图片URL")
    private String qrCodeUrl;
    @TableField("order_id")
    @Schema(description = "订单ID", example = "O20250101001")
    private String orderId;
    @TableField("order_no")
    @Schema(description = "订单编号", example = "ORD202501010001")
    private String orderNumber;
    @TableField("order_type")
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提", example = "0")
    private Integer orderType;
    @TableField("dish_id")
    @Schema(description = "菜品ID", example = "D001")
    private String dishId;
    @TableField("dish_name")
    @Schema(description = "菜品名称", example = "宫保鸡丁")
    private String dishName;
    @TableField("dish_price")
    @Schema(description = "菜品价格", example = "38.00")
    private BigDecimal dishPrice;
    @TableField("quantity")
    @Schema(description = "数量", example = "1")
    private Integer quantity;
    @TableField("production_station")
    @Schema(description = "生产工位：main_workstation-操作台，steam_oven-蒸箱，oven-烤箱，fryer-炸炉等")
    private String productionStation;
    @TableField("produce_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "生产时间")
    private LocalDateTime produceTime;
    @TableField("inventory_deduction")
    @Schema(description = "库存扣减记录（JSON）")
    private String inventoryDeduction;
    @TableField("material_trace_codes")
    @Schema(description = "使用的原料追溯码列表（JSON数组）")
    private String materialTraceCodes;
    @TableField("material_details")
    @Schema(description = "原料详情（JSON数组，包含原料名称、数量等）")
    private String materialDetails;
    @TableField("kitchen_order_id")
    @Schema(description = "后厨订单ID")
    private Long kitchenOrderId;
    @TableField("make_status")
    @Schema(description = "制作状态：pending-待制作, making-制作中, completed-已完成, served-已出餐", example = "pending")
    private String makeStatus;
    @TableField("make_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始制作时间")
    private LocalDateTime makeStartTime;
    @TableField("make_complete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "制作完成时间")
    private LocalDateTime makeCompleteTime;
    @TableField("chef_id")
    @Schema(description = "制作人员ID")
    private Long chefId;
    @TableField("chef_name")
    @Schema(description = "制作人员姓名", example = "李师傅")
    private String chefName;
    @TableField("serve_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "出餐时间")
    private LocalDateTime serveTime;
    @TableField("serve_method")
    @Schema(description = "出餐方式：dine_in-堂食, takeout-外卖, pickup-自提", example = "dine_in")
    private String serveMethod;
    @TableField("table_number")
    @Schema(description = "桌号（堂食时使用）", example = "A01")
    private String tableNumber;
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;
    @TableField("store_name")
    @Schema(description = "门店名称", example = "总店")
    private String storeName;
    @TableField("status")
    @Schema(description = "状态：created-已创建, printed-已打印, served-已出餐, expired-已过期", example = "created")
    private String status;
    @TableField("print_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "打印时间")
    private LocalDateTime printTime;
    @TableField("print_count")
    @Schema(description = "打印次数", example = "1")
    private Integer printCount;
    @TableField("material_cost")
    @Schema(description = "原料成本", example = "15.00")
    private BigDecimal materialCost;
    @TableField("labor_cost")
    @Schema(description = "人工成本", example = "5.00")
    private BigDecimal laborCost;
    @TableField("total_cost")
    @Schema(description = "总成本", example = "20.00")
    private BigDecimal totalCost;
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

    public FoodTraceCode() {
    }

    public Long getId() {
        return this.id;
    }

    public String getTraceCodeId() {
        return this.traceCodeId;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getQrCodeUrl() {
        return this.qrCodeUrl;
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

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public BigDecimal getDishPrice() {
        return this.dishPrice;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public String getProductionStation() {
        return this.productionStation;
    }

    public LocalDateTime getProduceTime() {
        return this.produceTime;
    }

    public String getInventoryDeduction() {
        return this.inventoryDeduction;
    }

    public String getMaterialTraceCodes() {
        return this.materialTraceCodes;
    }

    public String getMaterialDetails() {
        return this.materialDetails;
    }

    public Long getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public String getMakeStatus() {
        return this.makeStatus;
    }

    public LocalDateTime getMakeStartTime() {
        return this.makeStartTime;
    }

    public LocalDateTime getMakeCompleteTime() {
        return this.makeCompleteTime;
    }

    public Long getChefId() {
        return this.chefId;
    }

    public String getChefName() {
        return this.chefName;
    }

    public LocalDateTime getServeTime() {
        return this.serveTime;
    }

    public String getServeMethod() {
        return this.serveMethod;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getPrintTime() {
        return this.printTime;
    }

    public Integer getPrintCount() {
        return this.printCount;
    }

    public BigDecimal getMaterialCost() {
        return this.materialCost;
    }

    public BigDecimal getLaborCost() {
        return this.laborCost;
    }

    public BigDecimal getTotalCost() {
        return this.totalCost;
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

    public void setTraceCodeId(final String traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setQrCodeUrl(final String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
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

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setDishPrice(final BigDecimal dishPrice) {
        this.dishPrice = dishPrice;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setProductionStation(final String productionStation) {
        this.productionStation = productionStation;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setProduceTime(final LocalDateTime produceTime) {
        this.produceTime = produceTime;
    }

    public void setInventoryDeduction(final String inventoryDeduction) {
        this.inventoryDeduction = inventoryDeduction;
    }

    public void setMaterialTraceCodes(final String materialTraceCodes) {
        this.materialTraceCodes = materialTraceCodes;
    }

    public void setMaterialDetails(final String materialDetails) {
        this.materialDetails = materialDetails;
    }

    public void setKitchenOrderId(final Long kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setMakeStatus(final String makeStatus) {
        this.makeStatus = makeStatus;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMakeStartTime(final LocalDateTime makeStartTime) {
        this.makeStartTime = makeStartTime;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setMakeCompleteTime(final LocalDateTime makeCompleteTime) {
        this.makeCompleteTime = makeCompleteTime;
    }

    public void setChefId(final Long chefId) {
        this.chefId = chefId;
    }

    public void setChefName(final String chefName) {
        this.chefName = chefName;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setServeTime(final LocalDateTime serveTime) {
        this.serveTime = serveTime;
    }

    public void setServeMethod(final String serveMethod) {
        this.serveMethod = serveMethod;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public void setPrintTime(final LocalDateTime printTime) {
        this.printTime = printTime;
    }

    public void setPrintCount(final Integer printCount) {
        this.printCount = printCount;
    }

    public void setMaterialCost(final BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    public void setLaborCost(final BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public void setTotalCost(final BigDecimal totalCost) {
        this.totalCost = totalCost;
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
        if (!(o instanceof FoodTraceCode)) return false;
        final FoodTraceCode other = (FoodTraceCode) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$chefId = this.getChefId();
        final java.lang.Object other$chefId = other.getChefId();
        if (this$chefId == null ? other$chefId != null : !this$chefId.equals(other$chefId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$printCount = this.getPrintCount();
        final java.lang.Object other$printCount = other.getPrintCount();
        if (this$printCount == null ? other$printCount != null : !this$printCount.equals(other$printCount)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$traceCodeId = this.getTraceCodeId();
        final java.lang.Object other$traceCodeId = other.getTraceCodeId();
        if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$qrCodeUrl = this.getQrCodeUrl();
        final java.lang.Object other$qrCodeUrl = other.getQrCodeUrl();
        if (this$qrCodeUrl == null ? other$qrCodeUrl != null : !this$qrCodeUrl.equals(other$qrCodeUrl)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$dishPrice = this.getDishPrice();
        final java.lang.Object other$dishPrice = other.getDishPrice();
        if (this$dishPrice == null ? other$dishPrice != null : !this$dishPrice.equals(other$dishPrice)) return false;
        final java.lang.Object this$productionStation = this.getProductionStation();
        final java.lang.Object other$productionStation = other.getProductionStation();
        if (this$productionStation == null ? other$productionStation != null : !this$productionStation.equals(other$productionStation)) return false;
        final java.lang.Object this$produceTime = this.getProduceTime();
        final java.lang.Object other$produceTime = other.getProduceTime();
        if (this$produceTime == null ? other$produceTime != null : !this$produceTime.equals(other$produceTime)) return false;
        final java.lang.Object this$inventoryDeduction = this.getInventoryDeduction();
        final java.lang.Object other$inventoryDeduction = other.getInventoryDeduction();
        if (this$inventoryDeduction == null ? other$inventoryDeduction != null : !this$inventoryDeduction.equals(other$inventoryDeduction)) return false;
        final java.lang.Object this$materialTraceCodes = this.getMaterialTraceCodes();
        final java.lang.Object other$materialTraceCodes = other.getMaterialTraceCodes();
        if (this$materialTraceCodes == null ? other$materialTraceCodes != null : !this$materialTraceCodes.equals(other$materialTraceCodes)) return false;
        final java.lang.Object this$materialDetails = this.getMaterialDetails();
        final java.lang.Object other$materialDetails = other.getMaterialDetails();
        if (this$materialDetails == null ? other$materialDetails != null : !this$materialDetails.equals(other$materialDetails)) return false;
        final java.lang.Object this$makeStatus = this.getMakeStatus();
        final java.lang.Object other$makeStatus = other.getMakeStatus();
        if (this$makeStatus == null ? other$makeStatus != null : !this$makeStatus.equals(other$makeStatus)) return false;
        final java.lang.Object this$makeStartTime = this.getMakeStartTime();
        final java.lang.Object other$makeStartTime = other.getMakeStartTime();
        if (this$makeStartTime == null ? other$makeStartTime != null : !this$makeStartTime.equals(other$makeStartTime)) return false;
        final java.lang.Object this$makeCompleteTime = this.getMakeCompleteTime();
        final java.lang.Object other$makeCompleteTime = other.getMakeCompleteTime();
        if (this$makeCompleteTime == null ? other$makeCompleteTime != null : !this$makeCompleteTime.equals(other$makeCompleteTime)) return false;
        final java.lang.Object this$chefName = this.getChefName();
        final java.lang.Object other$chefName = other.getChefName();
        if (this$chefName == null ? other$chefName != null : !this$chefName.equals(other$chefName)) return false;
        final java.lang.Object this$serveTime = this.getServeTime();
        final java.lang.Object other$serveTime = other.getServeTime();
        if (this$serveTime == null ? other$serveTime != null : !this$serveTime.equals(other$serveTime)) return false;
        final java.lang.Object this$serveMethod = this.getServeMethod();
        final java.lang.Object other$serveMethod = other.getServeMethod();
        if (this$serveMethod == null ? other$serveMethod != null : !this$serveMethod.equals(other$serveMethod)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$printTime = this.getPrintTime();
        final java.lang.Object other$printTime = other.getPrintTime();
        if (this$printTime == null ? other$printTime != null : !this$printTime.equals(other$printTime)) return false;
        final java.lang.Object this$materialCost = this.getMaterialCost();
        final java.lang.Object other$materialCost = other.getMaterialCost();
        if (this$materialCost == null ? other$materialCost != null : !this$materialCost.equals(other$materialCost)) return false;
        final java.lang.Object this$laborCost = this.getLaborCost();
        final java.lang.Object other$laborCost = other.getLaborCost();
        if (this$laborCost == null ? other$laborCost != null : !this$laborCost.equals(other$laborCost)) return false;
        final java.lang.Object this$totalCost = this.getTotalCost();
        final java.lang.Object other$totalCost = other.getTotalCost();
        if (this$totalCost == null ? other$totalCost != null : !this$totalCost.equals(other$totalCost)) return false;
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
        return other instanceof FoodTraceCode;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $chefId = this.getChefId();
        result = result * PRIME + ($chefId == null ? 43 : $chefId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $printCount = this.getPrintCount();
        result = result * PRIME + ($printCount == null ? 43 : $printCount.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $traceCodeId = this.getTraceCodeId();
        result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $qrCodeUrl = this.getQrCodeUrl();
        result = result * PRIME + ($qrCodeUrl == null ? 43 : $qrCodeUrl.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $dishPrice = this.getDishPrice();
        result = result * PRIME + ($dishPrice == null ? 43 : $dishPrice.hashCode());
        final java.lang.Object $productionStation = this.getProductionStation();
        result = result * PRIME + ($productionStation == null ? 43 : $productionStation.hashCode());
        final java.lang.Object $produceTime = this.getProduceTime();
        result = result * PRIME + ($produceTime == null ? 43 : $produceTime.hashCode());
        final java.lang.Object $inventoryDeduction = this.getInventoryDeduction();
        result = result * PRIME + ($inventoryDeduction == null ? 43 : $inventoryDeduction.hashCode());
        final java.lang.Object $materialTraceCodes = this.getMaterialTraceCodes();
        result = result * PRIME + ($materialTraceCodes == null ? 43 : $materialTraceCodes.hashCode());
        final java.lang.Object $materialDetails = this.getMaterialDetails();
        result = result * PRIME + ($materialDetails == null ? 43 : $materialDetails.hashCode());
        final java.lang.Object $makeStatus = this.getMakeStatus();
        result = result * PRIME + ($makeStatus == null ? 43 : $makeStatus.hashCode());
        final java.lang.Object $makeStartTime = this.getMakeStartTime();
        result = result * PRIME + ($makeStartTime == null ? 43 : $makeStartTime.hashCode());
        final java.lang.Object $makeCompleteTime = this.getMakeCompleteTime();
        result = result * PRIME + ($makeCompleteTime == null ? 43 : $makeCompleteTime.hashCode());
        final java.lang.Object $chefName = this.getChefName();
        result = result * PRIME + ($chefName == null ? 43 : $chefName.hashCode());
        final java.lang.Object $serveTime = this.getServeTime();
        result = result * PRIME + ($serveTime == null ? 43 : $serveTime.hashCode());
        final java.lang.Object $serveMethod = this.getServeMethod();
        result = result * PRIME + ($serveMethod == null ? 43 : $serveMethod.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $printTime = this.getPrintTime();
        result = result * PRIME + ($printTime == null ? 43 : $printTime.hashCode());
        final java.lang.Object $materialCost = this.getMaterialCost();
        result = result * PRIME + ($materialCost == null ? 43 : $materialCost.hashCode());
        final java.lang.Object $laborCost = this.getLaborCost();
        result = result * PRIME + ($laborCost == null ? 43 : $laborCost.hashCode());
        final java.lang.Object $totalCost = this.getTotalCost();
        result = result * PRIME + ($totalCost == null ? 43 : $totalCost.hashCode());
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
        return "FoodTraceCode(id=" + this.getId() + ", traceCodeId=" + this.getTraceCodeId() + ", traceCode=" + this.getTraceCode() + ", qrCodeUrl=" + this.getQrCodeUrl() + ", orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", dishPrice=" + this.getDishPrice() + ", quantity=" + this.getQuantity() + ", productionStation=" + this.getProductionStation() + ", produceTime=" + this.getProduceTime() + ", inventoryDeduction=" + this.getInventoryDeduction() + ", materialTraceCodes=" + this.getMaterialTraceCodes() + ", materialDetails=" + this.getMaterialDetails() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", makeStatus=" + this.getMakeStatus() + ", makeStartTime=" + this.getMakeStartTime() + ", makeCompleteTime=" + this.getMakeCompleteTime() + ", chefId=" + this.getChefId() + ", chefName=" + this.getChefName() + ", serveTime=" + this.getServeTime() + ", serveMethod=" + this.getServeMethod() + ", tableNumber=" + this.getTableNumber() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", status=" + this.getStatus() + ", printTime=" + this.getPrintTime() + ", printCount=" + this.getPrintCount() + ", materialCost=" + this.getMaterialCost() + ", laborCost=" + this.getLaborCost() + ", totalCost=" + this.getTotalCost() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
