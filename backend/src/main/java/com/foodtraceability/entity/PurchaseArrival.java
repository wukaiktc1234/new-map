package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购到货单主表实体类
 * 用于管理采购商品的到货登记信息
 */
@TableName("purchase_arrivals")
@Schema(description = "采购到货单主表实体")
public class PurchaseArrival {

    /**
     * 到货单主键ID（自增）
     */
    @TableId(value = "arrival_id", type = IdType.AUTO)
    @Schema(description = "到货单主键ID", example = "1")
    private Long arrivalId;

    /**
     * 到货单编号
     */
    @TableField("arrival_code")
    @Schema(description = "到货单编号", example = "AR20260425001")
    private String arrivalCode;

    /**
     * 关联采购订单ID
     */
    @TableField("order_id")
    @Schema(description = "关联采购订单ID", example = "1")
    private Long orderId;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /**
     * 收货方类型：STORE 门店 / WAREHOUSE 仓库
     */
    @TableField("receiver_type")
    @Schema(description = "收货方类型（STORE-门店, WAREHOUSE-仓库）")
    private String receiverType;

    /**
     * 收货门店ID
     */
    @TableField("store_id")
    @Schema(description = "收货门店ID")
    private String storeId;

    /**
     * 收货仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "收货仓库ID", example = "1")
    private Long warehouseId;

    /**
     * 发货状态
     */
    @TableField("shipment_status")
    @Schema(description = "发货状态")
    private String shipmentStatus;

    /**
     * 物流单号
     */
    @TableField("logistics_no")
    @Schema(description = "物流单号")
    private String logisticsNo;

    /**
     * 物流公司
     */
    @TableField("logistics_company")
    @Schema(description = "物流公司")
    private String logisticsCompany;

    /**
     * 运输方式
     */
    @TableField("transport_mode")
    @Schema(description = "运输方式")
    private String transportMode;

    /**
     * 车牌号
     */
    @TableField("vehicle_plate_no")
    @Schema(description = "车牌号")
    private String vehiclePlateNo;

    /**
     * 车辆类型
     */
    @TableField("vehicle_type")
    @Schema(description = "车辆类型")
    private String vehicleType;

    /**
     * 司机姓名
     */
    @TableField("driver_name")
    @Schema(description = "司机姓名")
    private String driverName;

    /**
     * 司机电话
     */
    @TableField("driver_phone")
    @Schema(description = "司机电话")
    private String driverPhone;

    /**
     * 运费（单位：分）
     */
    @TableField("freight_amount")
    @Schema(description = "运费（分）")
    private Long freightAmount;

    /**
     * 预计到货日期
     */
    @TableField("estimated_arrival_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预计到货日期")
    private LocalDate estimatedArrivalDate;

    /**
     * 实际到货日期
     */
    @TableField("actual_arrival_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "实际到货日期")
    private LocalDate actualArrivalDate;

    /**
     * 到货总数量
     */
    @TableField("total_quantity")
    @Schema(description = "到货总数量", example = "100.000")
    private BigDecimal totalQuantity;

    /**
     * 到货总金额（单位：分）
     */
    @TableField("total_amount")
    @Schema(description = "到货总金额（分）", example = "30000")
    private Long totalAmount;

    /**
     * 已确认收货数量
     */
    @TableField("received_quantity")
    @Schema(description = "已确认收货数量", example = "50.000")
    private BigDecimal receivedQuantity;

    /**
     * 到货单状态（0-PENDING 1-RECEIVING 2-PARTIAL_RECEIVED 3-RECEIVED 4-CLOSED）
     */
    @TableField("status")
    @Schema(description = "到货单状态（0-待收货, 1-收货中, 2-部分收货, 3-已收货, 4-已关闭）", example = "0")
    private Integer status;

    /**
     * 关闭原因
     */
    @TableField("close_reason")
    @Schema(description = "关闭原因")
    private String closeReason;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    @Schema(description = "创建人ID", example = "1")
    private Long createUserId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /**
     * 采购订单编号（非数据库字段，用于前端展示）
     */
    @TableField(exist = false)
    @Schema(description = "采购订单编号")
    private String orderCode;
    /** 质检结果：0待检 1通过 2失败 */
    private Integer qualityCheckResult;
    /** 质检备注 */
    private String qualityCheckRemark;
    /** 入库确认时间 */
    private LocalDateTime confirmTime;

    /**
     * 供应商名称（非数据库字段，用于前端展示）
     */
    @TableField(exist = false)
    @Schema(description = "供应商名称")
    private String supplierName;

    /**
     * 到货明细列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "到货明细列表")
    private List<PurchaseArrivalItem> items;

    // ==================== Getter & Setter ====================

    public Long getArrivalId() {
        return arrivalId;
    }

    public void setArrivalId(Long arrivalId) {
        this.arrivalId = arrivalId;
    }

    public String getArrivalCode() {
        return arrivalCode;
    }

    public void setArrivalCode(String arrivalCode) {
        this.arrivalCode = arrivalCode;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getLogisticsNo() {
        return logisticsNo;
    }

    public void setLogisticsNo(String logisticsNo) {
        this.logisticsNo = logisticsNo;
    }

    public String getLogisticsCompany() {
        return logisticsCompany;
    }

    public void setLogisticsCompany(String logisticsCompany) {
        this.logisticsCompany = logisticsCompany;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getVehiclePlateNo() {
        return vehiclePlateNo;
    }

    public void setVehiclePlateNo(String vehiclePlateNo) {
        this.vehiclePlateNo = vehiclePlateNo;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public Long getFreightAmount() {
        return freightAmount;
    }

    public void setFreightAmount(Long freightAmount) {
        this.freightAmount = freightAmount;
    }

    public LocalDate getEstimatedArrivalDate() {
        return estimatedArrivalDate;
    }

    public void setEstimatedArrivalDate(LocalDate estimatedArrivalDate) {
        this.estimatedArrivalDate = estimatedArrivalDate;
    }

    public LocalDate getActualArrivalDate() {
        return actualArrivalDate;
    }

    public void setActualArrivalDate(LocalDate actualArrivalDate) {
        this.actualArrivalDate = actualArrivalDate;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getQualityCheckRemark() {
        return qualityCheckRemark;
    }

    public void setQualityCheckRemark(String qualityCheckRemark) {
        this.qualityCheckRemark = qualityCheckRemark;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public List<PurchaseArrivalItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseArrivalItem> items) {
        this.items = items;
    }
}
