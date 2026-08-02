package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 供应商查询DTO
 * 用于接收供应商列表的查询条件
 */
@Schema(description = "供应商查询条件")
public class SupplierQueryDTO {

    /**
     * 供应商名称（模糊查询）
     */
    @Schema(description = "供应商名称（模糊查询）")
    private String supplierName;

    /**
     * 联系人（模糊查询）
     */
    @Schema(description = "联系人（模糊查询）")
    private String contactPerson;

    /**
     * 电话（模糊查询）
     */
    @Schema(description = "电话（模糊查询）")
    private String phone;

    /**
     * 分类
     */
    @Schema(description = "分类（原材料/包装/设备/其他）")
    private String category;

    /**
     * 状态（1合作中 0停用 2黑名单）
     */
    @Schema(description = "状态（1-合作中, 0-停用, 2-黑名单）")
    private Integer status;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    // ==================== Getter & Setter ====================

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
