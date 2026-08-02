package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息响应DTO
 */
@Schema(description = "用户信息响应")
public class UserInfoResponse {
    @Schema(description = "用户ID", example = "1234567890")
    private String id;
    @Schema(description = "用户名", example = "admin")
    private String username;
    @Schema(description = "用户姓名", example = "管理员")
    private String name;
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    @Schema(description = "用户状态（active: 正常, inactive: 禁用, locked: 锁定）", example = "active")
    private Integer status;
    @Schema(description = "角色列表", example = "[\"admin\", \"user\"]")
    private List<String> roles;
    @Schema(description = "权限列表", example = "[\"user:manage\", \"product:view\"]")
    private List<String> permissions;
    @Schema(description = "创建时间", example = "2023-01-01T12:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间", example = "2023-01-01T12:00:00")
    private LocalDateTime updatedAt;
    @Schema(description = "门店ID", example = "1234567890")
    private Long storeId;
    @Schema(description = "门店名称", example = "中心店")
    private String storeName;
    @Schema(description = "部门ID", example = "1234567890")
    private Long departmentId;
    @Schema(description = "部门名称", example = "生产部")
    private String department;
    @Schema(description = "公司ID", example = "1234567890")
    private String companyId;
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    public UserInfoResponse() {
    }

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
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

    public Integer getStatus() {
        return this.status;
    }

    public List<String> getRoles() {
        return this.roles;
    }

    public List<String> getPermissions() {
        return this.permissions;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartment() {
        return this.department;
    }

    public String getCompanyId() {
        return this.companyId;
    }

    public String getAvatar() {
        return this.avatar;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setUsername(final String username) {
        this.username = username;
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

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setRoles(final List<String> roles) {
        this.roles = roles;
    }

    public void setPermissions(final List<String> permissions) {
        this.permissions = permissions;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartment(final String department) {
        this.department = department;
    }

    public void setCompanyId(final String companyId) {
        this.companyId = companyId;
    }

    public void setAvatar(final String avatar) {
        this.avatar = avatar;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof UserInfoResponse)) return false;
        final UserInfoResponse other = (UserInfoResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$username = this.getUsername();
        final java.lang.Object other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$roles = this.getRoles();
        final java.lang.Object other$roles = other.getRoles();
        if (this$roles == null ? other$roles != null : !this$roles.equals(other$roles)) return false;
        final java.lang.Object this$permissions = this.getPermissions();
        final java.lang.Object other$permissions = other.getPermissions();
        if (this$permissions == null ? other$permissions != null : !this$permissions.equals(other$permissions)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
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
        final java.lang.Object this$companyId = this.getCompanyId();
        final java.lang.Object other$companyId = other.getCompanyId();
        if (this$companyId == null ? other$companyId != null : !this$companyId.equals(other$companyId)) return false;
        final java.lang.Object this$avatar = this.getAvatar();
        final java.lang.Object other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof UserInfoResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $username = this.getUsername();
        result = result * PRIME + ($username == null ? 43 : $username.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $roles = this.getRoles();
        result = result * PRIME + ($roles == null ? 43 : $roles.hashCode());
        final java.lang.Object $permissions = this.getPermissions();
        result = result * PRIME + ($permissions == null ? 43 : $permissions.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $department = this.getDepartment();
        result = result * PRIME + ($department == null ? 43 : $department.hashCode());
        final java.lang.Object $companyId = this.getCompanyId();
        result = result * PRIME + ($companyId == null ? 43 : $companyId.hashCode());
        final java.lang.Object $avatar = this.getAvatar();
        result = result * PRIME + ($avatar == null ? 43 : $avatar.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "UserInfoResponse(id=" + this.getId() + ", username=" + this.getUsername() + ", name=" + this.getName() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", status=" + this.getStatus() + ", roles=" + this.getRoles() + ", permissions=" + this.getPermissions() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", departmentId=" + this.getDepartmentId() + ", department=" + this.getDepartment() + ", companyId=" + this.getCompanyId() + ", avatar=" + this.getAvatar() + ")";
    }
}
