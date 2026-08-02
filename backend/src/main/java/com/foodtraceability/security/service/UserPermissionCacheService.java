package com.foodtraceability.security.service;

import com.foodtraceability.security.model.UserPermissionInfo;

import java.util.Optional;

public interface UserPermissionCacheService {
    
    Optional<UserPermissionInfo> getUserPermissionInfo(String userId);
    
    void cacheUserPermissionInfo(String userId, UserPermissionInfo permissionInfo);
    
    void evictUserPermissionCache(String userId);
    
    void evictAllUserPermissionCache();
}