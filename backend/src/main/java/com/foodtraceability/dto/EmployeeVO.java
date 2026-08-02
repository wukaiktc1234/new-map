package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工HR视图对象（VO）
 * 用于HR管理场景下展示员工完整信息
 */
@Schema(description = "员工HR视图对象")
public class EmployeeVO {

    @Schema(description = "员工ID", example = "1234567890")
    private String id;

    @Schema(description = "员工编号", example = "EMP001")
    private String employeeNo;

    @Schema(description = "员工姓名", example = "张三")
    private String name;

    /** 性别代码: 0未知 1男 2女 */
    @Schema(description = "性别代码", example = "1")
    private Integer genderCode;

    @Schema(description = "性别名称", example = "男")
    private String genderName;

    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "部门ID", example = "1")
    private String departmentId;

    @Schema(description = "部门名称", example = "后厨部")
    private String departmentName;

    @Schema(description = "职位ID", example = "1")
    private String positionId;

    @Schema(description = "职位名称", example = "厨师")
    private String positionName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入职日期", example = "2026-01-15")
    private LocalDate entryDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "转正日期", example = "2026-04-15")
    private LocalDate regularDate;

    /** 员工类型: 1全职 2兼职 3实习 4外包 */
    @Schema(description = "员工类型", example = "1")
    private Integer employeeType;

    @Schema(description = "员工类型名称", example = "全职")
    private String employeeTypeName;

    /** 员工状态: 1在职 2试用期 3离职 4退休 */
    @Schema(description = "员工状态", example = "1")
    private Integer employeeStatus;

    @Schema(description = "员工状态名称", example = "在职")
    private String employeeStatusName;

    @Schema(description = "学历", example = "本科")
    private String education;

    @Schema(description = "紧急联系人", example = "李四")
    private String emergencyContact;

    @Schema(description = "紧急联系电话", example = "13900139000")
    private String emergencyPhone;

    @Schema(description = "银行账号", example = "6222021234567890123")
    private String bankAccount;

    @Schema(description = "地址", example = "北京市朝阳区xxx")
    private String address;

    @Schema(description = "基本薪资（元）", example = "8000.00")
    private BigDecimal baseSalary;

    @Schema(description = "备注", example = "")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getGenderCode() { return genderCode; }
    public void setGenderCode(Integer genderCode) { this.genderCode = genderCode; }
    public String getGenderName() { return genderName; }
    public void setGenderName(String genderName) { this.genderName = genderName; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }
    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public LocalDate getRegularDate() { return regularDate; }
    public void setRegularDate(LocalDate regularDate) { this.regularDate = regularDate; }
    public Integer getEmployeeType() { return employeeType; }
    public void setEmployeeType(Integer employeeType) { this.employeeType = employeeType; }
    public String getEmployeeTypeName() { return employeeTypeName; }
    public void setEmployeeTypeName(String employeeTypeName) { this.employeeTypeName = employeeTypeName; }
    public Integer getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(Integer employeeStatus) { this.employeeStatus = employeeStatus; }
    public String getEmployeeStatusName() { return employeeStatusName; }
    public void setEmployeeStatusName(String employeeStatusName) { this.employeeStatusName = employeeStatusName; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
