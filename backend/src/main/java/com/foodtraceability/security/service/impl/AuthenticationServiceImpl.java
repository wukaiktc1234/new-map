package com.foodtraceability.security.service.impl;

import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.MfaToken;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.AuthenticationService;
import com.foodtraceability.security.service.MfaService;
import com.foodtraceability.security.service.TokenService;
import com.foodtraceability.security.service.dto.*;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.security.utils.PasswordUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Set<String> ALL_PERMISSIONS = Set.of(
        "user:view", "user:edit",
        "employee:view", "employee:edit",
        "trace_code:view", "trace_code:edit",
        "kitchen:view", "kitchen:edit",
        "finance:view", "finance:edit",
        "product:food:view", "product:food:create", "product:food:update", "product:food:delete",
        "product:category:view", "product:category:create", "product:category:update", "product:category:delete",
        "product:combo:view", "product:combo:create", "product:combo:update", "product:combo:delete",
        "product:recipe:view", "product:recipe:create", "product:recipe:delete",
        "product:pricing:view", "product:pricing:adjust", "product:pricing:batch",
        "product:cost:view",
        // 财务模块细粒度权限（2026-06-30 新增）
        "finance:voucher:create", "finance:voucher:query", "finance:voucher:update", "finance:voucher:approve",
        "finance:payable:create", "finance:payable:query", "finance:payable:update", "finance:payable:delete", "finance:payable:approve",
        "finance:receivable:create", "finance:receivable:query", "finance:receivable:update", "finance:receivable:delete", "finance:receivable:approve",
        "finance:budget:create", "finance:budget:query", "finance:budget:update", "finance:budget:delete", "finance:budget:approve",
        "finance:cost:create", "finance:cost:query", "finance:cost:update", "finance:cost:delete", "finance:cost:approve",
        "finance:bank:create", "finance:bank:query", "finance:bank:update", "finance:bank:delete",
        "finance:period:create", "finance:period:query", "finance:period:update", "finance:period:approve",
        // 财务模块补充权限（2026-06-30 新增 - 修复 403）
        "finance:record:view", "finance:record:create", "finance:record:update", "finance:record:approve",
        "finance:invoice:view", "finance:invoice:create", "finance:invoice:update", "finance:invoice:delete",
        "finance:invoice:issue", "finance:invoice:void", "finance:invoice:red-flush", "finance:invoice:verify",
        "finance:warning:view", "finance:warning:create", "finance:warning:process", "finance:warning:delete",
        "finance:audit-log:view",
        "finance:approval:view",
        "finance:statistics:view",
        // 溯源模块细粒度权限（2026-06-30 新增）
        "trace:create", "trace:query", "trace:update", "trace:delete", "trace:recall", "trace:export"
    );


    public AuthenticationServiceImpl(TokenService tokenService, MfaService mfaService, JwtUtils jwtUtils, UserMapper userMapper) {
        this.tokenService = tokenService;
        this.mfaService = mfaService;
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
    }

    private final TokenService tokenService;

    private final MfaService mfaService;

    private final JwtUtils jwtUtils;

    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginRequest.getUsername())
        );

        if (user == null) {
            return LoginResponse.failure("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return LoginResponse.failure("用户账号已禁用");
        }

        if (!PasswordUtils.matches(loginRequest.getPassword(), user.getPassword())) {
            return LoginResponse.failure("用户名或密码错误");
        }

        SecurityUser securityUser = createSecurityUser(user);

        if (securityUser.isMfaEnabled()) {
            MfaToken mfaToken = mfaService.generateMfaToken(securityUser.getUserId());
            return LoginResponse.mfaRequired(mfaToken.getToken());
        }

        JwtToken jwtToken = tokenService.generateToken(securityUser);
        return LoginResponse.success(jwtToken, securityUser);
    }

    private SecurityUser createSecurityUser(User user) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(String.valueOf(user.getId()));
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(null);
        securityUser.setName(user.getFullName());
        securityUser.setEmail(user.getEmail());
        securityUser.setPhone(user.getPhone());
        securityUser.setStatus(user.getStatus());
        securityUser.setMfaEnabled(mfaService.isMfaEnabled(String.valueOf(user.getId())));

        Long userId = user.getId();
        
        List<String> roles = new ArrayList<>();
        List<String> roleCodes = userMapper.getUserRoles(userId);
        if (roleCodes != null) {
            roles.addAll(roleCodes);
        }
        
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            if (user.getRoles().startsWith("[")) {
                String rolesStr = user.getRoles().replace("[", "").replace("]", "").replace("\"", "");
                for (String r : rolesStr.split(",")) {
                    if (!r.trim().isEmpty() && !roles.contains(r.trim())) {
                        roles.add(r.trim());
                    }
                }
            } else if (!roles.contains(user.getRoles())) {
                roles.add(user.getRoles());
            }
        }
        securityUser.setRoles(roles);

        Set<String> permissions = new HashSet<>();
        
        List<String> directPermissions = userMapper.getUserPermissions(userId);
        if (directPermissions != null) {
            permissions.addAll(directPermissions);
        }
        
        List<String> rolePermissions = userMapper.getUserRolePermissions(userId);
        if (rolePermissions != null) {
            permissions.addAll(rolePermissions);
        }
        
        boolean isAdmin = "admin".equalsIgnoreCase(user.getUsername());
        if (!isAdmin && roles != null) {
            for (String role : roles) {
                if ("admin".equalsIgnoreCase(role) || "role_admin".equalsIgnoreCase(role)) {
                    isAdmin = true;
                    break;
                }
            }
        }
        
        if (isAdmin) {
            permissions.addAll(ALL_PERMISSIONS);
        }
        
        securityUser.setPermissions(new ArrayList<>(permissions));

        return securityUser;
    }

    @Override
    public Optional<JwtToken> verifyMfa(String userId, String mfaToken, String code) {
        if (mfaService.verifyMfaCode(userId, mfaToken, code)) {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                return Optional.empty();
            }
            SecurityUser securityUser = createSecurityUser(user);
            securityUser.setMfaVerified(true);
            JwtToken jwtToken = tokenService.generateToken(securityUser);
            return Optional.of(jwtToken);
        }
        return Optional.empty();
    }

    @Override
    public SecurityUser register(RegisterRequest registerRequest) {
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        if (!PasswordUtils.isValidPassword(registerRequest.getPassword())) {
            throw new IllegalArgumentException("密码强度不足，至少8位，包含大小写字母、数字和特殊字符");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(PasswordUtils.encryptPassword(registerRequest.getPassword()));
        user.setFullName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1);
        userMapper.insert(user);

        return createSecurityUser(user);
    }

    @Override
    public SecurityUser updateUserProfile(String userId, UserProfileUpdateRequest profileUpdateRequest) {
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setFullName(profileUpdateRequest.getName());
        user.setEmail(profileUpdateRequest.getEmail());
        user.setPhone(profileUpdateRequest.getPhone());
        userMapper.updateById(user);
        return createSecurityUser(user);
    }

    @Override
    public boolean changePassword(String userId, PasswordChangeRequest passwordChangeRequest) {
        if (!passwordChangeRequest.getNewPassword().equals(passwordChangeRequest.getConfirmNewPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }

        if (!PasswordUtils.isValidPassword(passwordChangeRequest.getNewPassword())) {
            throw new IllegalArgumentException("新密码强度不足，至少8位，包含大小写字母、数字和特殊字符");
        }

        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            return false;
        }

        if (!PasswordUtils.matches(passwordChangeRequest.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        user.setPassword(PasswordUtils.encryptPassword(passwordChangeRequest.getNewPassword()));
        userMapper.updateById(user);
        return true;
    }

    @Override
    public Optional<JwtToken> refreshToken(String refreshToken) {
        return tokenService.refreshToken(refreshToken);
    }

    @Override
    public boolean logout(String token) {
        return tokenService.invalidateToken(token);
    }

    @Override
    public Optional<SecurityUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return Optional.of((SecurityUser) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    @Override
    public Optional<SecurityUser> getUserByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(createSecurityUser(user));
    }

    @Override
    public Optional<SecurityUser> getUserById(String userId) {
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(createSecurityUser(user));
    }
}
