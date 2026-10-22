package com.foodtraceability.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SecurityUser#getAuthorities()} 单元测试 —— P1-AUTH-CHAIN-001-A（N30 止血）。
 *
 * <p>锁定的历史缺陷：JWT claim 中 roles 已被 {@code JwtUtils} 统一小写（如 {@code role_store_manager}），
 * 旧实现直接拼 {@code "ROLE_" + role.toUpperCase()} 得到 {@code ROLE_ROLE_STORE_MANAGER}，
 * 而 Spring {@code hasRole('store_manager')} 期望 {@code ROLE_STORE_MANAGER} → 471 处角色校验对非 admin 恒 false。
 */
class SecurityUserAuthoritiesTest {

    private static Set<String> authorityNames(SecurityUser user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("表路径角色（ROLE_STORE_MANAGER）→ authority 为 ROLE_STORE_MANAGER，不出现 ROLE_ROLE_")
    void tablePathRoleProducesSinglePrefix() {
        SecurityUser user = new SecurityUser();
        user.setRoles(List.of("ROLE_STORE_MANAGER"));

        Set<String> names = authorityNames(user);

        assertTrue(names.contains("ROLE_STORE_MANAGER"), "必须含 ROLE_STORE_MANAGER，实际: " + names);
        assertFalse(names.contains("ROLE_ROLE_STORE_MANAGER"), "不得出现双前缀，实际: " + names);
    }

    @Test
    @DisplayName("JWT 小写角色（role_store_manager）→ 同样得到 ROLE_STORE_MANAGER")
    void jwtLowercasedRoleProducesSameAuthority() {
        SecurityUser user = new SecurityUser();
        user.setRoles(List.of("role_store_manager"));

        Set<String> names = authorityNames(user);

        assertTrue(names.contains("ROLE_STORE_MANAGER"), "必须含 ROLE_STORE_MANAGER，实际: " + names);
        assertFalse(names.contains("ROLE_ROLE_STORE_MANAGER"), "不得出现双前缀，实际: " + names);
    }

    @Test
    @DisplayName("注解大写写法（STORE_MANAGER）→ 同样得到 ROLE_STORE_MANAGER")
    void uppercaseRoleProducesSameAuthority() {
        SecurityUser user = new SecurityUser();
        user.setRoles(List.of("STORE_MANAGER"));

        assertTrue(authorityNames(user).contains("ROLE_STORE_MANAGER"));
    }

    @Test
    @DisplayName("admin 三种写法均命中管理员分支：添加 * 并展开 AdminPermissions.ALL")
    void adminVariantsGrantWildcard() {
        for (String adminRole : List.of("admin", "ADMIN", "ROLE_ADMIN")) {
            SecurityUser user = new SecurityUser();
            user.setRoles(List.of(adminRole));

            Set<String> names = authorityNames(user);
            assertTrue(names.contains("*"), adminRole + " 应命中管理员通配，实际: " + names);
            assertTrue(names.contains("ROLE_ADMIN"), adminRole + " 应含 ROLE_ADMIN，实际: " + names);
            assertFalse(names.contains("ROLE_ROLE_ADMIN"), adminRole + " 不得出现 ROLE_ROLE_ADMIN");
        }
    }

    @Test
    @DisplayName("非 admin 角色不得获得 * 通配（不得 fail-open）")
    void nonAdminRoleDoesNotGainWildcard() {
        SecurityUser user = new SecurityUser();
        user.setRoles(List.of("ROLE_STORE_MANAGER"));

        Set<String> names = authorityNames(user);
        assertFalse(names.contains("*"), "店长不得获得通配权限，实际: " + names);
    }

    @Test
    @DisplayName("permissions 中的具体权限码原样成为 authority（含 order:manage）")
    void permissionsBecomeAuthorities() {
        SecurityUser user = new SecurityUser();
        user.setRoles(List.of("role_store_manager"));
        user.setPermissions(List.of("order:manage", "order:view", "inventory:query"));

        Set<String> names = authorityNames(user);

        assertTrue(names.contains("order:manage"));
        assertTrue(names.contains("order:view"));
        assertTrue(names.contains("inventory:query"));
    }

    @Test
    @DisplayName("非法/空白角色值被跳过，不产生空 authority")
    void blankRoleIsSkipped() {
        SecurityUser user = new SecurityUser();
        user.setRoles(java.util.Arrays.asList(null, "", "   ", "ROLE_"));

        Set<String> names = authorityNames(user);

        assertTrue(names.isEmpty(), "空白角色不应产生 authority，实际: " + names);
    }
}
