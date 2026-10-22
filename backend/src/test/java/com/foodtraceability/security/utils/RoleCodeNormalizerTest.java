package com.foodtraceability.security.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RoleCodeNormalizer} 单元测试 —— P1-AUTH-CHAIN-001-A（N30 止血）。
 *
 * <p>覆盖 board §24.13a N30 记录的三类写法混用：
 * <ul>
 *   <li>{@code user_roles.role_id} = {@code ROLE_STORE_MANAGER}（带前缀大写）</li>
 *   <li>JWT claim = {@code role_store_manager}（{@code JwtUtils} 统一小写）</li>
 *   <li>注解实参 = {@code 'STORE_MANAGER'} / {@code 'store_manager'} / {@code 'ROLE_REGIONAL_MANAGER'}</li>
 * </ul>
 */
class RoleCodeNormalizerTest {

    @Test
    @DisplayName("normalize：剥 ROLE_ 前缀并小写（三种历史写法归一为同一值）")
    void normalizeStripsPrefixAndLowercases() {
        assertEquals("store_manager", RoleCodeNormalizer.normalize("ROLE_STORE_MANAGER"));
        assertEquals("store_manager", RoleCodeNormalizer.normalize("role_store_manager"));
        assertEquals("store_manager", RoleCodeNormalizer.normalize("STORE_MANAGER"));
        assertEquals("store_manager", RoleCodeNormalizer.normalize("store_manager"));
        assertEquals("store_manager", RoleCodeNormalizer.normalize("  Store_Manager  "));
    }

    @Test
    @DisplayName("normalize：兼容转义/引号残留与 ROLE_ROLE_ 历史脏值")
    void normalizeToleratesEscapesAndDoublePrefix() {
        assertEquals("hr", RoleCodeNormalizer.normalize("hr\\"));
        assertEquals("hr", RoleCodeNormalizer.normalize("'hr'"));
        assertEquals("hr", RoleCodeNormalizer.normalize("\"hr\""));
        assertEquals("store_manager", RoleCodeNormalizer.normalize("ROLE_ROLE_STORE_MANAGER"));
    }

    @Test
    @DisplayName("normalize：null / 空白 / 仅前缀 → null（fail-closed）")
    void normalizeReturnsNullForBlank() {
        assertNull(RoleCodeNormalizer.normalize(null));
        assertNull(RoleCodeNormalizer.normalize(""));
        assertNull(RoleCodeNormalizer.normalize("   "));
        assertNull(RoleCodeNormalizer.normalize("ROLE_"));
    }

    @Test
    @DisplayName("normalize：纯数字角色 ID 原样保留小写（不误判为角色码）")
    void normalizeKeepsNumericId() {
        assertEquals("9", RoleCodeNormalizer.normalize("9"));
        assertEquals("9", RoleCodeNormalizer.normalize(" 9 "));
    }

    @Test
    @DisplayName("toAuthority：恒为 ROLE_ + 全大写，不产生 ROLE_ROLE_（N30 核心缺陷回归）")
    void toAuthorityNeverDoublePrefixes() {
        assertEquals("ROLE_STORE_MANAGER", RoleCodeNormalizer.toAuthority("role_store_manager"));
        assertEquals("ROLE_STORE_MANAGER", RoleCodeNormalizer.toAuthority("ROLE_STORE_MANAGER"));
        assertEquals("ROLE_STORE_MANAGER", RoleCodeNormalizer.toAuthority("STORE_MANAGER"));
        assertFalse(RoleCodeNormalizer.toAuthority("ROLE_STORE_MANAGER").contains("ROLE_ROLE_"));
        assertNull(RoleCodeNormalizer.toAuthority(null));
    }

    @Test
    @DisplayName("matches：大小写与前缀不影响结果（hasRole 比较面）")
    void matchesIgnoresCaseAndPrefix() {
        assertTrue(RoleCodeNormalizer.matches("ROLE_STORE_MANAGER", "store_manager"));
        assertTrue(RoleCodeNormalizer.matches("role_store_manager", "STORE_MANAGER"));
        assertTrue(RoleCodeNormalizer.matches("ROLE_STORE_MANAGER", "ROLE_STORE_MANAGER"));
        assertFalse(RoleCodeNormalizer.matches("ROLE_STORE_MANAGER", "hr_director"));
    }

    @Test
    @DisplayName("matches：幻码无论如何归一都不匹配真实码（B 卡范畴，A 卡只能保证不误判）")
    void matchesDoesNotResurrectPhantomCodes() {
        assertFalse(RoleCodeNormalizer.matches("ROLE_STORE_MANAGER", "HR_MANAGER"));
        assertFalse(RoleCodeNormalizer.matches("ROLE_STORE_MANAGER", "regional_manager"));
        assertFalse(RoleCodeNormalizer.matches("ROLE_REGION_MANAGER", "regional_manager"));
    }

    @Test
    @DisplayName("matches：任一为 null/空白 → false（不得 fail-open）")
    void matchesIsFailClosed() {
        assertFalse(RoleCodeNormalizer.matches(null, "store_manager"));
        assertFalse(RoleCodeNormalizer.matches("store_manager", null));
        assertFalse(RoleCodeNormalizer.matches("", ""));
        assertFalse(RoleCodeNormalizer.matches("   ", "store_manager"));
    }
}
