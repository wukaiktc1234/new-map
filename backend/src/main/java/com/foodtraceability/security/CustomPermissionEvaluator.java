package com.foodtraceability.security;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * 自定义权限评估器
 * 实现PermissionEvaluator接口，用于处理hasPermission表达式
 */
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // 检查认证对象
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 获取用户权限
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }

        // 检查是否有*权限
        for (GrantedAuthority authority : authorities) {
            if ("*".equals(authority.getAuthority())) {
                return true;
            }
        }

        // 检查是否有指定的权限
        if (permission instanceof String) {
            for (GrantedAuthority authority : authorities) {
                if (permission.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // 同样的逻辑，处理基于ID和类型的权限检查
        return hasPermission(authentication, null, permission);
    }
}