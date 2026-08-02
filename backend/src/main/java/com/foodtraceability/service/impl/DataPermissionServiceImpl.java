package com.foodtraceability.service.impl;

import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.Store;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.DepartmentMapper;
import com.foodtraceability.mapper.RoleDepartmentMapper;
import com.foodtraceability.mapper.RoleMapper;
import com.foodtraceability.mapper.RoleStoreMapper;
import com.foodtraceability.mapper.StoreMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.PermissionVerifyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据权限服务实现类
 * 处理数据权限过滤逻辑，支持门店、部门等多种数据权限范围
 *
 * 安全特性说明：
 * ✅ 使用参数化条件对象（DataScopeCondition），彻底避免SQL注入
 * ✅ 所有用户输入都经过严格的白名单验证
 * ✅ 采用安全失败模式（Fail-Safe），异常时拒绝访问
 * ✅ 已移除所有不安全的SQL字符串拼接操作
 *
 * @version 2.0 安全增强版本
 */
@Service
public class DataPermissionServiceImpl implements DataPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(DataPermissionServiceImpl.class);


    public DataPermissionServiceImpl(UserMapper userMapper, RoleMapper roleMapper, RoleStoreMapper roleStoreMapper, RoleDepartmentMapper roleDepartmentMapper, ObjectMapper objectMapper, PermissionVerifyService permissionVerifyService, StoreMapper storeMapper, DepartmentMapper departmentMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.roleStoreMapper = roleStoreMapper;
        this.roleDepartmentMapper = roleDepartmentMapper;
        this.objectMapper = objectMapper;
        this.permissionVerifyService = permissionVerifyService;
        this.storeMapper = storeMapper;
        this.departmentMapper = departmentMapper;
    }

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final RoleStoreMapper roleStoreMapper;

    private final RoleDepartmentMapper roleDepartmentMapper;

    private final ObjectMapper objectMapper;

    private final PermissionVerifyService permissionVerifyService;

    private final StoreMapper storeMapper;

    private final DepartmentMapper departmentMapper;

    /**
     * 数据权限条件对象（参数化查询容器）
     *
     * 安全设计原则：
     * ✅ 不直接构建SQL字符串，仅存储参数化数据
     * ✅ 严格的白名单输入验证
     * ✅ 条件类型限定为预定义的安全枚举值
     * ✅ 所有setter方法都经过安全校验
     *
     * 使用示例：
     * DataScopeCondition condition = new DataScopeCondition("STORE", "t");
     * condition.setSingleValue("store_001"); // 经过验证
     */
    public static class DataScopeCondition {
        /** 允许的条件类型白名单（预定义安全枚举） */
        private static final Set<String> ALLOWED_TYPES = Set.of(
            "ALL", "COMPANY", "REGION", "STORE", "DEPARTMENT",
            "STORES", "DEPARTMENTS", "SELF", "DENY"
        );

        /** 输入最大长度限制（防止超长字符串攻击） */
        private static final int MAX_INPUT_LENGTH = 64;

        /** 安全字符白名单正则：只允许字母、数字、下划线、连字符 */
        private static final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]{1," + MAX_INPUT_LENGTH + "}$");

        /** 条件类型 */
        private String type;
        /** 字段名前缀（表别名） */
        private String fieldPrefix;
        /** 参数值列表（用于IN查询） */
        private List<String> values;
        /** 单个参数值（用于等值查询） */
        private String singleValue;

        /**
         * 构造函数
         * @param type 条件类型（必须是ALLOWED_TYPES中的值）
         * @param fieldPrefix 表别名前缀
         * @throws IllegalArgumentException 如果type不在白名单中
         */
        public DataScopeCondition(String type, String fieldPrefix) {
            // 验证条件类型
            if (type != null && !ALLOWED_TYPES.contains(type.toUpperCase())) {
                logger.warn("非法的数据权限条件类型: {}", type);
                throw new IllegalArgumentException("不允许的条件类型: " + type);
            }
            this.type = type != null ? type.toUpperCase() : null;
            this.fieldPrefix = validateFieldPrefix(fieldPrefix);
            this.values = new ArrayList<>();
        }

        /**
         * 验证并清理字段前缀
         * 只允许字母、数字、下划线
         */
        private String validateFieldPrefix(String prefix) {
            if (prefix == null || prefix.trim().isEmpty()) {
                return "";
            }
            // 字段前缀只允许安全的SQL标识符字符
            if (!prefix.matches("^[a-zA-Z0-9_]*$")) {
                logger.warn("非法的字段前缀: {}", prefix);
                throw new IllegalArgumentException("字段前缀包含非法字符");
            }
            return prefix;
        }

        // Getter方法
        public String getType() { return type; }
        public String getFieldPrefix() { return fieldPrefix; }
        public List<String> getValues() { return Collections.unmodifiableList(values); }
        public String getSingleValue() { return singleValue; }

        /**
         * 设置条件类型（经过白名单验证）
         * @param type 条件类型（必须是ALLOWED_TYPES中的值）
         * @throws IllegalArgumentException 如果type不在白名单中
         */
        public void setType(String type) {
            if (type != null && !ALLOWED_TYPES.contains(type.toUpperCase())) {
                logger.warn("非法的数据权限条件类型: {}", type);
                throw new IllegalArgumentException("不允许的条件类型: " + type);
            }
            this.type = type != null ? type.toUpperCase() : null;
        }

        /**
         * 设置单个参数值（经过严格安全验证）
         * @param value 参数值（必须符合白名单规则）
         * @throws IllegalArgumentException 如果输入不合法
         */
        public void setSingleValue(String value) {
            validateAndSanitizeInput(value, "singleValue");
            this.singleValue = value;
        }

        /**
         * 添加参数值到列表（经过严格安全验证）
         * @param value 参数值（必须符合白名单规则）
         * @throws IllegalArgumentException 如果输入不合法
         */
        public void addValue(String value) {
            validateAndSanitizeInput(value, "value");
            this.values.add(value);
        }

        /**
         * 通用输入验证和清理方法
         *
         * 安全策略（白名单模式）：
         * - 只允许字母、数字、下划线、连字符
         * - 限制最大长度为64个字符
         * - 拒绝空值或空白字符串
         *
         * @param input 待验证的输入
         * @param fieldName 字段名称（用于错误日志）
         * @throws IllegalArgumentException 如果输入不符合安全要求
         */
        private void validateAndSanitizeInput(String input, String fieldName) {
            // 空值检查
            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + "不能为空");
            }

            // 长度检查（防止DoS攻击）
            if (input.length() > MAX_INPUT_LENGTH) {
                logger.warn("输入超过最大长度限制: field={}, length={}, max={}",
                    fieldName, input.length(), MAX_INPUT_LENGTH);
                throw new IllegalArgumentException(fieldName + "超过最大长度限制");
            }

            // 白名单字符验证（核心安全检查）
            if (!SAFE_INPUT_PATTERN.matcher(input).matches()) {
                logger.warn("检测到非法输入字符: field={}, value={}", input);
                throw new IllegalArgumentException(fieldName + "包含非法字符（只允许字母、数字、下划线和连字符）");
            }
        }

        /**
         * 检查条件是否表示拒绝访问
         */
        public boolean isDenyCondition() {
            return "DENY".equals(type);
        }

        /**
         * 检查条件是否有效（非null且非DENY）
         */
        public boolean isValidCondition() {
            return type != null && !"DENY".equals(type);
        }
    }

    /**
     * 构建数据权限条件（安全方法）
     * 返回参数化的条件对象，彻底避免SQL注入风险
     *
     * 安全机制：
     * - 使用DataScopeCondition对象封装参数，不直接拼接SQL
     * - 所有输入值通过白名单验证
     * - 条件类型严格限定为预定义枚举值
     *
     * @param userId 用户ID（必须经过身份验证）
     * @param tableName 表名（保留兼容性，当前未使用）
     * @param alias 表别名（用于字段前缀）
     * @return DataScopeCondition 参数化条件对象，null表示无限制
     */
    @Override
    public DataScopeCondition buildDataScopeCondition(String userId, String tableName, String alias) {
        if (userId == null) {
            return null;
        }

        try {
            // 检查是否为管理员
            if (permissionVerifyService.isAdmin(userId)) {
                return null; // 管理员无限制
            }

            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                // 用户不存在，返回拒绝访问的条件
                DataScopeCondition condition = new DataScopeCondition("DENY", alias);
                return condition;
            }

            String dataScope = permissionVerifyService.getUserDataScope(userId);
            String prefix = (alias != null && !alias.isEmpty()) ? alias + "." : "";
            DataScopeCondition condition = new DataScopeCondition(dataScope, prefix);

            switch (dataScope) {
                case "all":
                    // 全部数据权限，无限制
                    return null;

                case "company":
                    // 本公司数据权限
                    String companyId = user.getCompanyId();
                    if (companyId != null) {
                        condition.setSingleValue(companyId);
                        condition.setType("COMPANY");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "region":
                    // 本区域数据权限
                    String userRegion = getUserRegion(userId);
                    if (userRegion != null) {
                        condition.setSingleValue(userRegion);
                        condition.setType("REGION");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "store":
                    // 本门店数据权限
                    String storeId = user.getStoreId() != null ? String.valueOf(user.getStoreId()) : null;
                    if (storeId != null) {
                        condition.setSingleValue(storeId);
                        condition.setType("STORE");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "department":
                    // 本部门数据权限
                    String deptId = user.getDepartmentId() != null ? String.valueOf(user.getDepartmentId()) : null;
                    if (deptId != null) {
                        condition.setSingleValue(deptId);
                        condition.setType("DEPARTMENT");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "stores":
                    // 指定门店数据权限
                    List<String> accessibleStoreIds = getRoleAccessibleStoreIds(userId);
                    if (!accessibleStoreIds.isEmpty()) {
                        for (String sid : accessibleStoreIds) {
                            condition.addValue(sid);
                        }
                        condition.setType("STORES");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "departments":
                    // 指定部门数据权限
                    List<String> accessibleDeptIds = getRoleAccessibleDepartmentIds(userId);
                    if (!accessibleDeptIds.isEmpty()) {
                        for (String did : accessibleDeptIds) {
                            condition.addValue(did);
                        }
                        condition.setType("DEPARTMENTS");
                    } else {
                        condition.setType("DENY");
                    }
                    return condition;

                case "self":
                    // 仅本人数据权限
                    condition.setSingleValue(userId);
                    condition.setType("SELF");
                    return condition;

                default:
                    // 未知的数据权限范围，拒绝访问
                    condition.setType("DENY");
                    return condition;
            }
        } catch (Exception e) {
            logger.error("构建数据权限条件失败: userId={}, tableName={}", userId, tableName, e);
            // 安全失败模式：返回拒绝条件
            DataScopeCondition errorCondition = new DataScopeCondition("DENY", alias);
            return errorCondition;
        }
    }

    @Override
    public List<String> getAccessibleStoreIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 防御性权限校验：管理员直接返回 null（表示不限制）
        // 与 buildDataScopeCondition 中的逻辑保持一致，防止因 roles JSON 异常
        // 导致 dataScope 降级为 "self" 时管理员权限被错误限制
        if (permissionVerifyService.isAdmin(userId)) {
            return null;
        }

        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                return Collections.emptyList();
            }

            String dataScope = permissionVerifyService.getUserDataScope(userId);

            switch (dataScope) {
                case "all":
                case "company":
                case "region":
                    // 全部/公司/区域权限，返回所有可访问门店
                    return getAllStoreIds(userId);

                case "store":
                    // 本门店权限
                    String storeId = user.getStoreId() != null ? String.valueOf(user.getStoreId()) : null;
                    return storeId != null ? Collections.singletonList(storeId) : Collections.emptyList();

                case "stores":
                    // 指定门店权限
                    return getRoleAccessibleStoreIds(userId);

                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("获取可访问门店失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getAccessibleDepartmentIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                return Collections.emptyList();
            }

            String dataScope = permissionVerifyService.getUserDataScope(userId);

            switch (dataScope) {
                case "all":
                case "company":
                    // 全部/公司权限，返回所有可访问部门
                    return getAllDepartmentIds(userId);

                case "department":
                    // 本部门权限
                    String deptId = user.getDepartmentId() != null ? String.valueOf(user.getDepartmentId()) : null;
                    return deptId != null ? Collections.singletonList(deptId) : Collections.emptyList();

                case "departments":
                    // 指定部门权限
                    return getRoleAccessibleDepartmentIds(userId);

                default:
                    return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("获取可访问部门失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean checkDataAccess(String userId, String tableName, String dataId) {
        if (userId == null || dataId == null) {
            return false;
        }

        try {
            // 管理员有全部权限
            if (permissionVerifyService.isAdmin(userId)) {
                return true;
            }

            // 根据表名和数据ID进行具体权限检查
            // 这里可以根据实际业务需求扩展
            return true;
        } catch (Exception e) {
            logger.error("检查数据访问权限失败: userId={}, tableName={}, dataId={}", userId, tableName, dataId, e);
            return false;
        }
    }

    @Override
    public String getUserDataScope(String userId) {
        return permissionVerifyService.getUserDataScope(userId);
    }

    @Override
    public boolean canAccessStore(String userId, String storeId) {
        if (userId == null || storeId == null) {
            return false;
        }

        // 管理员有全部权限
        if (permissionVerifyService.isAdmin(userId)) {
            return true;
        }

        List<String> accessibleStoreIds = getAccessibleStoreIds(userId);
        return accessibleStoreIds.contains(storeId);
    }

    @Override
    public boolean canAccessDepartment(String userId, String departmentId) {
        if (userId == null || departmentId == null) {
            return false;
        }

        // 管理员有全部权限
        if (permissionVerifyService.isAdmin(userId)) {
            return true;
        }

        List<String> accessibleDeptIds = getAccessibleDepartmentIds(userId);
        return accessibleDeptIds.contains(departmentId);
    }

    @Override
    public List<String> getRoleAccessibleStoreIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                return Collections.emptyList();
            }

            String rolesJson = user.getRoles();
            if (rolesJson == null || rolesJson.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> roleIds = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});

            // ✅ 性能优化：批量查询替代循环查询（解决N+1问题）
            // 原实现在循环中对每个角色ID执行一次数据库查询
            // 新实现一次性查询所有角色的门店ID，减少数据库交互次数
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 批量查询所有角色关联的门店ID
            List<String> allStoreIds = roleStoreMapper.selectStoreIdsByRoleIds(roleIds);

            // 去重并返回
            return new ArrayList<>(new LinkedHashSet<>(allStoreIds));
        } catch (Exception e) {
            logger.error("获取角色可访问门店失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getRoleAccessibleDepartmentIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null) {
                return Collections.emptyList();
            }

            String rolesJson = user.getRoles();
            if (rolesJson == null || rolesJson.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> roleIds = objectMapper.readValue(rolesJson, new TypeReference<List<String>>() {});

            // ✅ 性能优化：批量查询替代循环查询（解决N+1问题）
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 批量查询所有角色关联的部门ID
            List<String> allDeptIds = roleDepartmentMapper.selectDepartmentIdsByRoleIds(roleIds);

            // 去重并返回
            return new ArrayList<>(new LinkedHashSet<>(allDeptIds));
        } catch (Exception e) {
            logger.error("获取角色可访问部门失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户所在区域
     * 当前用户表无 region 字段，返回 null 表示不走 region 分支
     * 若后续用户表新增区域字段，可在此处实现区域查询逻辑
     */
    private String getUserRegion(String userId) {
        try {
            User user = userMapper.selectById(Long.parseLong(userId));
            if (user == null || user.getStoreId() == null) {
                return null;
            }

            // 当前用户无区域概念，返回 null 表示不走 region 分支
            // 若后续需要区域权限，可通过门店表的 region 字段或用户表的 region 字段实现
            return null;
        } catch (Exception e) {
            logger.error("获取用户区域失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 获取所有活跃门店ID列表
     * 用于 ALL/COMPANY/REGION 数据权限范围时返回全部门店
     * @param userId 用户ID（保留参数，用于未来按用户权限范围过滤）
     * @return 活跃门店ID列表
     */
    private List<String> getAllStoreIds(String userId) {
        try {
            List<Store> activeStores = storeMapper.selectActiveStores();
            if (activeStores == null || activeStores.isEmpty()) {
                return Collections.emptyList();
            }
            return activeStores.stream()
                .map(Store::getStoreId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取所有门店ID失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有部门ID列表
     * 用于 ALL/COMPANY 数据权限范围时返回全部部门
     * @param userId 用户ID（保留参数，用于未来按用户权限范围过滤）
     * @return 部门ID列表
     */
    private List<String> getAllDepartmentIds(String userId) {
        try {
            List<Department> allDepartments = departmentMapper.getAllDepartments();
            if (allDepartments == null || allDepartments.isEmpty()) {
                return Collections.emptyList();
            }
            return allDepartments.stream()
                .map(Department::getDepartmentId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取所有部门ID失败: userId={}", userId, e);
            return Collections.emptyList();
        }
    }
}
