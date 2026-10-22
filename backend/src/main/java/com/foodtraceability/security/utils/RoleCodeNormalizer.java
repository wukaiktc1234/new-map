package com.foodtraceability.security.utils;

import java.util.Locale;

/**
 * 角色码归一化工具 —— P1-AUTH-CHAIN-001-A（N30 止血）。
 *
 * <p>背景（board §24.13a N30 实测）：全仓 471 处 {@code hasRole(...)} / {@code hasAnyRole(...)} 依赖角色码比较，
 * 但历史数据与注解存在三种写法混用：
 * <ol>
 *   <li>{@code user_roles.role_id} 存 {@code ROLE_STORE_MANAGER}（带前缀大写）；</li>
 *   <li>{@code JwtUtils} 写入 claim 前统一 {@code toLowerCase()}，产出一 {@code role_store_manager}；</li>
 *   <li>{@code SecurityUser} 原实现直接拼 {@code "ROLE_" + role.toUpperCase()}，对带前缀值生成
 *       {@code ROLE_ROLE_STORE_MANAGER}，导致 Spring {@code hasRole('store_manager')}（期望
 *       {@code ROLE_STORE_MANAGER}）永远不匹配；注解侧又混用 {@code 'STORE_MANAGER'} / {@code 'store_manager'} /
 *       {@code 'ROLE_REGIONAL_MANAGER'} 等大小写与前缀变体。</li>
 * </ol>
 *
 * <p>本工具只做**字符串归一**，不查库、不改数据，是 A 卡「止血」的唯一收敛点：
 * <ul>
 *   <li>{@link #normalize(String)}：脱引号/转义残留 → trim → 循环剥 {@code ROLE_} 前缀 → 小写。
 *       例：{@code "ROLE_STORE_MANAGER"} / {@code "role_store_manager"} / {@code "'STORE_MANAGER'"} /
 *       {@code "hr\\"} 均归一为 {@code store_manager} / {@code store_manager} / {@code store_manager} / {@code hr}。</li>
 *   <li>{@link #toAuthority(String)}：生成 Spring Security authority，恒为 {@code ROLE_} + 全大写规范码，
 *       不会产生双前缀。</li>
 *   <li>{@link #matches(String, String)}：两侧归一后比较，任一为 {@code null} 视为不匹配（fail-closed）。</li>
 * </ul>
 *
 * <p>注意：本类不解决「码在 DB 中不存在」的问题（18 个幻码见
 * {@code docs/architecture/03-review/p1-auth-chain-001-a-n30-phantom-role-codes.md}），那属 B 卡。
 */
public final class RoleCodeNormalizer {

    /** Spring Security 角色 authority 固定前缀 */
    public static final String ROLE_PREFIX = "ROLE_";

    private RoleCodeNormalizer() {
    }

    /**
     * 归一为小写、无前缀的角色编码。
     *
     * @param raw 原始角色值（可能带 {@code ROLE_} 前缀、大小写混用、含引号或转义残留）
     * @return 归一后的编码；{@code null} / 空白 / 仅含前缀时返回 {@code null}
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        // 注解字符串与 SpEL 转义可能带入引号、反斜杠
        String s = raw.replace("\\", "").replace("\"", "").replace("'", "").trim();
        if (s.isEmpty()) {
            return null;
        }
        // 循环剥离：兼容历史脏值 ROLE_ROLE_XXX；
        // 用 >= 而非 >：裸 "ROLE_" 也应被剥成空串并归一为 null，否则会产出 ROLE_ROLE_ 脏 authority
        while (s.length() >= ROLE_PREFIX.length() && s.toUpperCase(Locale.ROOT).startsWith(ROLE_PREFIX)) {
            s = s.substring(ROLE_PREFIX.length()).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * 生成 Spring Security authority：{@code ROLE_} + 全大写规范码（永不出现 {@code ROLE_ROLE_}）。
     *
     * @param raw 原始角色值
     * @return authority；无法归一时返回 {@code null}
     */
    public static String toAuthority(String raw) {
        String code = normalize(raw);
        return code == null ? null : ROLE_PREFIX + code.toUpperCase(Locale.ROOT);
    }

    /**
     * 归一后比较两个角色值是否同一角色（大小写、{@code ROLE_} 前缀不影响结果）。
     *
     * @param left  左值
     * @param right 右值
     * @return 同一角色返回 {@code true}；任一无法归一（{@code null}/空白）返回 {@code false}
     */
    public static boolean matches(String left, String right) {
        String a = normalize(left);
        if (a == null) {
            return false;
        }
        String b = normalize(right);
        if (b == null) {
            return false;
        }
        return a.equals(b);
    }
}
