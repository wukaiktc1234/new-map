package com.foodtraceability.security.model;

import com.foodtraceability.security.constants.AdminPermissions;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityUser implements UserDetails {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUser.class);

    private String userId;
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private Integer status;
    private boolean mfaEnabled;
    private boolean mfaVerified;
    private List<String> roles;
    private List<String> permissions;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        logger.debug("getAuthorities called - roles: {}, permissions: {}", roles, permissions);

        boolean isAdmin = false;

        if (roles != null) {
            for (String role : roles) {
                if (role == null || role.isEmpty()) continue;

                // 同时添加大写和原始大小写两种 ROLE authority，兼容项目中混用大小写的 @PreAuthorize 注解
                // hasAnyRole('admin') 检查 ROLE_admin；hasAnyRole('ADMIN') 检查 ROLE_ADMIN
                String normalizedRole = role.toUpperCase();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
                if (!role.equals(normalizedRole)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // 管理员角色自动获得所有权限（*）
                // 支持: admin/ADMIN, ROLE_ADMIN, Z(超级管理员), ROLE_Z
                boolean isAdminRole = "ROLE_ADMIN".equals(normalizedRole)
                    || "ADMIN".equals(normalizedRole)
                    || "Z".equals(normalizedRole)
                    || "ROLE_Z".equals(normalizedRole)
                    || "admin".equalsIgnoreCase(role);
                if (isAdminRole) {
                    isAdmin = true;
                    authorities.add(new SimpleGrantedAuthority("*"));
                    logger.debug("Added '*' authority for admin role: {}", role);
                }
            }
        }

        if (permissions != null) {
            // 当 permissions 包含 "*" 通配符时（admin 用户），标记为 admin 以便后续展开
            if (permissions.contains(AdminPermissions.WILDCARD)) {
                isAdmin = true;
            }
            // 添加 permissions 中的具体权限码（非通配符部分）
            for (String permission : permissions) {
                if (!AdminPermissions.WILDCARD.equals(permission)) {
                    authorities.add(new SimpleGrantedAuthority(permission));
                }
            }
        }

        // 管理员权限展开：检测到 admin 角色或 "*" 通配符时，
        // 展开为完整的权限码集合，供 @PreAuthorize("hasAuthority('xxx')") 使用。
        // 这样 JWT token 只需存储 "*"（小 token），运行时再展开为所有权限。
        // 修复：原代码仅在 permissions 包含 "*" 时展开，但 admin 角色检测添加的 "*"
        // 不会触发展开，导致 hasAuthority('xxx') 对 admin 失败。
        if (isAdmin) {
            logger.debug("Admin detected, expanding to all admin permissions");
            for (String permission : AdminPermissions.ALL) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        logger.debug("Final authorities count: {}", authorities.size());

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * 判断账户是否为启用状态
     * 兼容多种状态值：
     * - 数据库存储：1（启用）/ 0（禁用），SMALLINT/INTEGER 类型
     * - 规范 §24：后端和数据库使用数字编码 1/0/2（1启用 0禁用 2试用）
     * 修复Bug：原代码仅判断"active"，但数据库存储1，导致所有用户被判定为禁用
     */
    private boolean isAccountActive() {
        if (status == null) {
            return false;
        }
        // status 为 Integer 1 表示启用（正常）
        return status == 1;
    }

    @Override
    public boolean isAccountNonExpired() {
        return isAccountActive();
    }

    @Override
    public boolean isAccountNonLocked() {
        return isAccountActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return isAccountActive();
    }

    @Override
    public boolean isEnabled() {
        return isAccountActive();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public boolean isMfaVerified() {
        return mfaVerified;
    }

    public void setMfaVerified(boolean mfaVerified) {
        this.mfaVerified = mfaVerified;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
