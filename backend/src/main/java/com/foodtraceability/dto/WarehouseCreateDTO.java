package com.foodtraceability.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 仓库创建请求DTO
 */
public class WarehouseCreateDTO {
    /**
     * 仓库编码（唯一）
     */
    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 32, message = "仓库编码长度不能超过32个字符")
    private String warehouseCode;

    /**
     * 仓库名称
     */
    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过100个字符")
    private String warehouseName;

    /**
     * 仓库类型（1:主仓 2:冷库 3:冻库 4:常温库）
     */
    @NotNull(message = "仓库类型不能为空")
    @Min(value = 1, message = "仓库类型无效")
    @Max(value = 4, message = "仓库类型无效")
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
    @Pattern(regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$", message = "联系电话格式不正确")
    private String phone;

    /**
     * 库容
     */
    @DecimalMin(value = "0", message = "库容不能为负数")
    private BigDecimal capacity;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

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
