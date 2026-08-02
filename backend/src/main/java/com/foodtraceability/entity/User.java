package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.foodtraceability.annotation.Desensitize;
import com.foodtraceability.annotation.DesensitizeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 用于管理系统用户信息，包括权限和角色管理
 */
@TableName("users")
@Schema(description = "用户实体")
public class User {

    /**
     * 用户ID
     * 注：PostgreSQL 真实表 users 主键为 user_id（V1.0.0.100__init_postgresql.sql）。
     * H2 schema.sql 与 BaseDatabaseInitializer.createUsersTable() 已同步使用 user_id。
     * 使用 ASSIGN_ID 雪花算法：避免与 BaseDatabaseInitializer 手动插入的 admin(user_id=1) 冲突。
     */
    @TableId(type = IdType.ASSIGN_ID, value = "user_id")
    @Schema(description = "用户ID", example = "1234567890")
    private Long id;

    /**
     * 用户名（登录名）
     */
    @Schema(description = "用户名（登录名）", example = "admin")
    private String username;

    /**
     * 密码（已加密）
     */
    @Schema(description = "密码（已加密）", example = "$2a$10$...")
    private String password;

    /**
     * 用户姓名
     */
    @Schema(description = "用户姓名", example = "张三")
    @Desensitize(value = DesensitizeType.NAME)
    @TableField(value = "name")
    private String fullName;

    /**
     * 用户邮箱
     */
    @Schema(description = "用户邮箱", example = "zhangsan@example.com")
    @Desensitize(value = DesensitizeType.EMAIL)
    private String email;

    /**
     * 用户手机号
     */
    @Schema(description = "用户手机号", example = "13800138000")
    @Desensitize(value = DesensitizeType.PHONE)
    private String phone;

    /**
     * 用户状态（1: 正常, 0: 禁用）
     * 后端和数据库统一使用 Integer，前端通过 DataConverter 转换
     */
    @Schema(description = "用户状态（1: 正常, 0: 禁用）", example = "1")
    private Integer status;

    /**
     * 用户角色ID列表（JSON格式存储）
     */
    @Schema(description = "用户角色ID列表（JSON格式存储）", example = "[\"1234567890\", \"0987654321\"]")
    @TableField(value = "roles")
    private String roles;

    /**
     * 用户角色名称列表（仅用于前端显示，不存储到数据库）
     */
    @Schema(description = "用户角色名称列表（仅用于前端显示）", example = "[\"系统管理员\", \"操作员\"]")
    @TableField(exist = false)
    private String roleNames;

    /**
     * 所属部门名称
     */
    @Schema(description = "所属部门名称", example = "生产部")
    @TableField(exist = false)
    private String department;

    /**
     * 所属部门ID
     */
    @Schema(description = "所属部门ID", example = "1234567890")
    @TableField("department_id")
    private Long departmentId;

    /**
     * 所属门店ID
     */
    @Schema(description = "所属门店ID", example = "1234567890")
    @TableField("store_id")
    private Long storeId;

    /**
     * 所属门店名称
     */
    @Schema(description = "所属门店名称", example = "中心店")
    @TableField(exist = false)
    private String storeName;

    /**
     * 员工编号
     */
    @Schema(description = "员工编号", example = "EMP2024001")
    @TableField(value = "employee_code")
    private String employeeCode;

    /**
     * 用户权限
     */
    @Schema(description = "用户权限", example = "admin")
    @TableField(exist = false)
    private String permissions;

    /**
     * 是否被锁定
     */
    @Schema(description = "是否被锁定", example = "false")
    @TableField(value = "is_locked")
    private Boolean isLocked;

    /**
     * 锁定时间
     */
    @Schema(description = "锁定时间", example = "2023-01-01 12:00:00")
    @TableField(value = "lock_time")
    private LocalDateTime lockTime;

    /**
     * 密码错误次数
     */
    @Schema(description = "密码错误次数", example = "0")
    @TableField(value = "password_error_count")
    private Integer passwordErrorCount;

    /**
     * 最后密码错误时间
     */
    @Schema(description = "最后密码错误时间", example = "2023-01-01 12:00:00")
    @TableField(value = "last_password_error_time")
    private LocalDateTime lastPasswordErrorTime;

    /**
     * 是否需要修改密码
     */
    @Schema(description = "是否需要修改密码", example = "false")
    @TableField(value = "need_change_password")
    private Boolean needChangePassword;

    /**
     * 最后密码修改时间
     */
    @Schema(description = "最后密码修改时间", example = "2023-01-01 12:00:00")
    @TableField(value = "last_password_change_time")
    private LocalDateTime lastPasswordChangeTime;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间", example = "2023-01-01 12:00:00")
    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Schema(description = "最后登录IP", example = "127.0.0.1")
    @TableField(value = "last_login_ip")
    private String lastLoginIp;

    /**
     * 公司ID
     */
    @Schema(description = "公司ID", example = "1234567890")
    @TableField(exist = false)
    private String companyId;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * 是否删除
     */
    @Schema(description = "是否删除", example = "false")
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2023-01-01 12:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2023-01-01 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "admin")
    @TableField(value = "created_by")
    private String createdBy;

    /**
     * 更新人
     */
    @Schema(description = "更新人", example = "admin")
    @TableField(value = "updated_by")
    private String updatedBy;

    /**
     * 版本号
     */
    @Schema(description = "版本号", example = "1")
    @Version
    @TableField(exist = false)
    private Integer version;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(String roleNames) {
        this.roleNames = roleNames;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean locked) {
        isLocked = locked;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public Integer getPasswordErrorCount() {
        return passwordErrorCount;
    }

    public void setPasswordErrorCount(Integer passwordErrorCount) {
        this.passwordErrorCount = passwordErrorCount;
    }

    public LocalDateTime getLastPasswordErrorTime() {
        return lastPasswordErrorTime;
    }

    public void setLastPasswordErrorTime(LocalDateTime lastPasswordErrorTime) {
        this.lastPasswordErrorTime = lastPasswordErrorTime;
    }

    public Boolean getNeedChangePassword() {
        return needChangePassword;
    }

    public void setNeedChangePassword(Boolean needChangePassword) {
        this.needChangePassword = needChangePassword;
    }

    public LocalDateTime getLastPasswordChangeTime() {
        return lastPasswordChangeTime;
    }

    public void setLastPasswordChangeTime(LocalDateTime lastPasswordChangeTime) {
        this.lastPasswordChangeTime = lastPasswordChangeTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
