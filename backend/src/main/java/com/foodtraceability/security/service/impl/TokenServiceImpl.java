package com.foodtraceability.security.service.impl;

import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.config.JwtConfig;
import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.TokenService;
import com.foodtraceability.security.utils.JwtUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);

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


    public TokenServiceImpl(JwtUtils jwtUtils, UserMapper userMapper, JwtConfig jwtConfig) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.jwtConfig = jwtConfig;
    }

    private final JwtUtils jwtUtils;

    private final UserMapper userMapper;

    private final JwtConfig jwtConfig;

    @Override
    public JwtToken generateToken(SecurityUser user) {
        String token = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        // 存储令牌信息到Redis（包含RefreshToken jti列表管理）
        jwtUtils.storeTokenInfo(user.getUserId(), token, refreshToken);

        return new JwtToken(token, refreshToken, jwtConfig.getAccessTokenExpiration());
    }

    @Override
    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    @Override
    public Optional<SecurityUser> getUserFromToken(String token) {
        return jwtUtils.getUserFromToken(token);
    }

    @Override
    public Optional<JwtToken> refreshToken(String refreshToken) {
        try {
            // 1. 提取旧RefreshToken的jti
            String oldJti = jwtUtils.getJtiFromToken(refreshToken);

            // 2. 检查刷新令牌是否在黑名单中
            if (oldJti != null && jwtUtils.isJtiInBlacklist(oldJti)) {
                logger.warn("刷新令牌已在黑名单中，拒绝刷新: jti={}", oldJti);
                return Optional.empty();
            }

            // 3. 验证刷新令牌的有效性
            if (!jwtUtils.validateToken(refreshToken)) {
                return Optional.empty();
            }

            // 4. 从刷新令牌中获取用户信息
            Optional<SecurityUser> userOptional = jwtUtils.getUserFromToken(refreshToken);
            if (userOptional.isEmpty()) {
                return Optional.empty();
            }

            SecurityUser user = userOptional.get();

            // 5. 从数据库加载完整的用户信息（包括角色和权限）
            Long userId = Long.parseLong(user.getUserId());
            User dbUser = userMapper.selectById(userId);
            if (dbUser == null) {
                logger.warn("用户不存在，无法刷新令牌: userId={}", userId);
                return Optional.empty();
            }

            // 构建完整的角色列表
            List<String> roles = new ArrayList<>();
            List<String> roleCodes = userMapper.getUserRoles(userId);
            if (roleCodes != null) {
                roles.addAll(roleCodes);
            }
            if (dbUser.getRoles() != null && !dbUser.getRoles().isEmpty()) {
                if (dbUser.getRoles().startsWith("[")) {
                    String rolesStr = dbUser.getRoles().replace("[", "").replace("]", "").replace("\"", "");
                    for (String r : rolesStr.split(",")) {
                        if (!r.trim().isEmpty() && !roles.contains(r.trim())) {
                            roles.add(r.trim());
                        }
                    }
                } else if (!roles.contains(dbUser.getRoles())) {
                    roles.add(dbUser.getRoles());
                }
            }
            user.setRoles(roles);

            // 构建完整的权限列表
            Set<String> permissions = new HashSet<>();
            List<String> directPermissions = userMapper.getUserPermissions(userId);
            if (directPermissions != null) {
                permissions.addAll(directPermissions);
            }
            List<String> rolePermissions = userMapper.getUserRolePermissions(userId);
            if (rolePermissions != null) {
                permissions.addAll(rolePermissions);
            }

            // 管理员拥有所有权限
            boolean isAdmin = "admin".equalsIgnoreCase(dbUser.getUsername());
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

            user.setPermissions(new ArrayList<>(permissions));

            // 6. 将旧RefreshToken的jti加入黑名单（轮转机制核心）
            if (oldJti != null) {
                long remainingValidity = jwtUtils.getTokenRemainingValidity(refreshToken);
                if (remainingValidity <= 0) {
                    // 使用配置的RefreshToken过期时间作为兜底
                    remainingValidity = jwtConfig.getRefreshTokenExpiration();
                }
                jwtUtils.addToBlacklistByJti(oldJti, remainingValidity);
                // 从用户的RefreshToken列表中移除旧jti
                jwtUtils.removeRefreshTokenJti(user.getUserId(), oldJti);
                logger.info("旧RefreshToken已加入黑名单并从用户列表移除: jti={}", oldJti);
            }

            // 7. 生成新的令牌（generateToken内部会自动管理jti列表）
            return Optional.of(generateToken(user));
        } catch (Exception e) {
            logger.error("刷新令牌失败: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean invalidateToken(String token) {
        jwtUtils.addToBlacklist(token);
        return true;
    }

    @Override
    public boolean isTokenInvalidated(String token) {
        return jwtUtils.isTokenInBlacklist(token);
    }

    @Override
    public long getTokenRemainingValidity(String token) {
        return jwtUtils.getTokenRemainingValidity(token);
    }
}
