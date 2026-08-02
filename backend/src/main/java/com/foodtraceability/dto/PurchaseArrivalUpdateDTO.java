package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 采购到货单更新请求
 * 仅允许更新物流信息，不影响库存和财务
 */
@Schema(description = "采购到货单更新请求")
public class PurchaseArrivalUpdateDTO {

    @Schema(description = "发货状态（shipped-已发货, in_transit-运输中, delivered-已送达）")
    private String shipmentStatus;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "运输方式")
    private String transportMode;

    @Schema(description = "车牌号")
    private String vehiclePlateNo;

    @Schema(description = "车辆类型")
    private String vehicleType;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "司机电话")
    private String driverPhone;

    @Schema(description = "运费（分）")
    private Long freightAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预计到货日期")
    private LocalDate estimatedArrivalDate;

    @Schema(description = "实际到货日期")
    private LocalDate actualArrivalDate;

    @Schema(description = "备注")
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
