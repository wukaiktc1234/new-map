package com.foodtraceability.security.aspect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.security.annotation.DataScope;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.RoleService;
import com.foodtraceability.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据范围权限切面
 * 自动为查询添加数据范围过滤条件
 */
@Aspect
@Component
public class DataScopeAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataScopeAspect.class);

    private final UserService userService;
    private final RoleService roleService;

    public DataScopeAspect(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) throws Throwable {
        handleDataScope(point, dataScope);
    }

    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        DataScopeContext.clear();
    }

    private void handleDataScope(JoinPoint point, DataScope dataScope) {
        SecurityUser securityUser = getCurrentUser();
        if (securityUser == null) {
            return;
        }

        if (isAdmin(securityUser)) {
            return;
        }

        StringBuilder sqlString = new StringBuilder();
        DataScope.DataScopeType scopeType = dataScope.type();

        if (scopeType == DataScope.DataScopeType.AUTO) {
            scopeType = determineDataScopeType(securityUser);
        }

        String alias = dataScope.tableAlias();
        if (!alias.isEmpty()) {
            alias = alias + ".";
        }

        switch (scopeType) {
            case ALL:
                return;
            case COMPANY:
                sqlString.append(alias).append(escapeColumn(dataScope.companyIdColumn())).append(" = '").append(escapeValue(securityUser.getUserId())).append("'");
                break;
            case STORE:
                sqlString.append(alias).append(escapeColumn(dataScope.storeIdColumn())).append(" = '").append(escapeValue(getUserStoreId(securityUser))).append("'");
                break;
            case DEPARTMENT:
                sqlString.append(alias).append(escapeColumn(dataScope.deptIdColumn())).append(" = '").append(escapeValue(getUserDeptId(securityUser))).append("'");
                break;
            case SELF:
                sqlString.append(alias).append(escapeColumn(dataScope.userIdColumn())).append(" = '").append(escapeValue(securityUser.getUserId())).append("'");
                break;
            case SELF_AND_SUBORDINATE:
                sqlString.append(alias).append(escapeColumn(dataScope.userIdColumn())).append(" = '").append(escapeValue(securityUser.getUserId())).append("'");
                break;
            case NONE:
                sqlString.append("1 = 0");
                break;
            default:
                sqlString.append(alias).append(escapeColumn(dataScope.userIdColumn())).append(" = '").append(escapeValue(securityUser.getUserId())).append("'");
        }

        if (sqlString.length() > 0) {
            setDataScopeSql(sqlString.toString());
            logger.debug("数据范围SQL: {}", sqlString);
        }
    }

    private SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        }
        return null;
    }

    private boolean isAdmin(SecurityUser user) {
        List<String> roles = user.getRoles();
        if (roles != null) {
            for (String role : roles) {
                if ("admin".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }
        List<String> permissions = user.getPermissions();
        if (permissions != null && permissions.contains("*")) {
            return true;
        }
        return false;
    }

    private DataScope.DataScopeType determineDataScopeType(SecurityUser user) {
        try {
            User dbUser = userService.getById(Long.parseLong(user.getUserId()));
            if (dbUser == null || dbUser.getRoles() == null) {
                return DataScope.DataScopeType.SELF;
            }

            String rolesJson = dbUser.getRoles();
            if (rolesJson.startsWith("[")) {
                String[] roleIds = rolesJson.replace("[", "").replace("]", "").replace("\"", "").split(",");
                DataScope.DataScopeType mostPermissive = DataScope.DataScopeType.NONE;

                for (String roleId : roleIds) {
                    roleId = roleId.trim();
                    if (roleId.isEmpty()) continue;

                    Role role = roleService.getRoleById(Long.parseLong(roleId));
                    if (role != null && role.getDataScope() != null) {
                        DataScope.DataScopeType type = parseDataScope(role.getDataScope());
                        if (isMorePermissive(type, mostPermissive)) {
                            mostPermissive = type;
                        }
                    }
                }
                return mostPermissive;
            }
        } catch (Exception e) {
            logger.error("确定数据范围类型失败: {}", e.getMessage());
        }
        return DataScope.DataScopeType.SELF;
    }

    private DataScope.DataScopeType parseDataScope(String scope) {
        if (scope == null) return DataScope.DataScopeType.SELF;
        switch (scope.toLowerCase()) {
            case "all": return DataScope.DataScopeType.ALL;
            case "company": return DataScope.DataScopeType.COMPANY;
            case "store": return DataScope.DataScopeType.STORE;
            case "department": return DataScope.DataScopeType.DEPARTMENT;
            case "self": return DataScope.DataScopeType.SELF;
            case "self_and_subordinate": return DataScope.DataScopeType.SELF_AND_SUBORDINATE;
            case "custom": return DataScope.DataScopeType.CUSTOM;
            case "none": return DataScope.DataScopeType.NONE;
            default: return DataScope.DataScopeType.SELF;
        }
    }

    private boolean isMorePermissive(DataScope.DataScopeType a, DataScope.DataScopeType b) {
        int rankA = getRank(a);
        int rankB = getRank(b);
        return rankA > rankB;
    }

    private int getRank(DataScope.DataScopeType type) {
        switch (type) {
            case ALL: return 8;
            case COMPANY: return 7;
            case STORE: return 6;
            case DEPARTMENT: return 5;
            case SELF_AND_SUBORDINATE: return 4;
            case CUSTOM: return 3;
            case SELF: return 2;
            case NONE: return 1;
            default: return 0;
        }
    }

    private String getUserStoreId(SecurityUser user) {
        try {
            User dbUser = userService.getById(Long.parseLong(user.getUserId()));
            return dbUser != null && dbUser.getStoreId() != null ? String.valueOf(dbUser.getStoreId()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserDeptId(SecurityUser user) {
        try {
            User dbUser = userService.getById(Long.parseLong(user.getUserId()));
            return dbUser != null && dbUser.getDepartmentId() != null ? String.valueOf(dbUser.getDepartmentId()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void setDataScopeSql(String sql) {
        DataScopeContext.setSql(sql);
    }

    /**
     * 转义列名，防止SQL注入
     */
    private String escapeColumn(String column) {
        if (column == null || column.isEmpty()) {
            return "";
        }
        // 只允许字母、数字、下划线
        if (!column.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("非法的列名: " + column);
        }
        return column;
    }

    /**
     * 转义值，防止SQL注入
     */
    private String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * 数据范围上下文（线程本地存储）
     */
    public static class DataScopeContext {
        private static final ThreadLocal<String> SQL_HOLDER = new ThreadLocal<>();

        public static void setSql(String sql) {
            SQL_HOLDER.set(sql);
        }

        public static String getSql() {
            return SQL_HOLDER.get();
        }

        public static void clear() {
            SQL_HOLDER.remove();
        }
    }
}
