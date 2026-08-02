package com.foodtraceability.security.service.impl;

import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.security.model.UserPermissionInfo;
import com.foodtraceability.security.service.UserPermissionCacheService;
import com.foodtraceability.service.RoleService;
import com.foodtraceability.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 用户权限缓存服务实现
 * 基于内存 ConcurrentHashMap 实现短期缓存，TTL 1小时
 * 数据从 PostgreSQL 数据库查询，缓存命中时直接返回
 */
@Service
public class UserPermissionCacheServiceImpl implements UserPermissionCacheService {

    private static final Logger logger = LoggerFactory.getLogger(UserPermissionCacheServiceImpl.class);

    /** 缓存过期时间（小时） */
    private static final long CACHE_EXPIRE_HOURS = 1;

    /** 清理阈值：缓存条目超过此数量时触发清理 */
    private static final int CLEANUP_THRESHOLD = 5000;

    private final UserService userService;

    private final RoleService roleService;

    private final ObjectMapper objectMapper;

    /** 内存缓存存储：userId -> TimedEntry<UserPermissionInfo> */
    private final ConcurrentMap<String, TimedEntry<UserPermissionInfo>> permissionCache = new ConcurrentHashMap<>();

    public UserPermissionCacheServiceImpl(UserService userService, RoleService roleService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.roleService = roleService;
        this.objectMapper = objectMapper;
    }

    /**
     * 带过期时间的缓存条目
     */
    private static class TimedEntry<T> {
        private final T value;
        private final Instant expireAt;

        TimedEntry(T value, long expireInHours) {
            this.value = value;
            this.expireAt = Instant.now().plusSeconds(expireInHours * 3600);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }

        T getValue() {
            return value;
        }
    }

    @Override
    public Optional<UserPermissionInfo> getUserPermissionInfo(String userId) {
        try {
            // 触发过期清理
            cleanupExpiredEntriesIfNeeded();

            TimedEntry<UserPermissionInfo> cached = permissionCache.get(userId);
            if (cached != null && !cached.isExpired()) {
                logger.info("用户权限信息命中缓存: userId={}", userId);
                return Optional.of(cached.getValue());
            }

            // 缓存未命中或已过期，从数据库构建
            logger.info("用户权限信息未命中缓存，从数据库构建: userId={}", userId);
            UserPermissionInfo info = buildUserPermissionInfo(userId);
            cacheUserPermissionInfo(userId, info);
            return Optional.of(info);
        } catch (Exception e) {
            logger.error("获取用户权限信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return Optional.of(buildUserPermissionInfo(userId));
        }
    }

    @Override
    public void cacheUserPermissionInfo(String userId, UserPermissionInfo permissionInfo) {
        try {
            permissionInfo.setCacheTime(System.currentTimeMillis());
            permissionCache.put(userId, new TimedEntry<>(permissionInfo, CACHE_EXPIRE_HOURS));
            logger.info("用户权限信息已缓存: userId={}, expireHours={}", userId, CACHE_EXPIRE_HOURS);
        } catch (Exception e) {
            logger.error("缓存用户权限信息失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void evictUserPermissionCache(String userId) {
        try {
            permissionCache.remove(userId);
            logger.info("用户权限缓存已清除: userId={}", userId);
        } catch (Exception e) {
            logger.error("清除用户权限缓存失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void evictAllUserPermissionCache() {
        try {
            permissionCache.clear();
            logger.info("所有用户权限缓存已清除");
        } catch (Exception e) {
            logger.error("清除所有用户权限缓存失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 从数据库构建用户权限信息
     * @param userId 用户ID
     * @return 用户权限信息对象
     */
    private UserPermissionInfo buildUserPermissionInfo(String userId) {
        try {
            User user = userService.getById(Long.parseLong(userId));
            if (user == null) {
                logger.warn("用户不存在: userId={}", userId);
                return UserPermissionInfo.builder()
                    .userId(userId)
                    .roles(new ArrayList<>())
                    .permissions(new ArrayList<>())
                    .isAdmin(false)
                    .build();
            }

            List<String> roleIds = new ArrayList<>();
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                try {
                    roleIds = objectMapper.readValue(user.getRoles(), new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    logger.error("解析用户角色失败: userId={}, error={}", userId, e.getMessage());
                }
            }

            List<String> roleNames = new ArrayList<>();
            boolean isAdmin = false;

            for (String roleId : roleIds) {
                Role role = roleService.getRoleById(Long.valueOf(roleId));
                if (role != null) {
                    roleNames.add(role.getRoleName());
                    if ("admin".equalsIgnoreCase(role.getRoleCode())) {
                        isAdmin = true;
                    }
                }
            }

            return UserPermissionInfo.builder()
                .userId(userId)
                .username(user.getUsername())
                .roles(roleNames)
                .permissions(new ArrayList<>())
                .storeId(user.getStoreId() != null ? String.valueOf(user.getStoreId()) : null)
                .departmentId(user.getDepartmentId() != null ? String.valueOf(user.getDepartmentId()) : null)
                .isAdmin(isAdmin)
                .cacheTime(System.currentTimeMillis())
                .build();
        } catch (Exception e) {
            logger.error("构建用户权限信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return UserPermissionInfo.builder()
                .userId(userId)
                .roles(new ArrayList<>())
                .permissions(new ArrayList<>())
                .isAdmin(false)
                .build();
        }
    }

    /**
     * 清理过期缓存条目，防止内存泄漏
     * 当缓存条目超过阈值时触发清理
     */
    private void cleanupExpiredEntriesIfNeeded() {
        if (permissionCache.size() < CLEANUP_THRESHOLD) {
            return;
        }
        permissionCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        logger.debug("权限缓存清理完成: currentSize={}", permissionCache.size());
    }
}
