package com.foodtraceability.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 权限数据初始化Runner
 * 在应用启动时初始化权限数据
 */
@Component
@Order(3)
public class PermissionInitRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PermissionInitRunner.class);

    public PermissionInitRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("PermissionInitRunner 开始执行");

        try {
            // 检查permissions表是否存在（PostgreSQL兼容）
            String checkTableSql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = current_schema() AND table_name = 'permissions'";
            List<String> tables = jdbcTemplate.queryForList(checkTableSql, String.class);
            
            if (tables.isEmpty()) {
                System.out.println("permissions表不存在，跳过权限数据初始化");
                return;
            }

            System.out.println("permissions表存在，检查数据...");

            // 检查是否已有权限数据
            String checkDataSql = "SELECT COUNT(*) FROM permissions WHERE deleted = 0";
            Integer count = jdbcTemplate.queryForObject(checkDataSql, Integer.class);
            
            System.out.println("当前permissions表中有 " + count + " 条数据");
            
            if (count != null && count > 0) {
                System.out.println("permissions表已有数据，跳过初始化");
                return;
            }

            System.out.println("开始初始化权限数据...");
            
            // 插入权限数据
            insertPermissions();
            
            // 为admin角色分配权限
            assignPermissionsToAdmin();
            
            System.out.println("权限数据初始化完成！");
            
        } catch (Exception e) {
            System.err.println("权限数据初始化失败: " + e.getMessage());
            e.printStackTrace();
        }

        log.info("PermissionInitRunner 执行结束");
    }

    private void insertPermissions() {
        // 不包含description字段的SQL
        List<String> insertSqls = Arrays.asList(
            // 系统管理模块 (parent_id = 0)
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system', '系统管理', 1, 'system', 0, 1, 1, 1, 'el-icon-setting', '/system', NULL, 0, 1, 0, 1, 1)",

            // 系统设置 (parent_id = 1)
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:settings', '系统设置', 1, 'system', 1, 2, 1, 1, 'el-icon-setting', '/system/settings', 'system/settings/index', 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:settings:view', '查看系统设置', 3, 'system', 2, 3, 1, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:settings:edit', '编辑系统设置', 3, 'system', 2, 3, 2, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",

            // 用户管理 (parent_id = 1)
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:user', '用户管理', 1, 'system', 1, 2, 2, 1, 'el-icon-user', '/system/user', 'system/user/index', 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:user:view', '查看用户', 3, 'system', 5, 3, 1, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:user:create', '创建用户', 3, 'system', 5, 3, 2, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:user:edit', '编辑用户', 3, 'system', 5, 3, 3, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:user:delete', '删除用户', 3, 'system', 5, 3, 4, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",

            // 角色管理 (parent_id = 1)
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role', '角色管理', 1, 'system', 1, 2, 3, 1, 'el-icon-s-custom', '/system/role', 'system/role/index', 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role:view', '查看角色', 3, 'system', 10, 3, 1, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role:create', '创建角色', 3, 'system', 10, 3, 2, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role:edit', '编辑角色', 3, 'system', 10, 3, 3, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role:delete', '删除角色', 3, 'system', 10, 3, 4, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:role:assign', '分配权限', 3, 'system', 10, 3, 5, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",

            // 权限管理 (parent_id = 1)
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:permission', '权限管理', 1, 'system', 1, 2, 4, 1, 'el-icon-s-check', '/system/permission', 'system/permission/index', 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:permission:view', '查看权限', 3, 'system', 16, 3, 1, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:permission:create', '创建权限', 3, 'system', 16, 3, 2, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:permission:edit', '编辑权限', 3, 'system', 16, 3, 3, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)",
            "INSERT INTO permissions (permission_code, permission_name, permission_type, module, parent_id, level, sort_order, status, icon, path, component, is_hidden, is_cache, deleted, created_by, updated_by) VALUES " +
            "('system:permission:delete', '删除权限', 3, 'system', 16, 3, 4, 1, NULL, NULL, NULL, 0, 1, 0, 1, 1)"
        );

        int successCount = 0;
        for (String sql : insertSqls) {
            try {
                jdbcTemplate.execute(sql);
                successCount++;
            } catch (Exception e) {
                System.err.println("执行SQL失败: " + sql);
                System.err.println("错误: " + e.getMessage());
            }
        }
        
        System.out.println("权限数据插入完成，成功插入 " + successCount + " 条记录");
    }

    private void assignPermissionsToAdmin() {
        try {
            // 检查role_permissions表是否存在（PostgreSQL兼容）
            String checkTableSql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = current_schema() AND table_name = 'role_permissions'";
            List<String> tables = jdbcTemplate.queryForList(checkTableSql, String.class);
            
            if (tables.isEmpty()) {
                System.out.println("role_permissions表不存在，跳过权限分配");
                return;
            }

            // 获取admin角色的ID
            String getAdminRoleSql = "SELECT id FROM roles WHERE role_code = 'admin' LIMIT 1";
            List<Long> adminRoleIds = jdbcTemplate.queryForList(getAdminRoleSql, Long.class);
            
            if (adminRoleIds.isEmpty()) {
                System.out.println("未找到admin角色，跳过权限分配");
                return;
            }
            
            Long adminRoleId = adminRoleIds.get(0);
            System.out.println("为admin角色(ID:" + adminRoleId + ")分配权限");

            // 为admin角色分配所有权限
            String assignSql = "INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at, created_by, updated_by) " +
                    "SELECT ?, id, NOW(), NOW(), 1, 1 FROM permissions WHERE deleted = 0 " +
                    "AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = ? AND rp.permission_id = permissions.id)";
            
            int count = jdbcTemplate.update(assignSql, adminRoleId, adminRoleId);
            System.out.println("已为admin角色分配 " + count + " 个权限");
            
        } catch (Exception e) {
            System.err.println("为admin角色分配权限失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
