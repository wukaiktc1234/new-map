package com.foodtraceability.utils;

import com.foodtraceability.entity.User;
import com.foodtraceability.security.model.SecurityUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 用于获取当前登录用户信息
 *
 * <p>支持两种 Principal 类型：</p>
 * <ul>
 *   <li>JWT 认证：Principal 为 {@link SecurityUser}，userId 为 String 类型（需 Long.parseLong 转换）</li>
 *   <li>兼容场景：Principal 为 {@link User}（旧版登录链路保留）</li>
 * </ul>
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户（兼容旧版 User Principal）
     * @return 当前登录用户，如果未登录或为 SecurityUser 则返回null
     * @deprecated 推荐使用 {@link #getCurrentUserId()} 直接获取用户ID，避免 Principal 类型耦合
     */
    @Deprecated
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     * <p>兼容 SecurityUser 和 User 两种 Principal 类型。</p>
     * @return 当前登录用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // 优先处理 SecurityUser（JWT 认证链路）
        if (principal instanceof SecurityUser) {
            String userIdStr = ((SecurityUser) principal).getUserId();
            if (userIdStr == null || userIdStr.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 兼容 User（旧版登录链路）
        if (principal instanceof User) {
            User user = (User) principal;
            return user.getId();
        }
        return null;
    }

    /**
     * 获取当前登录用户名
     * @return 当前登录用户名，如果未登录则返回null
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser) {
            return ((SecurityUser) principal).getUsername();
        }
        if (principal instanceof User) {
            return ((User) principal).getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录用户的门店ID
     * @return 当前登录用户的门店ID，如果未登录或用户未分配门店则返回null
     */
    public static String getCurrentUserStoreId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // SecurityUser 没有 storeId 字段，返回 null（如需门店隔离可后续扩展）
        if (principal instanceof User) {
            Long storeId = ((User) principal).getStoreId();
            return storeId != null ? String.valueOf(storeId) : null;
        }
        return null;
    }

    /**
     * 判断当前用户是否为管理员
     * @return true表示是管理员，false表示不是管理员或未登录
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        // SecurityUser 通过 authorities 判断
        if (principal instanceof SecurityUser) {
            SecurityUser securityUser = (SecurityUser) principal;
            if (securityUser.getRoles() != null) {
                for (String role : securityUser.getRoles()) {
                    if ("admin".equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
            return false;
        }
        // User 通过 roles JSON 判断
        if (principal instanceof User) {
            User user = (User) principal;
            String rolesJson = user.getRoles();
            if (rolesJson != null && !rolesJson.isEmpty()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    java.util.List<String> roleIds = objectMapper.readValue(rolesJson, new TypeReference<java.util.List<String>>() {});
                    for (String roleId : roleIds) {
                        if ("admin".equals(roleId)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 判断当前用户是否有门店权限
     * @return true表示用户有门店权限，false表示用户没有门店权限或未登录
     */
    public static boolean hasStorePermission() {
        String storeId = getCurrentUserStoreId();
        return storeId != null && !storeId.isEmpty();
    }

    /**
     * 获取当前租户ID
     * 目前返回0表示默认租户
     * @return 租户ID
     */
    public static Long getCurrentTenantId() {
        // TODO: 实现多租户逻辑
        return 0L;
    }
}
