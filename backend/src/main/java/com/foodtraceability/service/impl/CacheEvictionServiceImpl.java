package com.foodtraceability.service.impl;

import com.foodtraceability.service.CacheEvictionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 缓存清除服务实现（基于内存，已移除 Redis 和 TwoLevelCacheManager 依赖）
 * 由于项目改为内存缓存（各 DataService 内部缓存），此处仅记录日志，
 * 实际缓存清除由各 DataService 自身管理。
 */
@Service
public class CacheEvictionServiceImpl implements CacheEvictionService {
    private static final Logger log = LoggerFactory.getLogger(CacheEvictionServiceImpl.class);

    @Override
    public void evictUserCache(String userId) {
        log.info("用户缓存清理请求: userId={}", userId);
    }

    @Override
    public void evictStoreCache(String storeId) {
        log.info("门店缓存清理请求: storeId={}", storeId);
    }

    @Override
    public void evictProductCache(String productId) {
        log.info("产品缓存清理请求: productId={}", productId);
    }

    @Override
    public void evictPositionCache(String positionId) {
        log.info("职位缓存清理请求: positionId={}", positionId);
    }

    @Override
    public void evictDepartmentCache(String departmentId) {
        log.info("部门缓存清理请求: departmentId={}", departmentId);
    }

    @Override
    public void evictRoleCache(String roleId) {
        log.info("角色缓存清理请求: roleId={}", roleId);
    }

    @Override
    public void evictUserListCache() {
        log.info("用户列表缓存清理请求");
    }

    @Override
    public void evictStoreListCache() {
        log.info("门店列表缓存清理请求");
    }

    @Override
    public void evictProductListCache() {
        log.info("产品列表缓存清理请求");
    }

    @Override
    public void evictPositionListCache() {
        log.info("职位列表缓存清理请求");
    }

    @Override
    public void evictDepartmentListCache() {
        log.info("部门列表缓存清理请求");
    }

    @Override
    public void evictRoleListCache() {
        log.info("角色列表缓存清理请求");
    }

    @Override
    public void evictAllCache() {
        log.info("所有缓存清理请求");
    }
}
