package com.foodtraceability.controller;

import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.dto.EmployeeAssignLocationDTO;
import com.foodtraceability.security.annotation.OperationLog;
import com.foodtraceability.security.constants.AdminPermissions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * C3 端点注解与权限码五面逐字一致性测试（p1-user-location-002-design-001.md D3-3/D3-5）。
 *
 * <p>权限码 {@code hr:employee:assign-location} 必须在以下五面逐字一致：
 * <ol>
 *   <li>{@link EmployeeController#assignLocation} 的 {@code @RequiresPermission} + {@code @PreAuthorize}；</li>
 *   <li>{@link AdminPermissions#ALL}（admin 通配展开面）；</li>
 *   <li>{@code db/init-data.sql} 的 permissions 行 + role_permissions 绑定；</li>
 *   <li>Flyway 种子迁移 {@code V*__seed_hr_employee_assign_location.sql}（含 U3 补齐的
 *       system:user:assign-store / unassign-store）；</li>
 *   <li>{@code frontend/src/utils/permissions.ts} 白名单。</li>
 * </ol>
 *
 * <p>命名事实（§115）：旧码 hr:employee:edit（前端）/ hr:employee:update（DB）已分叉，
 * 新码不得沿用任一分叉命名——本测试逐字断言新码，防止再次漂移。
 */
@DisplayName("C3 端点注解 + 权限码五面逐字一致（D3-3/D3-5）")
class EmployeeAssignLocationEndpointTest {

    private static final String CODE = "hr:employee:assign-location";
    private static final String MODULE = "EMPLOYEE_LOCATION_ASSIGN";

    @Test
    @DisplayName("端点方法：PUT /{id}/assign-location + 权限/审计注解齐全且值正确")
    void endpointCarriesPermissionAndAuditAnnotations() throws Exception {
        Method m = EmployeeController.class.getMethod("assignLocation", String.class, EmployeeAssignLocationDTO.class);

        // 路径面
        assertTrue(m.isAnnotationPresent(PutMapping.class), "必须是 PUT 方法");
        PutMapping put = m.getAnnotation(PutMapping.class);
        assertNotNull(put.value());
        assertEquals("/{id}/assign-location", put.value()[0],
                "类级 /v1/employees + 方法级 /{id}/assign-location 构成 C3 端点");

        // 权限面（@RequiresPermission → PermissionAspect 运行时校验）
        RequiresPermission rp = m.getAnnotation(RequiresPermission.class);
        assertNotNull(rp, "@RequiresPermission 必须存在（运行时 403 校验入口）");
        assertEquals(CODE, rp.value(), "权限码必须逐字一致，不得沿用 edit/update 分叉命名");
        assertEquals("assign-location", rp.action());

        // 权限面（@PreAuthorize → Spring Security 声明式校验）
        PreAuthorize pa = m.getAnnotation(PreAuthorize.class);
        assertNotNull(pa, "@PreAuthorize 必须存在");
        assertTrue(pa.value().contains("hasAuthority('" + CODE + "')"),
                "@PreAuthorize 必须含 hasAuthority('" + CODE + "')，实际: " + pa.value());
        assertTrue(pa.value().contains("hasAuthority('*')"),
                "admin 必须经通配符放行，实际: " + pa.value());

        // 审计面（@OperationLog → OperationLogAspect 静态行；动态旧值行由服务级补记）
        OperationLog ol = m.getAnnotation(OperationLog.class);
        assertNotNull(ol, "@OperationLog 必须存在（D3-4 审计留痕）");
        assertEquals(MODULE, ol.module(), "审计模块必须为 EMPLOYEE_LOCATION_ASSIGN（A4 验收 grep 点）");
        assertEquals(OperationLog.OperationType.UPDATE, ol.type(), "归属变更是 UPDATE 语义");
    }

    @Test
    @DisplayName("DTO：locationId @NotNull（controller 层校验）")
    void dtoRequiresLocationId() throws Exception {
        java.lang.reflect.Field locationIdField = EmployeeAssignLocationDTO.class.getDeclaredField("locationId");
        NotNull notNull = locationIdField.getAnnotation(NotNull.class);
        assertNotNull(notNull, "locationId 必须 @NotNull：null 归属不是合法请求，应 400");
    }

    @Test
    @DisplayName("admin 通配展开面：AdminPermissions.ALL 含新码")
    void adminWildcardContainsNewCode() {
        assertTrue(AdminPermissions.ALL.contains(CODE),
                "AdminPermissions.ALL 必须含 " + CODE + "（admin 展开后 @PreAuthorize 放行）");
    }

    @Test
    @DisplayName("五面逐字一致：init-data.sql / Flyway 种子迁移 / frontend permissions.ts")
    void permissionCodeVerbatimAcrossFileSurfaces() throws Exception {
        Path repoRoot = findRepoRoot();

        // 面3：init-data.sql（permissions 行 + ROLE_HR_DIRECTOR 绑定）
        Path initData = repoRoot.resolve("backend").resolve("src").resolve("main")
                .resolve("resources").resolve("db").resolve("init-data.sql");
        String initDataSql = Files.readString(initData);
        assertTrue(initDataSql.contains("'PERM_HR_EMP_ASSIGN_LOCATION'"),
                "init-data.sql 必须含 PERM_HR_EMP_ASSIGN_LOCATION 权限行");
        assertTrue(initDataSql.contains("'hr:employee:assign-location'"),
                "init-data.sql 权限码必须逐字一致");
        assertTrue(initDataSql.contains("'ROLE_HR_DIRECTOR'") && initDataSql.contains("'PERM_HR_EMP_ASSIGN_LOCATION'"),
                "init-data.sql 必须含 ROLE_HR_DIRECTOR 绑定（U7）");

        // 面4：Flyway 种子迁移（U3：三码一迁；U7：仅绑 ROLE_HR_DIRECTOR）
        Path migrationDir = repoRoot.resolve("backend").resolve("src").resolve("main")
                .resolve("resources").resolve("db").resolve("migration");
        Path migration;
        try (Stream<Path> stream = Files.list(migrationDir)) {
            migration = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("V") && name.contains("__seed_hr_employee_assign_location.sql");
                    })
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("缺少种子迁移 V*__seed_hr_employee_assign_location.sql"));
        }
        String migrationSql = Files.readString(migration);
        assertTrue(migrationSql.contains("'hr:employee:assign-location'"), "迁移必须种 hr:employee:assign-location");
        assertTrue(migrationSql.contains("'system:user:assign-store'"), "U3：迁移必须补 system:user:assign-store（此前 0 种子）");
        assertTrue(migrationSql.contains("'system:user:unassign-store'"), "U3：迁移必须补 system:user:unassign-store（此前 0 种子）");
        assertTrue(migrationSql.contains("'ROLE_HR_DIRECTOR'"), "U7：迁移必须绑定 ROLE_HR_DIRECTOR");
        assertTrue(!migrationSql.contains("'ROLE_STORE_MANAGER'"),
                "U7 裁定：不得绑定 ROLE_STORE_MANAGER");

        // 面5：frontend permissions.ts 白名单
        Path frontendPermissions = repoRoot.resolve("frontend").resolve("src").resolve("utils").resolve("permissions.ts");
        String frontendPermissionsTs = Files.readString(frontendPermissions);
        assertTrue(frontendPermissionsTs.contains(CODE),
                "frontend permissions.ts 白名单必须逐字含 " + CODE);
    }

    /**
     * 定位仓库根目录（含 frontend/ 与 backend/ 的公共父目录）。
     * surefire 的 user.dir 是 backend 模块目录，向上最多 4 级查找。
     */
    private static Path findRepoRoot() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 4 && dir != null; i++) {
            Path candidate = dir;
            if (Files.isDirectory(candidate.resolve("frontend").resolve("src"))
                    && Files.isDirectory(candidate.resolve("backend").resolve("src"))) {
                return candidate;
            }
            dir = dir.getParent();
        }
        fail("无法定位仓库根目录（frontend/ 与 backend/ 公共父目录），user.dir=" + System.getProperty("user.dir"));
        return null; // unreachable
    }
}
