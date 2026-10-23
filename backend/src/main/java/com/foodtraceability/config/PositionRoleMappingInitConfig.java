package com.foodtraceability.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.PositionRoleMapping;
import com.foodtraceability.entity.Role;
import com.foodtraceability.mapper.PositionRoleMappingMapper;
import com.foodtraceability.mapper.RoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 职位-角色映射初始化配置
 */
@Component
public class PositionRoleMappingInitConfig {

    private static final Logger log = LoggerFactory.getLogger(PositionRoleMappingInitConfig.class);


    public PositionRoleMappingInitConfig(JdbcTemplate jdbcTemplate, RoleMapper roleMapper, PositionRoleMappingMapper positionRoleMappingMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.roleMapper = roleMapper;
        this.positionRoleMappingMapper = positionRoleMappingMapper;
    }

    private final JdbcTemplate jdbcTemplate;

    private final RoleMapper roleMapper;

    private final PositionRoleMappingMapper positionRoleMappingMapper;

    @PostConstruct
    public void init() {
        initPositions();
        initPositionRoleMappings();
    }

    /**
     * 初始化默认职位数据（必要种子数据）
     */
    private void initPositions() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM positions WHERE deleted = 0", Integer.class);
            
            if (count != null && count > 0) {
                log.info("职位数据已存在，跳过初始化");
                return;
            }

            jdbcTemplate.update(
                "INSERT INTO positions (position_code, position_name, level, sort_order, description, status, created_at, updated_at, deleted) VALUES " +
                "('STORE_MANAGER', '店长', 'manager', 10, '门店负责人，管理门店日常运营', 1, NOW(), NOW(), 0)," +
                "('DEPT_MANAGER', '部门经理', 'manager', 20, '部门负责人，管理部门业务', 1, NOW(), NOW(), 0)," +
                "('PURCHASE_MANAGER', '采购经理', 'manager', 30, '负责采购业务管理', 1, NOW(), NOW(), 0)," +
                "('SUPER_ADMIN', '超级管理员', 'admin', 1, '系统超级管理员', 1, NOW(), NOW(), 0)," +
                "('STORE_CLERK', '店员', 'staff', 100, '门店普通员工', 1, NOW(), NOW(), 0)," +
                "('PURCHASE_STAFF', '采购员', 'staff', 110, '采购部门员工', 1, NOW(), NOW(), 0)"
            );
            log.info("职位数据初始化完成");
        } catch (Exception e) {
            log.warn("职位数据初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 初始化职位-角色映射（必要种子数据）
     */
    private void initPositionRoleMappings() {
        try {
            Long mappingCount = positionRoleMappingMapper.selectCount(
                new LambdaQueryWrapper<PositionRoleMapping>().eq(PositionRoleMapping::getDeleted, 0));
            
            if (mappingCount != null && mappingCount > 0) {
                log.info("职位-角色映射数据已存在，跳过初始化");
                return;
            }

            List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().eq(Role::getDeleted, 0));
            
            Long storeManagerRoleId = null;
            Long deptManagerRoleId = null;
            Long superAdminRoleId = null;
            
            for (Role role : roles) {
                if ("D".equals(role.getRoleCode())) {
                    storeManagerRoleId = role.getId();
                } else if ("J".equals(role.getRoleCode())) {
                    deptManagerRoleId = role.getId();
                } else if ("Z".equals(role.getRoleCode())) {
                    superAdminRoleId = role.getId();
                }
            }

            final Long finalStoreManagerRoleId = storeManagerRoleId;
            final Long finalDeptManagerRoleId = deptManagerRoleId;
            final Long finalSuperAdminRoleId = superAdminRoleId;

            Long storeManagerPositionId = getPositionId("STORE_MANAGER");
            Long deptManagerPositionId = getPositionId("DEPT_MANAGER");
            Long purchaseManagerPositionId = getPositionId("PURCHASE_MANAGER");
            Long superAdminPositionId = getPositionId("SUPER_ADMIN");

            if (storeManagerPositionId != null && finalStoreManagerRoleId != null) {
                createMapping(storeManagerPositionId, finalStoreManagerRoleId, true, 10, "店长职位自动关联店长角色");
            }

            if (deptManagerPositionId != null && finalDeptManagerRoleId != null) {
                createMapping(deptManagerPositionId, finalDeptManagerRoleId, true, 10, "部门经理职位自动关联经理角色");
            }

            if (purchaseManagerPositionId != null && finalDeptManagerRoleId != null) {
                createMapping(purchaseManagerPositionId, finalDeptManagerRoleId, true, 10, "采购经理职位自动关联经理角色");
            }

            if (superAdminPositionId != null && finalSuperAdminRoleId != null) {
                createMapping(superAdminPositionId, finalSuperAdminRoleId, true, 1, "超级管理员职位自动关联超级管理员角色");
            }

            log.info("职位-角色映射数据初始化完成");
        } catch (Exception e) {
            log.warn("职位-角色映射数据初始化失败: {}", e.getMessage());
        }
    }

    private Long getPositionId(String positionCode) {
        try {
            List<Long> ids = jdbcTemplate.queryForList(
                "SELECT position_id FROM positions WHERE position_code = ? AND deleted = 0",
                Long.class,
                positionCode);
            if (ids.isEmpty()) {
                // 职位编码在活库中不存在属数据口径漂移，不是异常：跳过该映射即可
                log.warn("职位编码不存在，跳过职位-角色映射: positionCode={}", positionCode);
                return null;
            }
            return ids.get(0);
        } catch (Exception e) {
            // 原来只 warn 且不带异常，职位-角色映射会静默缺失；补全异常与后果说明。
            log.error("获取职位ID失败，该职位-角色映射将被跳过（权限自动分配可能失效）: positionCode={}, error={}",
                    positionCode, e.getMessage(), e);
            return null;
        }
    }

    private void createMapping(Long positionId, Long roleId, boolean isPrimary, int priority, String description) {
        PositionRoleMapping mapping = new PositionRoleMapping();
        mapping.setPositionId(positionId);
        mapping.setRoleId(roleId);
        mapping.setIsPrimary(isPrimary ? 1 : 0);
        mapping.setPriority(priority);
        mapping.setDescription(description);
        mapping.setStatus(1);
        positionRoleMappingMapper.insert(mapping);
    }
}
