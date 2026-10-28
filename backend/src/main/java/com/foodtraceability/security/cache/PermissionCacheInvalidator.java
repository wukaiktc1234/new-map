package com.foodtraceability.security.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * bug-㉑：权限变更后**主动失效**权限缓存。
 *
 * <p>为什么必须有：TTL（默认 45s）只是兜底。若只在 TTL 到期才生效，
 * 「后台撤了某人的权限、他还能用 45 秒」在生产会被当成 bug 报上来（安全上也不可接受）。</p>
 *
 * <p>调用点：任何改动 `role_permissions` / `user_permissions` 的操作完成后立即调用。
 * 已接入：{@code RoleController#assignPermissions}（角色-权限绑定）、
 * {@code PermissionController} 的授权/回收接口。</p>
 */
@Component
public class PermissionCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(PermissionCacheInvalidator.class);

    @Autowired(required = false)
    private UserPermissionCache userPermissionCache;

    /** 角色-权限绑定的任何变更（增删改）都会影响该角色下所有用户 → 全量失效。 */
    public void onRolePermissionsChanged(String reason) {
        if (userPermissionCache != null) {
            userPermissionCache.invalidateAll();
            log.info("[PERM-CACHE] 角色权限变更，已全量失效（reason={}）", reason);
        }
    }

    /** 单个用户的直接授权变更 → 只失效该用户。 */
    public void onUserPermissionsChanged(String userId, String reason) {
        if (userPermissionCache != null) {
            userPermissionCache.invalidate(userId);
            log.info("[PERM-CACHE] 用户权限变更，已失效 userId={}（reason={}）", userId, reason);
        }
    }
}
