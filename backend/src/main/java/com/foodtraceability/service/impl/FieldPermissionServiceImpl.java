package com.foodtraceability.service.impl;

import com.foodtraceability.dto.store.operation.vo.DailySettlementVO;
import com.foodtraceability.entity.User;
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

        String roles = user.getRoles();

        if (roles == null) {
            return LEVEL_STAFF;
        }

        // 根据角色字符串判断权限级别
        String roleLower = roles.toLowerCase();
        if (roleLower.contains("super_admin") || roleLower.contains("admin")) {
            return LEVEL_ADMIN;
        }
        if (roleLower.contains("finance_director") || roleLower.contains("operations_director") || roleLower.contains("hr_director")) {
            return LEVEL_OPERATIONS;
        }
        if (roleLower.contains("regional_manager")) {
            return LEVEL_REGIONAL_MANAGER;
        }
        if (roleLower.contains("store_manager")) {
            return LEVEL_STORE_MANAGER;
        }

        return LEVEL_STAFF;
    }
}
