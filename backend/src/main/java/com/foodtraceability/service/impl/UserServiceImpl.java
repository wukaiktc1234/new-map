package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.PermissionVerifyService;
import com.foodtraceability.service.RoleService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.service.UserDataService;
import com.foodtraceability.service.StoreDataService;
import com.foodtraceability.dto.StoreBasicInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 提供用户相关的业务逻辑实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    /** Sprint 2 招聘链路角色编码常量（大写，与 roles.role_code 一致） */
    private static final String ROLE_STORE_MANAGER = "STORE_MANAGER";
    private static final String ROLE_HR_RECRUITER = "HR_RECRUITER";
    private static final String ROLE_HR_MANAGER = "HR_MANAGER";

    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserDataService userDataService;
    private final StoreDataService storeDataService;
    private final ObjectMapper objectMapper;

    public UserServiceImpl(PasswordEncoder passwordEncoder, RoleService roleService, 
                          UserDataService userDataService, StoreDataService storeDataService) {
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.userDataService = userDataService;
        this.storeDataService = storeDataService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public IPage<User> getUserPage(Page<User> page, String username, String fullName, String email,
                                   String phone, String role, String status, String department,
                                   String startTime, String endTime) {
        return baseMapper.findUsersByConditionPage(page, username, fullName, email, phone,
                role, status, department,
                startTime != null ? LocalDateTime.parse(startTime) : null,
                endTime != null ? LocalDateTime.parse(endTime) : null);
    }

    @Override
    public User getUserByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public User getUserByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
    }

    @Override
    public User getUserByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        user.setDeleted(0);
        save(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user) {
        user.setUpdatedTime(LocalDateTime.now());
        updateById(user);
        // 清除用户缓存
        userDataService.clearUserCache(user.getId().toString());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        boolean result = removeById(userId);
        // 清除用户缓存
        if (result) {
            userDataService.clearUserCache(userId.toString());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        boolean result = updateById(user);
        // 清除用户缓存
        if (result) {
            userDataService.clearUserCache(userId.toString());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleUserStatus(Long userId, Integer status) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setStatus(status);
        user.setUpdatedTime(LocalDateTime.now());
        boolean result = updateById(user);
        // 清除用户缓存
        if (result) {
            userDataService.clearUserCache(userId.toString());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignStore(Long userId, Long storeId) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setStoreId(storeId);
        user.setUpdatedTime(LocalDateTime.now());
        boolean result = updateById(user);
        // 清除用户缓存
        if (result) {
            userDataService.clearUserCache(userId.toString());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unassignStore(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        
        int result = baseMapper.unassignStore(userId);
        if (result > 0) {
            userDataService.clearUserCache(userId.toString());
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> getUserStoreInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return null;
        }
        Map<String, Object> storeInfo = new HashMap<>();
        storeInfo.put("storeId", user.getStoreId());
        
        if (user.getStoreId() != null) {
            try {
                StoreBasicInfo storeBasicInfo = storeDataService.getStoreBasicInfo(String.valueOf(user.getStoreId()));
                if (storeBasicInfo != null) {
                    storeInfo.put("storeName", storeBasicInfo.getStoreName());
                } else {
                    storeInfo.put("storeName", null);
                }
            } catch (Exception e) {
                storeInfo.put("storeName", null);
            }
        } else {
            storeInfo.put("storeName", null);
        }
        
        return storeInfo;
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return baseMapper.existsByUsername(username);
    }

    @Override
    public boolean checkEmailExists(String email) {
        return baseMapper.existsByEmail(email);
    }

    @Override
    public boolean checkPhoneExists(String phone) {
        return baseMapper.existsByPhone(phone);
    }

    @Override
    public long countUsers() {
        return baseMapper.countUsers();
    }

    @Override
    public List<Map<String, Object>> countByStatus() {
        return baseMapper.countByStatus().stream()
                .map(statusCount -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("status", statusCount.getStatus());
                    map.put("count", statusCount.getCount());
                    return map;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> countByRole() {
        return baseMapper.countByRole().stream()
                .map(roleCount -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", roleCount.getRole());
                    map.put("count", roleCount.getCount());
                    return map;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> countByDepartment() {
        return baseMapper.countByDepartment().stream()
                .map(departmentCount -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("department", departmentCount.getDepartment());
                    map.put("count", departmentCount.getCount());
                    return map;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoles(Long userId, List<String> roleIds) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }

        try {
            String rolesJson = objectMapper.writeValueAsString(roleIds);
            user.setRoles(rolesJson);
            user.setUpdatedTime(LocalDateTime.now());
            boolean result = updateById(user);
            // 清除用户缓存
            if (result) {
                userDataService.clearUserCache(userId.toString());
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("角色序列化失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unassignRoles(Long userId) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setRoles(null);
        user.setUpdatedTime(LocalDateTime.now());
        boolean result = updateById(user);
        // 清除用户缓存
        if (result) {
            userDataService.clearUserCache(userId.toString());
        }
        return result;
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        User user = getById(userId);
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of();
        }

        String rolesStr = user.getRoles().trim();
        try {
            return objectMapper.readValue(rolesStr, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 容错：roles 字段不是 JSON 数组格式时
            // 仅当为纯数字字符串时按单个角色 ID 处理，避免 role_code 误当 role_id
            if (rolesStr.startsWith("[")) {
                logger.warn("角色JSON数组解析失败: userId={}, roles={}", userId, rolesStr, e);
                return List.of();
            }
            logger.warn("roles 字段非 JSON 数组格式: userId={}, roles={}", userId, rolesStr);
            if (rolesStr.matches("\\d+")) {
                return List.of(rolesStr);
            }
            return List.of();
        }
    }

    @Override
    public List<String> getUserRoleNames(Long userId) {
        List<String> roleIds = getUserRoles(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        return roleIds.stream()
                .map(roleId -> {
                    Role role = roleService.getRoleById(Long.valueOf(roleId));
                    return role != null ? role.getRoleName() : null;
                })
                .filter(roleName -> roleName != null)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasRole(Long userId, String roleCode) {
        List<String> roleIds = getUserRoles(userId);
        if (roleIds.isEmpty()) {
            return false;
        }

        return roleIds.stream()
                .map(roleId -> roleService.getRoleById(Long.valueOf(roleId)))
                .filter((Role role) -> role != null)
                .anyMatch(role -> roleCode.equals(role.getRoleCode()));
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        List<String> roleIds = getUserRoles(userId);
        if (roleIds.isEmpty()) {
            return false;
        }

        return roleIds.stream()
                .map(roleId -> roleService.getRolePermissions(Long.valueOf(roleId)))
                .anyMatch(permissions -> permissions.contains(permissionCode));
    }

    // ============ Sprint 2 招聘链路事件接收人查询实现 ============

    @Override
    public List<Long> getStoreManagersByStoreId(Long storeId) {
        if (storeId == null) {
            return List.of();
        }
        // users.store_id 为 VARCHAR，将 Long 转字符串查询
        List<Long> userIds = baseMapper.findUserIdsByStoreIdAndRoleCode(
                String.valueOf(storeId), ROLE_STORE_MANAGER);
        return userIds != null ? userIds : List.of();
    }

    @Override
    public List<Long> getHrRecruiters() {
        List<Long> userIds = baseMapper.findUserIdsByRoleCode(ROLE_HR_RECRUITER);
        return userIds != null ? userIds : List.of();
    }

    @Override
    public List<Long> getHrManagers() {
        List<Long> userIds = baseMapper.findUserIdsByRoleCode(ROLE_HR_MANAGER);
        return userIds != null ? userIds : List.of();
    }
}
