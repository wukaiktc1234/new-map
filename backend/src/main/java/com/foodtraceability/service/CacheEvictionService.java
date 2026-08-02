package com.foodtraceability.service;

import java.util.List;

public interface CacheEvictionService {

    void evictUserCache(String userId);

    void evictStoreCache(String storeId);

    void evictProductCache(String productId);

    void evictPositionCache(String positionId);

    void evictDepartmentCache(String departmentId);

    void evictRoleCache(String roleId);

    void evictUserListCache();

    void evictStoreListCache();

    void evictProductListCache();

    void evictPositionListCache();

    void evictDepartmentListCache();

    void evictRoleListCache();

    void evictAllCache();
}
