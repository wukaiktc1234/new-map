package com.foodtraceability.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 仓库更新请求DTO
 */
public class WarehouseUpdateDTO {
    /**
     * 仓库名称
     */
    @Size(max = 100, message = "仓库名称长度不能超过100个字符")
    private String warehouseName;

    /**
     * 仓库类型（1:主仓 2:冷库 3:冻库 4:常温库）
     */
    private Integer warehouseType;

    /**
     * 仓库地址
     */
    @Size(max = 500, message = "仓库地址长度不能超过500个字符")
    private String address;

    /**
     * 仓库管理员ID
     */
    private Long managerId;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    /**
     * 库容
     */
    private BigDecimal capacity;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
