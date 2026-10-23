package com.foodtraceability.service.impl;

import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;

import java.util.List;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.FieldPermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 字段级权限控制服务实现类
 * 根据用户角色动态过滤敏感字段，实现字段级RBAC
 */
@Service
public class FieldPermissionServiceImpl implements FieldPermissionService {

    private static final Logger log = LoggerFactory.getLogger(FieldPermissionServiceImpl.class);

    /** 授权权威源查询（user_roles → roles）；users.roles 不参与鉴权 */
    private final UserMapper userMapper;

    public FieldPermissionServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 权限级别常量 */
    public static final int LEVEL_STAFF = 1;            // 普通员工
    public static final int LEVEL_STORE_MANAGER = 2;   // 店长
    public static final int LEVEL_REGIONAL_MANAGER = 3;// 区域经理
    public static final int LEVEL_OPERATIONS = 4;      // 运营总监/财务总监
    public static final int LEVEL_ADMIN = 5;           // 超级管理员

    @Override
    public DailySettlementVO filterSettlementSensitiveFields(DailySettlementVO vo, User currentUser) {
        if (vo == null || currentUser == null) {
            return vo;
        }

        int permissionLevel = getPermissionLevel(currentUser);

        log.debug("过滤日结对账敏感字段: userId={}, level={}", currentUser.getId(), permissionLevel);

        // 级别 >= 4 (运营/财务/管理员)：可见全部字段，无需过滤
        if (permissionLevel >= LEVEL_OPERATIONS) {
            return vo;
        }

        // 级别 <= 3 (区域经理及以下)：隐藏敏感字段（成本、利润、毛利率）
        vo.setTotalCost(null);
        vo.setNetProfit(null);
        vo.setGrossProfitRate(null);

        // 级别 == 1 (普通员工)：无权访问，返回空VO或抛出异常
        if (permissionLevel == LEVEL_STAFF) {
            log.warn("普通员工尝试访问日结敏感数据: userId={}", currentUser.getId());
            // 可选择：1) 返回空VO  2) 抛出 AccessDeniedException
            // 当前策略：返回仅包含基础字段的VO（已在上一步清空敏感字段）
        }

        return vo;
    }

    @Override
    public boolean hasSensitiveDataPermission(String module, User currentUser) {
        if (currentUser == null || module == null) {
            return false;
        }

        int level = getPermissionLevel(currentUser);

        switch (module) {
            case "settlement":
                // 日结模块：需要运营级别以上才能查看敏感数据
                return level >= LEVEL_OPERATIONS;

            case "salary":
                // 薪资模块：需要财务或HR级别
                return level >= LEVEL_OPERATIONS;

            case "customer":
                // 客户信息：店长及以上可查看脱敏数据
                return level >= LEVEL_STORE_MANAGER;

            default:
                log.warn("未知的权限模块: module={}", module);
                return false;
        }
    }

    @Override
    public int getPermissionLevel(User user) {
        if (user == null) {
            return LEVEL_STAFF; // 默认最低权限
        }

        // P1-AUTH-CHAIN-001：角色来源 = user_roles → roles（role_code + role_id_str），
        // 不再读 users.roles。原实现把 users.roles（数字ID的 JSON，如 ["9"]）当角色名做
        // contains("store_manager") 匹配，永远不命中 → 所有人都被判成 LEVEL_STAFF。
        if (user.getId() == null) {
            return LEVEL_STAFF;
        }
        List<Role> roles = userMapper.getRoleEntitiesByUserId(user.getId());
        if (roles == null || roles.isEmpty()) {
            return LEVEL_STAFF;
        }
        String roleText = roles.stream()
                .map(r -> (r.getRoleCode() != null ? r.getRoleCode() : "") + " "
                        + (r.getRoleIdStr() != null ? r.getRoleIdStr() : ""))
                .collect(java.util.stream.Collectors.joining(" "))
                .toLowerCase();

        if (roleText.contains("super_admin") || roleText.contains("admin")) {
            return LEVEL_ADMIN;
        }
        if (roleText.contains("finance_director") || roleText.contains("operations_director") || roleText.contains("hr_director")) {
            return LEVEL_OPERATIONS;
        }
        if (roleText.contains("regional_manager")) {
            return LEVEL_REGIONAL_MANAGER;
        }
        if (roleText.contains("store_manager")) {
            return LEVEL_STORE_MANAGER;
        }

        return LEVEL_STAFF;
    }
}
