package com.foodtraceability.service;

import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.User;

/**
 * 字段级权限控制服务接口
 * 用于根据用户角色动态过滤敏感字段
 * 实现字段级RBAC（Role-Based Access Control）
 *
 * 应用场景：
 * - 日结对账：总部可见成本/利润/毛利率，店长不可见
 * - 薪资数据：仅HR和财务可见具体金额
 * - 客户信息：脱敏手机号、地址等隐私数据
 *
 * 权限矩阵：
 * ┌────────────────────┬──────────┬───────────┬──────────┐
 * │ 角色               │ 成本/利润 │ 毛利率    │ 详细订单 │
 * ├────────────────────┼──────────┼───────────┼──────────┤
 * │ super_admin        │ ✅ 可见  │ ✅ 可见   │ ✅ 可见  │
 * │ finance_director   │ ✅ 可见  │ ✅ 可见   │ ✅ 可见  │
 * │ operations_director│ ✅ 可见  │ ✅ 可见   │ ❌ 不可见│
 * │ regional_manager   │ ❌ 不可见│ ❌ 不可见 │ ✅ 可见  │
 * │ store_manager      │ ❌ 不可见│ ❌ 不可见 │ ⚠️ 仅本店│
 * │ staff              │ ❌ 无权  │ ❌ 无权   │ ❌ 无权  │
 * └────────────────────┴──────────┴───────────┴──────────┘
 */
public interface FieldPermissionService {

    /**
     * 根据角色过滤日结对账VO的敏感字段
     *
     * @param vo          原始VO对象（包含所有字段）
     * @param currentUser 当前用户信息
     * @return 过滤后的VO对象（敏感字段已置空）
     */
    DailySettlementVO filterSettlementSensitiveFields(DailySettlementVO vo, User currentUser);

    /**
     * 检查用户是否有权查看指定模块的敏感数据
     *
     * @param module     模块标识（settlement/salary/customer等）
     * @param currentUser 当前用户
     * @return true-有权限 false-无权限
     */
    boolean hasSensitiveDataPermission(String module, User currentUser);

    /**
     * 获取用户的最高权限级别
     * 用于快速判断是否需要执行字段过滤
     *
     * @param user 用户实体
     * @return 权限级别（1=staff 2=store_manager 3=regional_manager 4=operations/finance 5=admin）
     */
    int getPermissionLevel(User user);
}
