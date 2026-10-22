package com.foodtraceability.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.mapper.PermissionMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * admin 权限目录（运行时全量权限码快照）—— P1-AUTH-CHAIN-001 / N31（Owner 裁决 1：admin 全局兜底）。
 *
 * <p><b>要解决的问题</b>：{@code @PreAuthorize("hasAuthority('backup:query')")} 这类注解**没有**
 * {@code or hasAuthority('*')} 兜底，而 {@code SecurityUser} 对 admin 只展开硬编码的
 * {@link com.foodtraceability.security.constants.AdminPermissions#ALL}（352 个码）。
 * 一旦注解引用的权限码既不在 DB 目录、也不在该硬编码清单里（N31 实测 B3 类 34 个码 / 67 处），
 * **连 admin 都进不去**——备份、通知、调度等基础设施端点全部死路。
 *
 * <p><b>做法</b>：启动时把 DB 目录里的全部权限码（{@code permissions} 表，status=1）读进本类，
 * 由 {@code SecurityUser#getAuthorities()} 在 admin 分支里与 {@code AdminPermissions.ALL} 一并展开。
 * 这样**不需要改任何一处注解**（Owner 裁决 1 要求「不要改 67 处注解」），且新增权限码只落库即可对 admin 生效。
 *
 * <p><b>为什么不塞进 JWT</b>：JWT 里 admin 只存 {@code "*"}（见 {@code AuthServiceImpl#buildSecurityUser}
 * 注释：避免 token &gt; 9KB / Tomcat 8KB header 限制），展开必须在请求时进行。
 *
 * <p><b>为什么用静态集合</b>：{@code SecurityUser} 是普通领域对象（JWT 解析时 new 出来，不在 Spring 容器内），
 * 无法注入本 Bean，故用静态持有；本类只读、幂等、线程安全（{@link ConcurrentHashMap#newKeySet()}）。
 *
 * <p><b>边界</b>：本目录只服务 admin / owner 的 authority 展开；**不改变**非 admin 角色的鉴权结果
 * （普通角色能否访问仍取决于 {@code role_permissions} 与注解写法）。
 */
@Component
public class AdminPermissionCatalog {

    private static final Logger logger = LoggerFactory.getLogger(AdminPermissionCatalog.class);

    private static final Set<String> CODES = ConcurrentHashMap.newKeySet();

    private final PermissionMapper permissionMapper;

    public AdminPermissionCatalog(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重新加载目录（可在权限初始化/批量授权后手动调用）。
     */
    public void reload() {
        try {
            List<Permission> list = permissionMapper.selectList(
                    new LambdaQueryWrapper<Permission>().eq(Permission::getStatus, 1));
            Set<String> next = ConcurrentHashMap.newKeySet();
            if (list != null) {
                for (Permission p : list) {
                    if (p == null || p.getPermissionCode() == null) {
                        continue;
                    }
                    String code = p.getPermissionCode().trim();
                    if (!code.isEmpty()) {
                        next.add(code);
                    }
                }
            }
            CODES.clear();
            CODES.addAll(next);
            logger.info("admin 权限目录加载完成: {} 个权限码", CODES.size());
        } catch (Exception e) {
            // 加载失败不阻断启动：admin 退化为仅 AdminPermissions.ALL（与改造前等价，fail-safe）
            logger.warn("admin 权限目录加载失败，admin 将仅使用 AdminPermissions.ALL: {}", e.getMessage());
        }
    }

    /**
     * 目录内全部权限码（只读快照）。
     */
    public static Set<String> codes() {
        return Collections.unmodifiableSet(CODES);
    }
}
