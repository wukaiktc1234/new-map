package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.RoleMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.constants.AdminPermissions;
import com.foodtraceability.service.PermissionVerifyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 权限验证服务实现类
 * 提供权限验证、角色验证、数据权限范围查询等功能
 *
 * 重构说明：
 * - 已移除 Redis 缓存（StringRedisTemplate）和分布式锁（RedisDistributedLock）
 * - 改为内存 ConcurrentHashMap 缓存，带 TTL 机制
 * - 本地锁（ReentrantLock）替代分布式锁，单机部署足够使用
 * - 保留所有数据库查询逻辑
 */
@Service
public class PermissionVerifyServiceImpl implements PermissionVerifyService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionVerifyServiceImpl.class);

    // ==================== 缓存键前缀 ====================
    private static final String PERMISSION_CACHE_PREFIX = "user:permissions:";
    private static final String ROLE_CACHE_PREFIX = "user:roles:";
    private static final String DATA_SCOPE_CACHE_PREFIX = "user:data_scope:";

    // ==================== 缓存配置 ====================
    /** 缓存过期时间（毫秒），1 小时 */
    private static final long CACHE_EXPIRE_MS = 60 * 60 * 1000L;
    /** 缓存过期时间随机偏移量（毫秒），10 分钟，防雪崩 */
    private static final long EXPIRE_JITTER_MS = 10 * 60 * 1000L;

    /**
     * 缓存条目，存储值和过期时间
     */
    private static final class CacheEntry<T> {
        final T value;
        final long expireAt;

        CacheEntry(T value, long ttlMs) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /** 权限缓存：userId -> 权限集合 */
    private final ConcurrentHashMap<String, CacheEntry<Set<String>>> permissionCache = new ConcurrentHashMap<>();
    /** 角色缓存：userId -> 角色集合 */
    private final ConcurrentHashMap<String, CacheEntry<Set<String>>> roleCache = new ConcurrentHashMap<>();
    /** 数据权限范围缓存：userId -> 数据权限范围 */
    private final ConcurrentHashMap<String, CacheEntry<String>> dataScopeCache = new ConcurrentHashMap<>();

    /** 缓存加载锁，按 userId 分组，防止缓存击穿 */
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    /**
     * 管理员角色编码列表
     */
    private static final List<String> ADMIN_ROLE_CODES = Arrays.asList("admin", "super_admin", "Z");

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    public PermissionVerifyServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        if (userId == null || permission == null) {
            return false;
        }

        Set<String> permissions = getUserPermissions(userId);

        // 检查是否有超级权限
        if (permissions.contains("*")) {
            return true;
        }

        // 检查精确匹配
        if (permissions.contains(permission)) {
            return true;
        }

        // 检查通配符匹配（resource:*）
        String[] parts = permission.split(":");
        if (parts.length == 2) {
            String wildcardPermission = parts[0] + ":*";
            if (permissions.contains(wildcardPermission)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasPermission(String userId, String resource, String action) {
        if (userId == null || resource == null || action == null) {
            return false;
        }

        String permission = resource + ":" + action;
        return hasPermission(userId, permission);
    }

    @Override
    public boolean hasAnyPermission(String userId, List<String> permissions) {
        if (userId == null || permissions == null || permissions.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);

        // 检查是否有超级权限
        if (userPermissions.contains("*")) {
            return true;
        }

        return permissions.stream().anyMatch(perm -> {
            if (userPermissions.contains(perm)) {
                return true;
            }
            // 检查通配符匹配
            String[] parts = perm.split(":");
            if (parts.length == 2) {
                String wildcardPermission = parts[0] + ":*";
                return userPermissions.contains(wildcardPermission);
            }
            return false;
        });
    }

    @Override
    public boolean hasAllPermissions(String userId, List<String> permissions) {
        if (userId == null || permissions == null || permissions.isEmpty()) {
            return true;
        }

        Set<String> userPermissions = getUserPermissions(userId);

        // 检查是否有超级权限
        if (userPermissions.contains("*")) {
            return true;
        }

        return permissions.stream().allMatch(perm -> {
            if (userPermissions.contains(perm)) {
                return true;
            }
            // 检查通配符匹配
            String[] parts = perm.split(":");
            if (parts.length == 2) {
                String wildcardPermission = parts[0] + ":*";
                return userPermissions.contains(wildcardPermission);
            }
            return false;
        });
    }

    @Override
    public Set<String> getUserPermissions(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        try {
            // 【第一层：快速路径 - 从内存缓存读取】
            CacheEntry<Set<String>> cached = permissionCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                return cached.value;
            }
            if (cached != null) {
                permissionCache.remove(userId, cached);
            }

            // 【第二层：本地锁 + 双检锁模式（防止缓存击穿）】
            return loadPermissionsWithLock(userId);
        } catch (Exception e) {
            logger.warn("权限缓存读取异常，降级到数据库直查: userId={}, error={}", userId, e.getMessage());
            try {
                return loadPermissionsFromDatabase(userId);
            } catch (Exception dbEx) {
                logger.error("从数据库加载权限失败: userId={}", userId, dbEx);
                return Collections.emptySet();
            }
        }
    }

    /**
     * 使用本地锁加载权限数据（防止缓存击穿）
     *
     * @param userId 用户ID
     * @return 权限集合
     */
    private Set<String> loadPermissionsWithLock(String userId) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        lock.lock();
        try {
            // 【双重检查】：获取锁后再次检查缓存
            CacheEntry<Set<String>> cached = permissionCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                logger.debug("双重检查命中缓存: userId={}", userId);
                return cached.value;
            }
            if (cached != null) {
                permissionCache.remove(userId, cached);
            }

            // 负责加载数据库数据
            Set<String> permissions = loadPermissionsFromDatabase(userId);

            // 写入缓存（带随机 TTL 防雪崩）
            long ttl = CACHE_EXPIRE_MS + getRandomJitterMs();
            permissionCache.put(userId, new CacheEntry<>(permissions, ttl));

            logger.debug("权限缓存重建成功: userId={}, permissionCount={}", userId, permissions.size());
            return permissions;
        } catch (Exception e) {
            logger.error("加载用户权限异常: userId={}", userId, e);
            return Collections.emptySet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从数据库加载权限数据
     */
    private Set<String> loadPermissionsFromDatabase(String userId) throws Exception {
        Set<String> permissions = new HashSet<>();

        List<String> directPermissions = userMapper.getUserPermissions(Long.parseLong(userId));
        if (directPermissions != null) {
            permissions.addAll(directPermissions);
        }

        List<String> rolePermissions = userMapper.getUserRolePermissions(Long.parseLong(userId));
        if (rolePermissions != null) {
            permissions.addAll(rolePermissions);
        }

        // 管理员用户添加通配权限 "*"
        if (isAdminUser(userId, rolePermissions)) {
            permissions.add(AdminPermissions.WILDCARD);
            logger.debug("管理员用户添加通配权限 '*': userId={}", userId);
        }

        return permissions;
    }

    /**
     * 判断用户是否为管理员
     * 管理员判定规则：
     * 1. 用户名为 "admin"
     * 2. 或角色列表包含 "admin"/"super_admin"/"Z"/"ROLE_ADMIN"
     *
     * @param userId 用户ID
     * @param rolePermissions 用户角色权限列表（可选，避免重复查询）
     * @return 是否为管理员
     */
    private boolean isAdminUser(String userId, List<String> rolePermissions) {
        try {
            // 路径1：通过用户名判断（最可靠）
            Long uid = Long.parseLong(userId);
            User user = userMapper.selectById(uid);
            if (user != null && "admin".equals(user.getUsername())) {
                return true;
            }

            // 路径2：通过角色列表判断
            List<String> roles = rolePermissions;
            if (roles == null) {
                roles = userMapper.getUserRoles(uid);
            }
            if (roles != null) {
                for (String role : roles) {
                    if (role == null) continue;
                    String normalized = role.toLowerCase();
                    if ("admin".equalsIgnoreCase(role)
                        || "super_admin".equals(normalized)
                        || "z".equals(normalized)
                        || "role_z".equals(normalized)
                        || "role_admin".equals(normalized)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("判断管理员用户异常: userId={}, error={}", userId, e.getMessage());
        }
        return false;
    }

    @Override
    public Set<String> getUserRoles(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        try {
            // 【第一层：快速路径 - 从内存缓存读取】
            CacheEntry<Set<String>> cached = roleCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                return cached.value;
            }
            if (cached != null) {
                roleCache.remove(userId, cached);
            }

            // 【第二层：本地锁 + 双检锁模式】
            return loadRolesWithLock(userId);
        } catch (Exception e) {
            logger.warn("角色缓存读取异常，降级到数据库直查: userId={}, error={}", userId, e.getMessage());
            try {
                List<String> roles = userMapper.getUserRoles(Long.parseLong(userId));
                return roles != null ? new HashSet<>(roles) : new HashSet<>();
            } catch (Exception dbEx) {
                logger.error("从数据库加载角色失败: userId={}", userId, dbEx);
                return Collections.emptySet();
            }
        }
    }

    /**
     * 使用本地锁加载角色数据（防止缓存击穿）
     */
    private Set<String> loadRolesWithLock(String userId) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        lock.lock();
        try {
            // 【双重检查】
            CacheEntry<Set<String>> cached = roleCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                logger.debug("双重检查命中角色缓存: userId={}", userId);
                return cached.value;
            }
            if (cached != null) {
                roleCache.remove(userId, cached);
            }

            List<String> roles = userMapper.getUserRoles(Long.parseLong(userId));
            Set<String> roleSet = roles != null ? new HashSet<>(roles) : new HashSet<>();

            long ttl = CACHE_EXPIRE_MS + getRandomJitterMs();
            roleCache.put(userId, new CacheEntry<>(roleSet, ttl));

            logger.debug("角色缓存重建成功: userId={}, roleCount={}", userId, roleSet.size());
            return roleSet;
        } catch (Exception e) {
            logger.error("加载用户角色异常: userId={}", userId, e);
            return Collections.emptySet();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refreshUserPermissionCache(String userId) {
        if (userId == null) {
            return;
        }

        try {
            logger.info("开始刷新用户权限缓存: userId={}", userId);

            // 清除所有缓存数据
            permissionCache.remove(userId);
            roleCache.remove(userId);
            dataScopeCache.remove(userId);

            logger.info("刷新用户权限缓存成功: userId={}", userId);
        } catch (Exception e) {
            logger.error("刷新用户权限缓存失败: userId={}", userId, e);
        }
    }

    @Override
    public boolean hasRole(String userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return false;
        }

        Set<String> roles = getUserRoles(userId);
        return roles.contains(roleCode);
    }

    @Override
    public boolean hasAnyRole(String userId, List<String> roleCodes) {
        if (userId == null || roleCodes == null || roleCodes.isEmpty()) {
            return true;
        }

        Set<String> userRoles = getUserRoles(userId);
        return roleCodes.stream().anyMatch(userRoles::contains);
    }

    @Override
    public boolean hasAllRoles(String userId, List<String> roleCodes) {
        if (userId == null || roleCodes == null || roleCodes.isEmpty()) {
            return true;
        }

        Set<String> userRoles = getUserRoles(userId);
        return userRoles.containsAll(roleCodes);
    }

    @Override
    public boolean isAdmin(String userId) {
        if (userId == null) {
            return false;
        }

        // 路径1：通过用户名判断（最可靠，与 loadPermissionsFromDatabase 保持一致）
        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user != null && "admin".equals(user.getUsername())) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("通过用户名判断管理员失败: userId={}, error={}", userId, e.getMessage());
        }

        // 路径2：通过角色列表判断（大小写不敏感，匹配 SUPER_ADMIN/super_admin 等）
        Set<String> roles = getUserRoles(userId);
        return roles.stream().anyMatch(role -> {
            if (role == null) return false;
            String normalized = role.toLowerCase();
            return ADMIN_ROLE_CODES.stream().anyMatch(code -> code.equalsIgnoreCase(role))
                || "super_admin".equals(normalized)
                || "role_admin".equals(normalized)
                || "role_z".equals(normalized);
        });
    }

    @Override
    public String getUserDataScope(String userId) {
        if (userId == null) {
            return "self";
        }

        try {
            // 【第一层：快速路径 - 从内存缓存读取】
            CacheEntry<String> cached = dataScopeCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                return cached.value;
            }
            if (cached != null) {
                dataScopeCache.remove(userId, cached);
            }

            // 【第二层：本地锁 + 双检锁模式】
            return loadDataScopeWithLock(userId);
        } catch (Exception e) {
            logger.error("获取用户数据权限范围失败: userId={}", userId, e);
            return "self";
        }
    }

    /**
     * 使用本地锁加载数据权限范围（防止缓存击穿）
     */
    private String loadDataScopeWithLock(String userId) {
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());

        lock.lock();
        try {
            // 【双重检查】
            CacheEntry<String> cached = dataScopeCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                logger.debug("双重检查命中数据权限缓存: userId={}", userId);
                return cached.value;
            }
            if (cached != null) {
                dataScopeCache.remove(userId, cached);
            }

            String dataScope = loadDataScopeFromDatabase(userId);

            long ttl = CACHE_EXPIRE_MS + getRandomJitterMs();
            dataScopeCache.put(userId, new CacheEntry<>(dataScope, ttl));

            logger.debug("数据权限缓存重建成功: userId={}, dataScope={}", userId, dataScope);
            return dataScope;
        } catch (Exception e) {
            logger.error("加载用户数据权限异常: userId={}", userId, e);
            return "self";
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从数据库加载数据权限范围
     */
    private String loadDataScopeFromDatabase(String userId) throws Exception {
        // 获取用户的所有角色
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            return "self";
        }

        String rolesJson = user.getRoles();
        if (rolesJson == null || rolesJson.isEmpty()) {
            return "self";
        }

        List<String> roleIds = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});
        if (roleIds.isEmpty()) {
            return "self";
        }

        // 获取最高权限范围
        return getHighestDataScope(roleIds);
    }

    /**
     * 获取最高数据权限范围
     * 数据权限范围优先级：all > company > region > stores/departments > store > department > self
     */
    private String getHighestDataScope(List<String> roleIds) {
        // 数据权限范围优先级映射
        Map<String, Integer> scopePriority = new HashMap<>();
        scopePriority.put("all", 100);
        scopePriority.put("company", 80);
        scopePriority.put("region", 70);
        scopePriority.put("stores", 60);
        scopePriority.put("departments", 60);
        scopePriority.put("store", 40);
        scopePriority.put("department", 40);
        scopePriority.put("self", 20);

        String highestScope = "self";
        int highestPriority = 0;

        for (String roleId : roleIds) {
            Role role = roleMapper.selectById(Long.parseLong(roleId));
            if (role == null) {
                logger.warn("getHighestDataScope 角色不存在: roleId={}", roleId);
                continue;
            }
            if (role.getDataScope() == null) {
                logger.warn("getHighestDataScope 角色数据范围为空: roleId={}", roleId);
                continue;
            }
            String scope = role.getDataScope();
            int priority = scopePriority.getOrDefault(scope, 0);
            logger.debug("getHighestDataScope 角色权限: roleId={}, scope={}, priority={}", roleId, scope, priority);
            if (priority > highestPriority) {
                highestPriority = priority;
                highestScope = scope;
            }
        }

        return highestScope;
    }

    /**
     * 生成随机缓存过期时间偏移量（防缓存雪崩）
     *
     * @return 随机偏移时间（毫秒），范围：0 ~ EXPIRE_JITTER_MS
     */
    private long getRandomJitterMs() {
        return ThreadLocalRandom.current().nextLong(0, EXPIRE_JITTER_MS + 1);
    }
}
