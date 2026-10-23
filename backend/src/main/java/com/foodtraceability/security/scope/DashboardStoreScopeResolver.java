package com.foodtraceability.security.scope;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.config.DashboardStoreScopeProperties;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.UserService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 看板门店作用域解析器 —— P1-DASHBOARD-FILTER-001（R8，越权风险）。
 *
 * <p>职责：把「当前登录用户」解析成一个门店ID，供 {@code DashboardServiceImpl}
 * 在开关开启时追加 {@code store_id = ?} / {@code location_id = ?} 条件。</p>
 *
 * <p><b>解析链（P1-USER-LOCATION-001/002 后的真实列）</b>：
 * ① {@code SecurityUser.locationId}（JWT claim）→ ② {@code users.location_id} →
 * ③ {@code employees.location_id}（按 {@code employees.user_id} 关联）→
 * ④ 经 {@link LocationIdBridge} 反查 {@code stores_new.store_id}（严禁数值兜底）。</p>
 *
 * <p><b>空值语义</b>：返回 {@link Optional#empty()} 表示「不限门店」，与历史全量口径一致：
 * 开关关闭 / admin 用户 / 解析不到归属 / 无 stores_new 映射。</p>
 */
@Component
public class DashboardStoreScopeResolver {

    private static final Logger logger = LoggerFactory.getLogger(DashboardStoreScopeResolver.class);

    /** 追加到 orders 类表的门店列。 */
    private static final String COL_STORE_ID = "store_id";

    /** 追加到 inventory 等按位置建模的表列。 */
    private static final String COL_LOCATION_ID = "location_id";

    private final DashboardStoreScopeProperties properties;
    private final UserService userService;
    private final EmployeeMapper employeeMapper;
    private final LocationIdBridge locationIdBridge;

    public DashboardStoreScopeResolver(DashboardStoreScopeProperties properties,
                                       UserService userService,
                                       EmployeeMapper employeeMapper,
                                       LocationIdBridge locationIdBridge) {
        this.properties = properties;
        this.userService = userService;
        this.employeeMapper = employeeMapper;
        this.locationIdBridge = locationIdBridge;
    }

    /** 开关状态（供 Service 决定是否走作用域分支）。 */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * 当前登录用户的门店ID。
     *
     * @return 门店ID；empty = 不限门店（开关关闭 / admin / 无绑定 / 无映射）
     */
    public Optional<Long> currentStoreId() {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        if (currentIsAdmin()) {
            logger.debug("看板门店作用域：当前用户为 admin → 全量口径");
            return Optional.empty();
        }
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            logger.debug("看板门店作用域：无登录用户上下文 → 不限门店");
            return Optional.empty();
        }
        Long locationId = resolveLocationId(userId);
        if (locationId == null) {
            logger.warn("看板门店作用域已开启，但 userId={} 未绑定 users.location_id / employees.location_id "
                    + "→ 按「不限门店」处理（P1-DASHBOARD-FILTER-001：缺绑定仍可能越权）", userId);
            return Optional.empty();
        }
        Long storeId = toStoreId(locationId);
        if (storeId == null) {
            logger.warn("看板门店作用域已开启，但 locationId={} 无 stores_new 门店映射（userId={}）"
                    + "→ 门店维度不追加过滤", locationId, userId);
            return Optional.empty();
        }
        logger.debug("看板门店作用域：userId={} locationId={} → storeId={}", userId, locationId, storeId);
        return Optional.of(storeId);
    }

    /**
     * 当前登录用户归属的位置ID（inventory 等表按 {@code location_id} 建模）。
     *
     * @return location_id；empty = 不限位置
     */
    public Optional<Long> currentLocationId() {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        if (currentIsAdmin()) {
            return Optional.empty();
        }
        Long userId = resolveCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        Long locationId = resolveLocationId(userId);
        if (locationId == null) {
            return Optional.empty();
        }
        return Optional.of(locationId);
    }

    /**
     * 只读注入：解析到门店时给 wrapper 追加 {@code store_id = ?}；
     * 解析不到时 <b>保持 wrapper 原样</b>（默认行为零变化）。
     */
    public <T> QueryWrapper<T> applyStoreId(QueryWrapper<T> wrapper) {
        currentStoreId().ifPresent(id -> wrapper.eq(COL_STORE_ID, id));
        return wrapper;
    }

    /**
     * 只读注入：解析到归属位置时给 wrapper 追加 {@code location_id = ?}；
     * 解析不到时保持原样。
     */
    public <T> QueryWrapper<T> applyLocationId(QueryWrapper<T> wrapper) {
        currentLocationId().ifPresent(id -> wrapper.eq(COL_LOCATION_ID, id));
        return wrapper;
    }

    // ------------------------------------------------------------------ 内部

    private SecurityUser currentSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        }
        return null;
    }

    private Long resolveCurrentUserId() {
        SecurityUser securityUser = currentSecurityUser();
        if (securityUser != null) {
            String userId = securityUser.getUserId();
            if (userId == null || userId.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                logger.warn("看板门店作用域：SecurityUser.userId 非数字（{}）→ 不限门店", userId);
                return null;
            }
        }
        // 旧版 User principal 兼容（与 SecurityUtils 口径一致）
        return SecurityUtils.getCurrentUserId();
    }

    private boolean currentIsAdmin() {
        SecurityUser securityUser = currentSecurityUser();
        if (securityUser == null) {
            // 旧版 User principal：沿用 SecurityUtils.isAdmin()（admin 角色 → 全量）
            return SecurityUtils.isAdmin();
        }
        List<String> roles = securityUser.getRoles();
        if (roles != null) {
            for (String role : roles) {
                if (role == null) {
                    continue;
                }
                if ("admin".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        }
        List<String> permissions = securityUser.getPermissions();
        return permissions != null && permissions.contains("*");
    }

    /**
     * 归属位置解析：JWT claim → users.location_id → employees.location_id（employees.user_id 关联）。
     */
    private Long resolveLocationId(Long userId) {
        SecurityUser securityUser = currentSecurityUser();
        if (securityUser != null && securityUser.getLocationId() != null) {
            return securityUser.getLocationId();
        }
        Long legacyLocationId = SecurityUtils.getCurrentUserLocationId();
        if (legacyLocationId != null) {
            return legacyLocationId;
        }
        try {
            User user = userService.getById(userId);
            if (user != null && user.getLocationId() != null) {
                return user.getLocationId();
            }
        } catch (Exception e) {
            logger.warn("看板门店作用域：读取 users.location_id 失败（userId={}）：{}", userId, e.getMessage());
        }
        try {
            Employee employee = employeeMapper.selectOne(new QueryWrapper<Employee>()
                    .eq("user_id", userId)
                    .last("LIMIT 1"));
            if (employee != null && employee.getLocationId() != null) {
                return employee.getLocationId();
            }
        } catch (Exception e) {
            logger.warn("看板门店作用域：读取 employees.location_id 失败（userId={}）：{}", userId, e.getMessage());
        }
        return null;
    }

    private Long toStoreId(Long locationId) {
        try {
            return locationIdBridge.toStoreId(locationId);
        } catch (Exception e) {
            logger.warn("看板门店作用域：location_id={} 反查 stores_new.store_id 失败：{}", locationId, e.getMessage());
            return null;
        }
    }
}
