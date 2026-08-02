package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店实体类（增强版）
 * 用于管理门店的详细信息，包括营业时间、面积、许可证等
 */
@TableName("stores_new")
public class StoreNew {

    /** 门店ID */
    @TableId(type = IdType.AUTO)
    private Long storeId;

    /** 门店编码（唯一） */
    private String storeCode;

    /** 门店名称 */
    private String storeName;

    /**
     * 门店类型
     * 1-直营 2-加盟 3-合作
     */
    private Integer storeType;

    /** 门店地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 营业开始时间 */
    private java.time.LocalTime businessHoursStart;

    /** 营业结束时间 */
    private java.time.LocalTime businessHoursEnd;

    /** 面积（平方米） */
    private BigDecimal areaSize;

    /**
     * 所属区域标识
     * 存储前端下拉选项 value（如 beijing/shanghai/guangzhou/shenzhen/hangzhou/chengdu），
     * 业务可扩展为自定义区域编码。
     */
    private String area;

    /** 店长ID（关联 employees.employee_id，雪花算法字符串） */
    private String managerId;

    /** 所属部门ID */
    @TableField("department_id")
    private String departmentId;

    /** 所属组织架构节点ID */
    @TableField("org_id")
    private String orgId;

    /** 开业日期 */
    private LocalDate openDate;

    /**
     * 门店状态
     * 1-营业中 2-装修中 3-暂停营业 4-已关闭
     */
    private Integer status;

    /** 食品经营许可证号 */
    private String licenseNo;

    /** 许可证到期日 */
    private LocalDate licenseExpiry;

    /**
     * 门店配置（JSON格式）
     * 包含桌台数量、收银机等配置信息
     */
    private String configJson;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

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

    public java.time.LocalTime getBusinessHoursStart() {
        return businessHoursStart;
    }

    public void setBusinessHoursStart(java.time.LocalTime businessHoursStart) {
        this.businessHoursStart = businessHoursStart;
    }

    public java.time.LocalTime getBusinessHoursEnd() {
        return businessHoursEnd;
    }

    public void setBusinessHoursEnd(java.time.LocalTime businessHoursEnd) {
        this.businessHoursEnd = businessHoursEnd;
    }

    public BigDecimal getAreaSize() {
        return areaSize;
    }

    public void setAreaSize(BigDecimal areaSize) {
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

    public LocalDate getOpenDate() {
        return openDate;
    }

    public void setOpenDate(LocalDate openDate) {
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

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
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
}
