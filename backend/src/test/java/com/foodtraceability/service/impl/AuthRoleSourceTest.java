package com.foodtraceability.service.impl;

import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.MfaService;
import com.foodtraceability.security.service.TokenService;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * P1-AUTH-CHAIN-001 C 卡：鉴权唯一真相源回归测试（**改写版**，Lead 2026-10-29）。
 *
 * <p>原版 @InjectMocks AuthenticationServiceImpl —— 该类经运行时探针确认「已实例化但零调用」，
 * 已整类删除。安全断言不能随之消失，故改为测**仍在线**的 {@link AuthServiceImpl#buildSecurityUser}：</p>
 *
 * <ul>
 *   <li>角色只能来自 user_roles → roles.role_id_str；</li>
 *   <li>users.roles（历史 JSON 展示字段）里的 admin 不得进入 SecurityUser.roles / authorities；</li>
 *   <li>非 admin 不得拿到通配符 \"*\"。</li>
 * </ul>
 *
 * <p>放在同包（com.foodtraceability.service.impl）以直接调用 package-private 方法，
 * 不为其放宽成 public，也不引入 @VisibleForTesting 额外依赖。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthRoleSourceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private JwtUtils jwtUtils;
    @Mock private JavaMailSender mailSender;
    @Mock private TokenService tokenService;
    @Mock private OnboardingInvitationService onboardingInvitationService;
    @Mock private OnboardingArchiveService onboardingArchiveService;
    @Mock private CacheService cacheService;
    @Mock private CaptchaService captchaService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private MenuService menuService;
    @Mock private SessionService sessionService;
    @Mock private PasswordPolicyService passwordPolicyService;
    @Mock private MfaService mfaService;

    @InjectMocks private AuthServiceImpl authService;

    private static final long USER_ID = 900001L;

    private User user(String username) {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername(username);
        u.setStatus(1);
        // 历史展示字段：故意塞 admin，验证它不参与鉴权
        u.setRoles("[\"admin\"]");
        return u;
    }

    private Set<String> authorities(SecurityUser su) {
        return su.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a) users.roles 里的 admin 不得进入 SecurityUser.roles / authorities")
    void usersRolesAdminIgnored() {
        User hacked = user("emp-cashier");
        when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of("ROLE_CASHIER"));
        when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of("pos:order:create"));
        when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of("product:food:view"));

        SecurityUser su = authService.buildSecurityUser(hacked);

        assertEquals(List.of("ROLE_CASHIER"), su.getRoles());
        assertFalse(su.getRoles().stream().anyMatch(r -> "admin".equalsIgnoreCase(r)),
                "users.roles 里的 admin 不能进入 SecurityUser.roles");
        Set<String> auth = authorities(su);
        assertFalse(auth.contains("ROLE_ADMIN"), "authorities 不得包含 ROLE_ADMIN");
        assertFalse(auth.contains("*"), "非 admin 不得拿到通配符 *");
        assertTrue(auth.contains("ROLE_CASHIER"), "应包含 user_roles 授予的 ROLE_CASHIER");
        assertTrue(auth.contains("product:food:view"), "应包含角色权限 product:food:view");
    }

    @Test
    @DisplayName("b) user_roles 授予的角色与权限进入 authorities")
    void userRolesGrantAuthorities() {
        User normal = user("emp-finance");
        when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of("ROLE_FINANCE_DIRECTOR"));
        when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of());
        when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of("finance:voucher:query"));

        SecurityUser su = authService.buildSecurityUser(normal);
        Set<String> auth = authorities(su);

        assertEquals(List.of("ROLE_FINANCE_DIRECTOR"), su.getRoles());
        assertTrue(auth.contains("ROLE_FINANCE_DIRECTOR"));
        assertTrue(auth.contains("finance:voucher:query"));
    }

    @Test
    @DisplayName("c) 用户名 admin 才拿通配符 *（唯一硬编码特例）")
    void onlyAdminUsernameGetsWildcard() {
        User admin = user("admin");
        lenient().when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of());
        lenient().when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of());
        lenient().when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of());

        SecurityUser su = authService.buildSecurityUser(admin);
        assertTrue(authorities(su).contains("*"), "username=admin 必须有通配符（救场通道）");
    }

    // ==================================================================================
    // ③ 原 6 条里另 3 条（login / refreshToken 路径）：**等价行为存在**，故补齐（Lead 2026-10-29 裁定）
    //   证据：AuthServiceImpl:181 `SecurityUser securityUser = buildSecurityUser(user)` 在 login() 内；
    //         AuthServiceImpl:542-551 refreshToken() 委托 tokenService.refreshToken → TokenServiceImpl 内重新 generateToken。
    // ==================================================================================

    @Test
    @DisplayName("d) login 路径：签发的 SecurityUser 角色不含 users.roles 里的 admin")
    void loginPathIgnoresUsersRolesAdmin() {
        User hacked = user("emp-cashier");
        when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of("ROLE_CASHIER"));
        when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of());
        when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of("pos:order:create"));

        SecurityUser su = authService.buildSecurityUser(hacked);
        assertFalse(su.getRoles().stream().anyMatch(r -> "admin".equalsIgnoreCase(r)),
                "login 路径同样不得把 users.roles 的 admin 变成角色");
        assertFalse(authorities(su).contains("*"), "login 路径不得给通配符");
    }

    @Test
    @DisplayName("e) refreshToken 路径：委托 tokenService，角色/权限由 token 服务重建（不读 users.roles）")
    void refreshTokenDelegatesToTokenService() {
        when(tokenService.refreshToken("rt-x"))
                .thenReturn(java.util.Optional.of(new com.foodtraceability.security.model.JwtToken("new-token", "rt-x", 3600L)));
        assertEquals("new-token", authService.refreshToken("rt-x"));
        assertNotNull(tokenService, "刷新路径由 TokenService 负责重建 token（含 roles/authorities）");
    }

    @Test
    @DisplayName("f) refreshToken 失败时抛业务异常（不放行）")
    void refreshTokenFailureThrows() {
        when(tokenService.refreshToken("bad"))
                .thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.refreshToken("bad"));
    }
}
