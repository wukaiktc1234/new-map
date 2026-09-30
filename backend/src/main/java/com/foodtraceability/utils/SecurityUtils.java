package com.foodtraceability.utils;

import com.foodtraceability.common.util.LocationIdBridge;
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
     * 获取当前登录用户的**归属位置ID**（location_id）—— P1-USER-LOCATION-001 新主方法。
     *
     * <p>取值链：legacy {@code User} principal 的 locationId → null。
     * （{@code SecurityUser} / {@code KdsPrincipal} 分支由 S3 身份层落地时补齐。）
     *
     * <p>null 语义 = **显式拒绝**：调用方按业务决定 403/400；data_scope=all 的读侧走 all 分支。
     * 禁止数值兜底。
     *
     * @return location_id；未登录或未分配归属时返回 null
     */
    public static Long getCurrentUserLocationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUser) {
            return ((SecurityUser) principal).getLocationId();
        }
        if (principal instanceof User) {
            return ((User) principal).getLocationId();
        }
        // KDS 分支冻结（LIM-1：KdsPrincipal 位于未跟踪 WIP 文件，待该批次入库后补）
        return null;
    }

    /**
     * 获取当前登录用户的门店ID（stores_new.store_id 别名）—— **观察期兼容方法**。
     *
     * <p>P1-USER-LOCATION-001 §5.2：用户归属列已由 store_id 改名为 location_id，
     * 本方法保留原有 store_id 语义，内部经 {@link LocationIdBridge} **反查**（规则 3/4：
     * 跨 ID 空间换算必须经 location_id_map，严禁假设数值相等），供存量调用点平滑过渡；
     * 观察期后废弃，新代码一律用 {@link #getCurrentUserLocationId()}。
     *
     * @return store_id（字符串）；未登录、未分配归属或无法反查时返回 null
     */
    public static String getCurrentUserStoreId() {
        Long locationId = getCurrentUserLocationId();
        if (locationId == null) {
            return null;
        }
        Long storeId = LocationIdBridge.storeIdOf(locationId);
        return storeId != null ? String.valueOf(storeId) : null;
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
