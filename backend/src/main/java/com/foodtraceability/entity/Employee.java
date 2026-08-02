package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工实体类
 * 用于管理系统中的员工信息
 */
@TableName("employees")
@Schema(description = "员工实体")
public class Employee {

    /**
     * 员工ID
     * Wave 5-B：IdType 由 ASSIGN_UUID 改为 ASSIGN_ID，
     * 配合 V20260717_030 数据库 employee_id 列从 VARCHAR(50) 改为 BIGINT。
     * 雪花算法生成 Long，Java 字段类型同步为 Long 以匹配数据库 BIGINT 列。
     */
    @TableId(type = IdType.ASSIGN_ID, value = "EMPLOYEE_ID")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "员工ID", example = "1234567890")
    private Long id;

    /**
     * 员工姓名
     */
    @TableField("EMPLOYEE_NAME")
    @Schema(description = "员工姓名", example = "张三")
    private String name;

    /**
     * 员工性别
     */
    @TableField("gender")
    @Schema(description = "员工性别", example = "male")
    private String gender;

    /**
     * 员工编码（唯一）
     */
    @TableField("EMPLOYEE_CODE")
    @Schema(description = "员工编码（唯一）", example = "EMP001")
    private String employeeCode;

    /**
     * 员工手机号
     */
    @TableField("phone")
    @Schema(description = "员工手机号", example = "13800138000")
    private String phone;

    /**
     * 员工邮箱
     */
    @TableField("email")
    @Schema(description = "员工邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 所属部门ID
     */
    @TableField("department_id")
    @Schema(description = "所属部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;

    /**
     * 所属部门名称
     */
    @TableField(exist = false)
    @Schema(description = "所属部门名称", example = "技术部")
    private String departmentName;

    /**
     * 所属职位ID
     */
    @TableField("position_id")
    @Schema(description = "所属职位ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;

    /**
     * 所属职位名称
     */
    @TableField(exist = false)
    @Schema(description = "所属职位名称", example = "软件工程师")
    private String positionName;

    /**
     * 员工状态（1: 在职, 0: 离职, 2: 试用期）
     * 后端和数据库统一使用 Integer，前端通过 DataConverter 转换为语义字符串
     */
    @TableField("status")
    @Schema(description = "员工状态（1: 在职, 0: 离职, 2: 试用期）", example = "1")
    private Integer status;

    /**
     * 所属门店ID
     */
    @TableField("store_id")
    @Schema(description = "所属门店ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long storeId;

    /**
     * 工作归属类型
     */
    @TableField("work_location_type")
    @Schema(description = "工作归属类型：STORE/WAREHOUSE/HEADQUARTERS")
    private String workLocationType;

    /**
     * 归属仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "归属仓库ID")
    private Long warehouseId;

    /**
     * 入职日期
     */
    @TableField("hire_date")
    @Schema(description = "入职日期")
    private LocalDateTime hireDate;

    /**
     * 离职日期
     */
    @TableField("resign_date")
    @Schema(description = "离职日期")
    private LocalDateTime resignDate;

    /**
     * 基本薪资
     */
    @TableField("base_salary")
    @Schema(description = "基本薪资", example = "10000.00")
    private java.math.BigDecimal baseSalary;

    /**
     * 员工地址
     */
    @TableField("address")
    @Schema(description = "员工地址", example = "北京市朝阳区")
    private String address;

    /**
     * 身份证号
     */
    @TableField("id_card")
    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    /**
     * 银行卡号
     */
    @TableField("bank_card")
    @Schema(description = "银行卡号", example = "6222021234567890123")
    private String bankCard;

    /**
     * 紧急联系人
     */
    @TableField("emergency_contact")
    @Schema(description = "紧急联系人", example = "张三")
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    @TableField("emergency_phone")
    @Schema(description = "紧急联系电话", example = "13800138000")
    private String emergencyPhone;

    /**
     * 员工照片URL
     */
    @TableField("photo_url")
    @Schema(description = "员工照片URL", example = "http://example.com/photo.jpg")
    private String photoUrl;

    /**
     * 员工备注
     */
    @TableField("remark")
    @Schema(description = "员工备注", example = "备注信息")
    private String remark;

    /**
     * 健康证到期日期（由 HR 系统同步维护）
     */
    @TableField("health_certificate_expiry_date")
    @Schema(description = "健康证到期日期")
    private LocalDate healthCertificateExpiryDate;

    /**
     * 用工类型（full-time: 全职, part-time: 兼职）
     */
    @TableField("employment_type")
    @Schema(description = "用工类型", example = "full-time")
    private String employmentType;

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;

    /**
     * 创建人
     */
    @TableField("created_by")
    @Schema(description = "创建人", example = "system")
    private String createdBy;

    /**
     * 更新人
     */
    @TableField("updated_by")
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getWorkLocationType() {
        return workLocationType;
    }

    public void setWorkLocationType(String workLocationType) {
        this.workLocationType = workLocationType;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDateTime getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDateTime hireDate) {
        this.hireDate = hireDate;
    }

    public LocalDateTime getResignDate() {
        return resignDate;
    }

    public void setResignDate(LocalDateTime resignDate) {
        this.resignDate = resignDate;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(BigDecimal baseSalary) {
        this.baseSalary = baseSalary;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getBankCard() {
        return bankCard;
    }

    public void setBankCard(String bankCard) {
        this.bankCard = bankCard;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDate getHealthCertificateExpiryDate() {
        return healthCertificateExpiryDate;
    }

    public void setHealthCertificateExpiryDate(LocalDate healthCertificateExpiryDate) {
        this.healthCertificateExpiryDate = healthCertificateExpiryDate;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
