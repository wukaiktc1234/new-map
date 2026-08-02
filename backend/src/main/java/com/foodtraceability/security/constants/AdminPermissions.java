package com.foodtraceability.security.constants;

import java.util.Set;

/**
 * 管理员权限常量类
 *
 * <p>设计目的：
 * <ul>
 *   <li>解决 JWT token 过大问题：admin 用户的 JWT token 只存储 "*" 通配符，
 *       不存储所有权限码，避免 token 超过 Tomcat 8KB header 限制。</li>
 *   <li>运行时权限展开：SecurityUser.getAuthorities() 检测到 "*" 时，
 *       展开为 AdminPermissions.ALL，供 @PreAuthorize 使用。</li>
 *   <li>前端权限返回：登录/获取用户信息接口返回完整权限列表给前端，
 *       保证前端权限检查（permissions.includes('xxx')）正常工作。</li>
 * </ul>
 *
 * <p>维护说明：新增模块权限时，请同步在 ALL 中添加对应权限码。
 */
public final class AdminPermissions {

    /** 管理员通配权限标识 */
    public static final String WILDCARD = "*";

    /**
     * 管理员拥有的所有权限码集合
     * 用于运行时展开 "*" 通配符，供 @PreAuthorize 注解检查。
     */
    public static final Set<String> ALL = Set.of(
        // 通配权限：admin 用户拥有此权限后，所有 @PreAuthorize("hasAuthority('xxx') or hasAuthority('*')") 自动放行
        "*",
        // === 基础权限（旧版） ===
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
        // 财务模块细粒度权限
        "finance:voucher:create", "finance:voucher:query", "finance:voucher:update", "finance:voucher:approve",
        "finance:payable:create", "finance:payable:query", "finance:payable:update", "finance:payable:delete", "finance:payable:approve",
        "finance:receivable:create", "finance:receivable:query", "finance:receivable:update", "finance:receivable:delete", "finance:receivable:approve",
        "finance:budget:create", "finance:budget:query", "finance:budget:update", "finance:budget:delete", "finance:budget:approve",
        "finance:cost:create", "finance:cost:query", "finance:cost:update", "finance:cost:delete", "finance:cost:approve",
        "finance:bank:create", "finance:bank:query", "finance:bank:update", "finance:bank:delete",
        "finance:period:create", "finance:period:query", "finance:period:update", "finance:period:approve",
        "finance:record:view", "finance:record:create", "finance:record:update", "finance:record:approve",
        "finance:invoice:view", "finance:invoice:create", "finance:invoice:update", "finance:invoice:delete",
        "finance:invoice:issue", "finance:invoice:void", "finance:invoice:red-flush", "finance:invoice:verify",
        "finance:warning:view", "finance:warning:create", "finance:warning:process", "finance:warning:delete",
        "finance:audit-log:view",
        "finance:approval:view", "finance:approval:approve",
        "finance:statistics:view",
        "finance:report:config", "finance:report:view",
        "finance:asset:view", "finance:asset:manage",
        "finance:auto-voucher:view", "finance:auto-voucher:create", "finance:auto-voucher:update", "finance:auto-voucher:delete", "finance:auto-voucher:export",
        "finance:approval-flow-config:view", "finance:approval-flow-config:manage",
        "finance:invoice-reimbursement:view", "finance:invoice-reimbursement:create", "finance:invoice-reimbursement:update", "finance:invoice-reimbursement:delete", "finance:invoice-reimbursement:approve", "finance:invoice-reimbursement:cancel", "finance:invoice-reimbursement:pay",
        // 报销模块（InvoiceReimbursementController 实际使用的权限码前缀）
        "finance:reimbursement:view", "finance:reimbursement:create", "finance:reimbursement:approve", "finance:reimbursement:pay",
        // 溯源模块
        "trace:create", "trace:query", "trace:update", "trace:delete", "trace:recall", "trace:export",
        // 采购模块
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
        // 仓储模块
        "warehouse:create", "warehouse:update", "warehouse:query", "warehouse:delete",
        "inventory:query", "inventory:create", "inventory:update", "inventory:delete", "inventory:approve", "inventory:execute", "inventory:lock", "inventory:deduct",
        // === 系统管理模块 ===
        "system:role:read", "system:role:create", "system:role:update", "system:role:delete",
        "system:user:read", "system:user:create", "system:user:update", "system:user:delete",
        "system:permission:read", "system:permission:create", "system:permission:update", "system:permission:delete",
        "system:config:read", "system:config:update",
        // 系统配置模块（SysConfigController 实际使用的权限码）
        "sys:config:query", "sys:config:edit",
        "system:aimodel:manage",
        // 系统字典模块（SysDictController 实际使用的权限码）
        "system:dict:query", "system:dict:create", "system:dict:update", "system:dict:delete",
        // === 审计/告警模块 ===
        "audit:log:query", "audit:log:export", "audit:log:clean", "audit:log:archive",
        "alert:query", "alert:edit", "alert:delete",
        // === 资产模块 ===
        "asset:view", "asset:manage", "asset:delete",
        // === HR 模块 ===
        "hr:approval:view", "hr:approval:manage",
        "hr:contract-template:view", "hr:contract-template:manage",
        "hr:attendance:view",
        "hr:employee:view", "hr:employee:manage",
        "hr:knowledge-base:view", "hr:knowledge-base:manage",
        "hr:training:view", "hr:training:manage",
        "hr:health-certificate:view", "hr:health-certificate:manage",
        "hr:contract:view", "hr:contract:manage",
        "hr:recruitment:view", "hr:recruitment:manage",
        "hr:salary:view", "hr:salary:manage",
        "hr:organization:view", "hr:organization:manage",
        "hr:position:view", "hr:position:manage",
        "hr:job-level:view", "hr:job-level:manage",
        "hr:onboarding:view", "hr:onboarding:manage",
        "hr:config:view", "hr:config:manage",
        "hr:invitation-code:view", "hr:invitation-code:manage",
        "hr:analytics:view",
        // === 门店管理模块 ===
        "store:shift:view", "store:shift:manage",
        "store:certificate:view", "store:certificate:manage",
        "store:archive:view", "store:archive:manage",
        "store:task:view", "store:task:manage",
        "store:settlement:view", "store:settlement:manage",
        "store:recruitment:view", "store:recruitment:manage",
        "store:table:view", "store:table:manage",
        "store:queue:view", "store:queue:manage",
        "store:material-request:view", "store:material-request:manage",
        "store:inventory:view", "store:inventory:manage",
        // === 排班管理模块 ===
        "schedule:view:self", "schedule:plan:create", "schedule:plan:edit", "schedule:plan:delete",
        "schedule:entry:edit", "schedule:shift:configure", "schedule:template:manage",
        // === 运营中心模块 ===
        "operations:dashboard:view", "operations:dashboard:manage",
        "operations:live-monitor:view", "operations:live-monitor:manage",
        "operations:decision-board:view", "operations:decision-board:manage",
        "operations:strategy:view", "operations:strategy:manage",
        "operations:alert:view", "operations:alert:manage",
        "operations:report:view", "operations:report:manage",
        // === 会员管理模块 ===
        "member:view", "member:manage", "member:delete",
        "member-level:view", "member-level:manage",
        "recharge:view", "recharge:manage",
        "recharge-plan:view", "recharge-plan:manage",
        "recharge-settings:view", "recharge-settings:manage",
        "refund:view", "refund:manage",
        // === 设备管理模块 ===
        "device:view", "device:manage", "device:delete",
        "device-alert:view", "device-alert:manage",
        "device-status-history:view",
        // === 电子签章模块 ===
        "seal:view", "seal:manage", "seal:delete",
        // === 食品追溯模块 ===
        "traceability:label-template:view", "traceability:label-template:manage",
        "traceability:supplier-trace:view",
        "traceability:inspection:view", "traceability:inspection:manage",
        "traceability:quality:view", "traceability:quality:manage",
        "traceability:expiry-alert:view", "traceability:expiry-alert:manage",
        "traceability:recall:view", "traceability:recall:manage",
        // === 硬件模块 ===
        "hardware:view", "hardware:manage"
    );

    private AdminPermissions() {
        // 工具类禁止实例化
    }

    /**
     * 判断权限列表是否包含通配符
     */
    public static boolean hasWildcard(java.util.Collection<String> permissions) {
        return permissions != null && permissions.contains(WILDCARD);
    }
}
