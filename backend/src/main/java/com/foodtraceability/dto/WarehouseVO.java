package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 仓库视图对象VO
 * 用于返回给前端的仓库信息
 */
public class WarehouseVO {
    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库编码
     */
    private String warehouseCode;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 仓库类型
     */
    private Integer warehouseType;

    /**
     * 仓库类型名称
     */
    private String warehouseTypeName;

    /**
     * 仓库地址
     */
    private String address;

    /**
     * 仓库管理员ID
     */
    private Long managerId;

    /**
     * 仓库管理员名称
     */
    private String managerName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 库容
     */
    private BigDecimal capacity;

    /**
     * 已使用容量
     */
    private BigDecimal usedCapacity;

    /**
     * 容量使用率
     */
    private BigDecimal capacityUsageRate;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // Getter和Setter方法
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Integer getWarehouseType() {
        return warehouseType;
    }

    public void setWarehouseType(Integer warehouseType) {
        this.warehouseType = warehouseType;
    }

    public String getWarehouseTypeName() {
        return warehouseTypeName;
    }

    public void setWarehouseTypeName(String warehouseTypeName) {
        this.warehouseTypeName = warehouseTypeName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(BigDecimal usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public BigDecimal getCapacityUsageRate() {
        return capacityUsageRate;
    }

    public void setCapacityUsageRate(BigDecimal capacityUsageRate) {
        this.capacityUsageRate = capacityUsageRate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
}
