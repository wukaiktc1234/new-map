package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * 用户更新DTO
 * 用于更新用户信息的请求参数
 */
@Schema(description = "用户更新DTO")
public class UserUpdateDTO {

    @Schema(description = "用户姓名", example = "张三")
    private String fullName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "用户邮箱", example = "zhangsan@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "用户手机号", example = "13800138000")
    private String phone;

    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 1, message = "状态值不能大于1")
    @Schema(description = "用户状态（1: 正常, 0: 禁用）", example = "1")
    private Integer status;

    @Schema(description = "所属部门ID", example = "1234567890")
    private Long departmentId;

    @Schema(description = "所属门店ID", example = "1234567890")
    private Long storeId;

    @Schema(description = "员工编号", example = "EMP2024001")
    private String employeeCode;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    public UserUpdateDTO() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
