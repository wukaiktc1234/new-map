package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户信息请求DTO
 */
@Schema(description = "更新用户信息请求")
public class UpdateUserRequest {
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    @Schema(description = "用户姓名", example = "张三")
    private String name;
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;
    @Schema(description = "门店ID", example = "1234567890")
    private String storeId;
    @Schema(description = "门店名称", example = "中心店")
    private String storeName;
    @Schema(description = "部门ID", example = "1234567890")
    private String departmentId;
    @Schema(description = "部门名称", example = "生产部")
    private String department;

    public UpdateUserRequest() {
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartment() {
        return this.department;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartment(final String department) {
        this.department = department;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UpdateUserRequest)) return false;
        final UpdateUserRequest other = (UpdateUserRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$avatar = this.getAvatar();
        final java.lang.Object other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$department = this.getDepartment();
        final java.lang.Object other$department = other.getDepartment();
        if (this$department == null ? other$department != null : !this$department.equals(other$department)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UpdateUserRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $avatar = this.getAvatar();
        result = result * PRIME + ($avatar == null ? 43 : $avatar.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $department = this.getDepartment();
        result = result * PRIME + ($department == null ? 43 : $department.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "UpdateUserRequest(name=" + this.getName() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", avatar=" + this.getAvatar() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", departmentId=" + this.getDepartmentId() + ", department=" + this.getDepartment() + ")";
    }
}
