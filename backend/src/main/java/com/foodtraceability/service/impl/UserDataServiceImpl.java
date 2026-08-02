package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.StoreBasicInfo;
import com.foodtraceability.dto.UserBasicInfo;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.StoreDataService;
import com.foodtraceability.service.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class UserDataServiceImpl implements UserDataService {

    private static final Logger log = LoggerFactory.getLogger(UserDataServiceImpl.class);

    private final UserMapper userMapper;

    private final StoreDataService storeDataService;

    public UserDataServiceImpl(UserMapper userMapper, StoreDataService storeDataService) {
        this.userMapper = userMapper;
        this.storeDataService = storeDataService;
    }

    @Override
    public Map<String, UserBasicInfo> batchGetUserBasicInfo(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        log.info("批量获取用户基本信息, userIds: {}", userIds);

        // 直接批量查询数据库
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .in(User::getId, userIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList()))
        );

        Map<Long, User> userMap = users.stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        // 批量查询门店信息
        List<String> storeIds = users.stream()
            .filter(u -> u.getStoreId() != null)
            .map(u -> String.valueOf(u.getStoreId()))
            .distinct()
            .collect(Collectors.toList());

        Map<String, StoreBasicInfo> storeInfoMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            try {
                storeInfoMap = storeDataService.batchGetStoreBasicInfo(storeIds);
            } catch (Exception e) {
                log.warn("批量获取门店信息失败", e);
            }
        }

        Map<String, UserBasicInfo> resultMap = new HashMap<>();
        for (String userId : userIds) {
            User user = userMap.get(Long.parseLong(userId));
            resultMap.put(userId, user != null ? convertToBasicInfo(user, storeInfoMap) : null);
        }

        return resultMap;
    }

    @Override
    @Cacheable(value = "userBasicInfo", key = "#userId", unless = "#result == null")
    public UserBasicInfo getUserBasicInfo(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        log.info("获取用户基本信息, userId: {}", userId);

        User user = userMapper.selectById(Long.parseLong(userId));

        if (user == null) {
            log.warn("用户不存在: userId={}", userId);
            return null;
        }

        return convertToBasicInfo(user);
    }

    @Override
    @CacheEvict(value = "userBasicInfo", key = "#userId")
    public void clearUserCache(String userId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearUserBatchCache(List<String> userIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearUserCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "userBasicInfo", allEntries = true)
    public void clearAllUserCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private UserBasicInfo convertToBasicInfo(User user) {
        return convertToBasicInfo(user, null);
    }

    private UserBasicInfo convertToBasicInfo(User user, Map<String, StoreBasicInfo> storeInfoMap) {
        String storeName = null;
        if (user.getStoreId() != null) {
            String storeIdStr = String.valueOf(user.getStoreId());
            if (storeInfoMap != null && storeInfoMap.containsKey(storeIdStr)) {
                StoreBasicInfo storeInfo = storeInfoMap.get(storeIdStr);
                if (storeInfo != null) {
                    storeName = storeInfo.getStoreName();
                }
            } else {
                try {
                    StoreBasicInfo storeInfo = storeDataService.getStoreBasicInfo(String.valueOf(user.getStoreId()));
                    if (storeInfo != null) {
                        storeName = storeInfo.getStoreName();
                    }
                } catch (Exception e) {
                    log.warn("获取门店名称失败, storeId: {}", user.getStoreId(), e);
                }
            }
        }

        return UserBasicInfo.builder()
            .userId(user.getId().toString())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .departmentId(user.getDepartmentId())
            .departmentName(user.getDepartment())
            .storeId(user.getStoreId())
            .storeName(storeName)
            .avatar(user.getAvatar())
            .version(user.getVersion() != null ? user.getVersion().longValue() : 0L)
            .updateTime(user.getUpdatedTime())
            .build();
    }

    @Override
    public List<String> getAllUserIds() {
        List<User> users = userMapper.selectList(null);
        return users.stream()
            .map(user -> user.getId().toString())
            .collect(Collectors.toList());
    }
}
