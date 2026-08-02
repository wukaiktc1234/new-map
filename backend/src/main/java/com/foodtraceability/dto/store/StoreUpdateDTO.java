package com.foodtraceability.dto.store;

import jakarta.validation.constraints.*;

/**
 * 门店更新DTO
 * 用于接收更新门店的请求参数，所有字段可选（仅更新非 null 字段）
 */
public class StoreUpdateDTO {

    /** 门店名称 */
    @Size(min = 2, max = 100, message = "门店名称长度必须在2-100个字符之间")
    private String storeName;

    /**
     * 门店类型
     * 1-直营 2-加盟 3-合作
     */
    @Min(value = 1, message = "门店类型值无效")
    @Max(value = 3, message = "门店类型值无效")
    private Integer storeType;

    /** 门店地址 */
    @Size(max = 500, message = "地址长度不能超过500个字符")
    private String address;

    /** 联系电话 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$|^\\d{3,4}-?\\d{7,8}$", message = "电话号码格式不正确")
    private String phone;

    /** 营业开始时间（HH:mm:ss格式） */
    @Pattern(regexp = "^$|^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$", message = "营业时间格式不正确")
    private String businessHoursStart;

    /** 营业结束时间（HH:mm:ss格式） */
    @Pattern(regexp = "^$|^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$", message = "营业时间格式不正确")
    private String businessHoursEnd;

    /** 面积（平方米） */
    @DecimalMin(value = "0.01", message = "面积必须大于0")
    @DecimalMax(value = "999999.99", message = "面积超出范围")
    private java.math.BigDecimal areaSize;

    /**
     * 所属区域标识
     * 业务可选字段，前端下拉选项 value（如 beijing/shanghai/guangzhou/shenzhen/hangzhou/chengdu）。
     */
    @Size(max = 50, message = "区域标识长度不能超过50个字符")
    private String area;

    /** 店长ID（关联 employees.employee_id） */
    private String managerId;

    /** 所属部门ID */
    private String departmentId;

    /** 所属组织架构节点ID */
    private String orgId;

    /** 开业日期 */
    private java.time.LocalDate openDate;

    /**
     * 门店状态
     * 1-营业中 2-装修中 3-暂停营业 4-已关闭
     */
    @Min(value = 1, message = "状态值无效")
    @Max(value = 4, message = "状态值无效")
    private Integer status;

    /** 食品经营许可证号 */
    @Size(max = 100, message = "许可证号长度不能超过100个字符")
    private String licenseNo;

    /** 许可证到期日 */
    private java.time.LocalDate licenseExpiry;

    /** 门店配置（JSON格式） */
    private String configJson;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getStoreType() {
        return storeType;
    }

    public void setStoreType(Integer storeType) {
        this.storeType = storeType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBusinessHoursStart() {
        return businessHoursStart;
    }

    public void setBusinessHoursStart(String businessHoursStart) {
        this.businessHoursStart = businessHoursStart;
    }

    public String getBusinessHoursEnd() {
        return businessHoursEnd;
    }

    public void setBusinessHoursEnd(String businessHoursEnd) {
        this.businessHoursEnd = businessHoursEnd;
    }

    public java.math.BigDecimal getAreaSize() {
        return areaSize;
    }

    public void setAreaSize(java.math.BigDecimal areaSize) {
        this.areaSize = areaSize;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public java.time.LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(java.time.LocalDate openDate) {
        this.openDate = openDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public java.time.LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(java.time.LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
