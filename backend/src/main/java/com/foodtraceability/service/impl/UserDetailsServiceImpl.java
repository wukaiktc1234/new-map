package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

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
        "trace:create", "trace:query", "trace:update", "trace:delete", "trace:recall", "trace:export",
        // 采购模块权限（2026-06-30 新增 - 修复采购申请/计划/订单/合同/入库/结算/供应商/商品档案/物资需求 403）
        "purchase:request:view", "purchase:request:create", "purchase:request:edit", "purchase:request:delete", "purchase:request:approve", "purchase:request:generate-order",
        "purchase:plan:view", "purchase:plan:create", "purchase:plan:edit", "purchase:plan:delete", "purchase:plan:approve",
        "purchase:order:view", "purchase:order:create", "purchase:order:edit", "purchase:order:delete", "purchase:order:approve",
        "purchase:contract:view", "purchase:contract:create", "purchase:contract:edit", "purchase:contract:delete", "purchase:contract:approve",
        "purchase:stockin:view", "purchase:stockin:create", "purchase:stockin:edit", "purchase:stockin:delete",
        "purchase:stockin:scan", "purchase:stockin:intervene", "purchase:stockin:cancel", "purchase:stockin:complete", "purchase:stockin:print",
        "purchase:settlement:view", "purchase:settlement:create", "purchase:settlement:edit", "purchase:settlement:delete", "purchase:settlement:approve",
        "purchase:archive:view", "purchase:archive:create", "purchase:archive:edit", "purchase:archive:delete",
        "purchase:category:view", "purchase:category:create", "purchase:category:edit", "purchase:category:delete",
        "purchase:supplier:view", "purchase:supplier:create", "purchase:supplier:edit", "purchase:supplier:delete",
        "purchase:material-request:view", "purchase:material-request:create", "purchase:material-request:edit", "purchase:material-request:delete", "purchase:material-request:approve",
        // 仓储模块权限（2026-06-30 新增 - 修复仓库/库存/盘点/调拨/预警/报损/出库/调整 403）
        "warehouse:create", "warehouse:update", "warehouse:query", "warehouse:delete",
        "inventory:query", "inventory:create", "inventory:update", "inventory:delete", "inventory:approve", "inventory:execute", "inventory:lock", "inventory:deduct"
    );

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 修复：User.status 是 Integer，不能用 String "1" 比较
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户账号已禁用: " + username);
        }

        List<String> roles = userMapper.getUserRoles(user.getId());
        List<String> permissions = getMergedPermissions(user);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        // admin 用户始终拥有 ROLE_admin 角色（用于 hasAnyRole('admin', ...) 端点鉴权）
        boolean isAdminUser = "admin".equalsIgnoreCase(username);
        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                if ("admin".equalsIgnoreCase(role)) {
                    isAdminUser = true;
                }
            }
        }
        if (isAdminUser) {
            authorities.add(new SimpleGrantedAuthority("ROLE_admin"));
        }
        if (permissions != null) {
            for (String permission : permissions) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        logger.debug("Loaded user {} with authorities: {}", username, authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    private List<String> getMergedPermissions(User user) {
        try {
            Long userId = user.getId();
            Set<String> mergedPermissions = new HashSet<>();

            List<String> directPermissions = userMapper.getUserPermissions(userId);
            if (directPermissions != null) {
                mergedPermissions.addAll(directPermissions);
            }

            List<String> rolePermissions = userMapper.getUserRolePermissions(userId);
            if (rolePermissions != null) {
                mergedPermissions.addAll(rolePermissions);
            }

            List<String> roles = userMapper.getUserRoles(userId);
            boolean isAdmin = "admin".equalsIgnoreCase(user.getUsername());
            if (!isAdmin && roles != null) {
                for (String role : roles) {
                    if ("admin".equalsIgnoreCase(role)) {
                        isAdmin = true;
                        break;
                    }
                }
            }

            if (isAdmin) {
                mergedPermissions.addAll(ALL_PERMISSIONS);
            }

            return new ArrayList<>(mergedPermissions);
        } catch (Exception e) {
            // 安全原则：权限加载失败时拒绝所有权限（fail-closed），而非授予所有权限（fail-open）
            // 致命Bug修复：原代码返回 ALL_PERMISSIONS 会让任意用户在数据库异常时获得管理员权限
            logger.error("[安全告警] 获取用户权限失败，已拒绝所有权限: userId={}, username={}, error={}",
                    user.getId(), user.getUsername(), e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
