package com.foodtraceability.security.model;

import com.foodtraceability.security.constants.AdminPermissions;
import com.foodtraceability.security.service.AdminPermissionCatalog;
import com.foodtraceability.security.utils.RoleCodeNormalizer;
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

    /**
     * P1-USER-LOCATION-001：用户归属位置ID（JWT claim {@code locationId}）。
     * 单字段走天下——门店员工指向 STORE 型 location，仓库员工指向 CENTRAL/DEPOT 型
     * （design-002 §5.2：不再区分 storeId / warehouseId）。
     * null = 未分配归属 → 调用方按**显式拒绝**处理（禁止数值兜底）。
     */
    private Long locationId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        logger.debug("getAuthorities called - roles: {}, permissions: {}", roles, permissions);

        boolean isAdmin = false;

        if (roles != null) {
            for (String role : roles) {
                if (role == null || role.isEmpty()) continue;

                // P1-AUTH-CHAIN-001-A（N30 止血）：统一走 RoleCodeNormalizer，
                // 消除历史实现的 ROLE_ + （已带 ROLE_ 的值）→ ROLE_ROLE_STORE_MANAGER 双前缀缺陷。
                // 归一后 authority 恒为 ROLE_ + 全大写规范码，与 Spring hasRole('store_manager') 期望的
                // ROLE_STORE_MANAGER 对齐；同时保留小写变体，兼容直接比较 authority 字符串的旧代码。
                String canonicalRole = RoleCodeNormalizer.normalize(role);
                if (canonicalRole == null) {
                    continue;
                }

                String upperAuthority = RoleCodeNormalizer.toAuthority(canonicalRole);
                authorities.add(new SimpleGrantedAuthority(upperAuthority));
                String lowerAuthority = RoleCodeNormalizer.ROLE_PREFIX + canonicalRole;
                if (!lowerAuthority.equals(upperAuthority)) {
                    authorities.add(new SimpleGrantedAuthority(lowerAuthority));
                }

                // 管理员角色自动获得所有权限（*）
                // 归一后 'ROLE_ADMIN' / 'ADMIN' / 'admin' / 'ROLE_Z' / 'Z' 分别归一为 'admin' / 'z'
                boolean isAdminRole = "admin".equals(canonicalRole) || "z".equals(canonicalRole);
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
        //
        // N31（Owner 裁决 1）：admin 全局兜底 —— 除硬编码 AdminPermissions.ALL 外，
        // 再并入 DB 目录全量权限码（AdminPermissionCatalog）。原因：注解里存在既不在
        // AdminPermissions.ALL、也无 or hasAuthority('*') 兜底的权限码（实测 34 个码 / 67 处），
        // 只靠硬编码清单会导致「连 admin 都进不去」。此改动**不需要改任何注解**。
        if (isAdmin) {
            logger.debug("Admin detected, expanding to all admin permissions");
            for (String permission : AdminPermissions.ALL) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
            for (String permission : AdminPermissionCatalog.codes()) {
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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
