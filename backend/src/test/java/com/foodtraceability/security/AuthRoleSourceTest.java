package com.foodtraceability.security;

import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.MfaService;
import com.foodtraceability.security.service.TokenService;
import com.foodtraceability.security.service.impl.AuthenticationServiceImpl;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.service.RoleService;
import com.foodtraceability.service.UserDataService;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.StoreDataService;
import com.foodtraceability.service.impl.AuthServiceImpl;
import com.foodtraceability.service.impl.UserServiceImpl;
import com.foodtraceability.service.CacheService;
import com.foodtraceability.service.CaptchaService;
import com.foodtraceability.service.MenuService;
import com.foodtraceability.service.OnboardingArchiveService;
import com.foodtraceability.service.OnboardingInvitationService;
import com.foodtraceability.service.PasswordPolicyService;
import com.foodtraceability.service.PasswordResetService;
import com.foodtraceability.service.SessionService;
import com.foodtraceability.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-AUTH-CHAIN-001 C 卡：鉴权唯一真相源回归测试。
 *
 * <p>断言 users.roles（JSON 展示字段）不再参与任何鉴权判定：
 * 把它改成 ["admin"] 也拿不到 admin 角色 / ROLE_ADMIN authority；
 * 真实角色与权限只来自 user_roles → roles → role_permissions。</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthRoleSourceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenService tokenService;

    @Mock
    private MfaService mfaService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private static final long USER_ID = 900001L;

    private User user(String username, String rolesJson) {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername(username);
        u.setStatus(1);
        u.setRoles(rolesJson);
        u.setLocationId(1L);
        return u;
    }

    private static Set<String> authorityNames(SecurityUser securityUser) {
        return securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a) users.roles=[\"admin\"] 不能提权：roles 不含 admin，authorities 不含 ROLE_ADMIN")
    void usersRolesJsonMustNotGrantAdmin() {
        User hacked = user("emp-hacked", "[\"admin\"]");

        // 授权权威源：该用户只有 cashier 角色 + 一个 POS 权限
        when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of("cashier"));
        when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of("pos:order:create"));
        when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of("product:food:view"));
        lenient().when(mfaService.isMfaEnabled(String.valueOf(USER_ID))).thenReturn(false);

        SecurityUser securityUser = authenticationService.buildSecurityUser(hacked);

        assertEquals(List.of("cashier"), securityUser.getRoles(),
                "角色只能来自 user_roles，users.roles 里的 admin 必须被忽略");
        assertFalse(securityUser.getRoles().stream().anyMatch(r -> "admin".equalsIgnoreCase(r)),
                "users.roles 里的 admin 不能进入 SecurityUser.roles");

        Set<String> authorities = authorityNames(securityUser);
        assertFalse(authorities.contains("ROLE_ADMIN"), "authorities 不得包含 ROLE_ADMIN");
        assertFalse(authorities.contains("*"), "authorities 不得包含通配符 *（admin 专属）");
        assertTrue(authorities.contains("ROLE_CASHIER"), "应包含 user_roles 授予的 ROLE_CASHIER");
        assertTrue(authorities.contains("product:food:view"), "应包含角色权限 product:food:view");
    }

    @Test
    @DisplayName("b) user_roles 授予的角色与权限进入 authorities")
    void userRolesGrantAuthorities() {
        User normal = user("emp-finance", "[14]");

        when(userMapper.getUserRoles(USER_ID)).thenReturn(List.of("finance_director"));
        when(userMapper.getUserPermissions(USER_ID)).thenReturn(List.of());
        when(userMapper.getUserRolePermissions(USER_ID)).thenReturn(List.of("finance:voucher:query"));
        lenient().when(mfaService.isMfaEnabled(String.valueOf(USER_ID))).thenReturn(false);

        SecurityUser securityUser = authenticationService.buildSecurityUser(normal);
        Set<String> authorities = authorityNames(securityUser);

        assertEquals(List.of("finance_director"), securityUser.getRoles());
        assertTrue(authorities.contains("ROLE_FINANCE_DIRECTOR"),
                "user_roles 授予的 finance_director 应进入 authorities");
        assertTrue(authorities.contains("finance:voucher:query"),
                "role_permissions 的权限码应进入 authorities");
        assertFalse(authorities.contains("*"));
    }

    @Test
    @DisplayName("c) getUserRoleNames 返回 role_code；中文名走独立的 getUserRoleDisplayNames")
    void roleNamesReturnCodesAndDisplayNamesSeparated() {
        UserServiceImpl userService = new UserServiceImpl(
                org.mockito.Mockito.mock(PasswordEncoder.class),
                org.mockito.Mockito.mock(RoleService.class),
                org.mockito.Mockito.mock(UserDataService.class),
                org.mockito.Mockito.mock(StoreDataService.class),
                org.mockito.Mockito.mock(EmployeeService.class),
                org.mockito.Mockito.mock(LocationService.class));
        // ServiceImpl 的 baseMapper 由 MyBatis-Plus 注入，这里用反射塞 mock
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);

        Role role = new Role();
        role.setId(9L);
        role.setRoleCode("store_manager");
        role.setRoleIdStr("ROLE_STORE_MANAGER");
        role.setRoleName("店长/门店经理");
        when(userMapper.getRoleEntitiesByUserId(USER_ID)).thenReturn(List.of(role));

        assertEquals(List.of("store_manager"), userService.getUserRoleNames(USER_ID),
                "getUserRoleNames 必须返回 role_code，而不是中文名");
        assertEquals(List.of("店长/门店经理"), userService.getUserRoleDisplayNames(USER_ID),
                "中文名由 getUserRoleDisplayNames 单独提供（仅展示用）");
        assertTrue(userService.hasRole(USER_ID, "store_manager"));
        assertFalse(userService.hasRole(USER_ID, "admin"));
    }

    // ==================== d) admin 兜底：/auth/me 的 roles 必须含 admin ====================

    private AuthServiceImpl authService(UserService userService) {
        return new AuthServiceImpl(
                userService,
                userMapper,
                jwtUtils,
                mock(JavaMailSender.class),
                tokenService,
                mock(OnboardingInvitationService.class),
                mock(OnboardingArchiveService.class),
                mock(CacheService.class),
                mock(CaptchaService.class),
                mock(PasswordResetService.class),
                mock(MenuService.class),
                mock(SessionService.class),
                mock(PasswordPolicyService.class));
    }

    /** 把「管理员 principal」放进 SecurityContext（模拟 admin token 里的 ROLE_ADMIN / *） */
    private SecurityUser loginAs(String username, List<String> roles, List<String> permissions) {
        SecurityUser principal = new SecurityUser();
        principal.setUserId(String.valueOf(USER_ID));
        principal.setUsername(username);
        principal.setRoles(new ArrayList<>(roles));
        principal.setPermissions(new ArrayList<>(permissions));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return principal;
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("d) admin 账号（无 user_roles 行）的 /auth/me 角色必须含 admin，不能返回空数组")
    void adminWithoutUserRolesStillGetsAdminRole() {
        User admin = user("admin", null);          // 内置 admin：users.roles 也不依赖
        UserService userService = mock(UserService.class);
        when(userService.getUserRoleNames(USER_ID)).thenReturn(List.of());   // 没有 user_roles 行

        loginAs("admin", List.of("admin"), List.of("*"));

        List<String> roles = authService(userService).resolveRoleCodesWithAdminFallback(admin);

        assertTrue(roles.contains("admin"),
                "admin 账号 /auth/me 的 roles 必须含 admin，实际=" + roles);
        assertFalse(roles.isEmpty(), "admin 账号 /auth/me 的 roles 不能为空数组");
    }

    @Test
    @DisplayName("d2) 非管理员 principal 不会因兜底逻辑获得 admin")
    void nonAdminPrincipalDoesNotGetAdminRole() {
        User cashier = user("vcashier01", null);
        UserService userService = mock(UserService.class);
        when(userService.getUserRoleNames(USER_ID)).thenReturn(List.of("cashier"));

        loginAs("vcashier01", List.of("cashier"), List.of("pos:order:create"));

        List<String> roles = authService(userService).resolveRoleCodesWithAdminFallback(cashier);

        assertEquals(List.of("cashier"), roles, "非管理员不得被补 admin");
    }

    @Test
    @DisplayName("d3) 管理员查他人 /permissions 时不会把 admin 补到别人身上")
    void adminRoleNotLeakedToOtherUsers() {
        User other = user("emp-b", null);
        UserService userService = mock(UserService.class);
        when(userService.getUserRoleNames(USER_ID)).thenReturn(List.of("employee"));

        loginAs("admin", List.of("admin"), List.of("*"));   // 当前登录人是 admin

        List<String> roles = authService(userService).resolveRoleCodesWithAdminFallback(other);

        assertEquals(List.of("employee"), roles, "别人的角色列表不得被补 admin");
    }
}
