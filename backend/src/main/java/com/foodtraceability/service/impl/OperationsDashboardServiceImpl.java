package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.OperationsDashboardDataService;
import com.foodtraceability.entity.StoreNew;
import com.foodtraceability.mapper.StoreNewMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.OperationsDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 运营数据中心服务实现类
 * 提供公司级数据分析与监控功能
 * 支持多店监控统计、门店绩效分析等运营决策支持
 *
 * <h2>权限控制说明</h2>
 * <ul>
 *   <li>operations_director（运营总监）：可查看所有门店数据</li>
 *   <li>regional_manager（区域经理）：只能查看辖区内的门店数据</li>
 *   <li>其他角色：无权访问</li>
 * </ul>
 *
 * <h2>跨模块数据联动</h2>
 * <ul>
 *   <li>门店列表来自 {@link StoreNewMapper}（stores_new 表）</li>
 *   <li>每家门店的绩效数据由 {@link OperationsDashboardDataService#getStorePerformance} 聚合</li>
 *   <li>内部聚合订单、排班、桌台等数据源</li>
 * </ul>
 */
@Service
public class OperationsDashboardServiceImpl implements OperationsDashboardService {

    private static final Logger log = LoggerFactory.getLogger(OperationsDashboardServiceImpl.class);

    /** 运营总监角色 */
    private static final String ROLE_OPERATIONS_DIRECTOR = "ROLE_OPERATIONS_DIRECTOR";
    /** 区域经理角色 */
    private static final String ROLE_REGIONAL_MANAGER = "ROLE_REGIONAL_MANAGER";

    private final OperationsDashboardDataService operationsDashboardDataService;
    private final StoreNewMapper storeNewMapper;

    /**
     * 构造函数注入
     *
     * @param operationsDashboardDataService 运营数据中心数据服务
     * @param storeNewMapper 门店Mapper（用于门店列表查询）
     */
    public OperationsDashboardServiceImpl(OperationsDashboardDataService operationsDashboardDataService,
                                           StoreNewMapper storeNewMapper) {
        this.operationsDashboardDataService = operationsDashboardDataService;
        this.storeNewMapper = storeNewMapper;
    }

    @Override
    public Map<String, Object> getDashboardStats(Principal principal) {
        log.info("获取运营统计数据: user={}", principal.getName());

        // 1. 权限检查：仅运营总监和区域经理可访问
        if (!hasPermission(principal)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问运营数据中心");
        }

        // 2. 获取用户辖区门店列表（区域经理需要限制范围）
        List<String> storeIds = getAuthorizedStoreIds(principal);

        // 3. 调用DataService获取统计数据
        Map<String, Object> stats = operationsDashboardDataService.getDashboardStats(storeIds, null);

        // 4. 补充权限信息
        stats.put("hasFullAccess", isOperationsDirector(principal));

        return stats;
    }

    @Override
    public IPage<Map<String, Object>> getStorePerformanceList(Integer page, Integer size,
                                                               Principal principal) {
        log.info("获取门店绩效列表: page={}, size={}, user={}", page, size, principal.getName());

        // 1. 权限检查
        if (!hasPermission(principal)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问运营数据中心");
        }

        // 2. 参数默认值处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1 || size > 100) {
            size = 10;
        }

        // 3. 获取用户有权限查看的门店ID列表
        List<String> authorizedStoreIds = getAuthorizedStoreIds(principal);

        // 4. 查询门店列表（仅未删除门店）
        LambdaQueryWrapper<StoreNew> storeWrapper = new LambdaQueryWrapper<>();
        storeWrapper.orderByDesc(StoreNew::getStatus)
                    .orderByAsc(StoreNew::getStoreId);

        // 区域经理仅查询辖区门店；运营总监查询全部
        if (authorizedStoreIds != null && !authorizedStoreIds.isEmpty()) {
            List<Long> storeIdLongs = authorizedStoreIds.stream()
                    .map(this::parseStoreIdSafely)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            if (storeIdLongs.isEmpty()) {
                // 辖区门店列表为空，返回空分页
                return new Page<>(page, size);
            }
            storeWrapper.in(StoreNew::getStoreId, storeIdLongs);
        }

        // 5. 分页查询门店
        Page<StoreNew> storePage = storeNewMapper.selectPage(new Page<>(page, size), storeWrapper);

        // 6. 对每个门店聚合绩效数据（跨模块联动）
        List<Map<String, Object>> performanceRecords = new ArrayList<>();
        for (StoreNew store : storePage.getRecords()) {
            String storeIdStr = String.valueOf(store.getStoreId());
            Map<String, Object> performance = operationsDashboardDataService.getStorePerformance(storeIdStr, null);

            // 补充门店基本信息（避免依赖 DataService 二次查询的缺失字段）
            performance.put("storeId", storeIdStr);
            performance.put("storeName", store.getStoreName());
            performance.put("storeCode", store.getStoreCode());
            performance.put("storeType", store.getStoreType());
            performance.put("status", store.getStatus());
            performance.put("address", store.getAddress());
            performance.put("managerId", store.getManagerId());

            performanceRecords.add(performance);
        }

        // 7. 构造返回分页结果
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setRecords(performanceRecords);
        resultPage.setTotal(storePage.getTotal());
        return resultPage;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查用户是否有权限访问运营数据中心
     *
     * @param principal 用户身份
     * @return 有权限返回true
     */
    private boolean hasPermission(Principal principal) {
        if (principal == null) {
            return false;
        }
        // 使用SecurityUtils检查角色 - 通过当前用户的角色列表判断
        return isOperationsDirector(principal) || isRegionalManager(principal);
    }

    /**
     * 判断是否为运营总监（拥有全部数据访问权限）
     * ROLE_ADMIN 视为超级管理员，拥有运营总监的全部权限
     *
     * @param principal 用户身份
     * @return 是运营总监返回true
     */
    private boolean isOperationsDirector(Principal principal) {
        if (principal == null) {
            return false;
        }
        // 从SecurityContext获取SecurityUser（JWT认证后存入的对象）
        SecurityUser user = getSecurityUser();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        List<String> roles = user.getRoles();
        return roles.contains("operations_director")
                || roles.contains("ROLE_OPERATIONS_DIRECTOR")
                || roles.contains("ROLE_ADMIN")
                || roles.contains("admin")
                || roles.contains("ADMIN");
    }

    /**
     * 判断是否为区域经理
     *
     * @param principal 用户身份
     * @return 是区域经理返回true
     */
    private boolean isRegionalManager(Principal principal) {
        if (principal == null) {
            return false;
        }
        SecurityUser user = getSecurityUser();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        List<String> roles = user.getRoles();
        return roles.contains("regional_manager")
                || roles.contains("ROLE_REGIONAL_MANAGER");
    }

    /**
     * 从SecurityContext获取SecurityUser
     */
    private SecurityUser getSecurityUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取用户有权限查看的门店ID列表
     * 运营总监返回null（表示全部），区域经理返回辖区门店列表
     *
     * <p>当前实现说明：</p>
     * <ul>
     *   <li>运营总监：返回 null（全部门店）</li>
     *   <li>区域经理：返回辖区门店列表（当前为单店系统，默认返回门店 ID=1）</li>
     *   <li>TODO：后续对接用户-门店关联表（user_store_relations）查询真实辖区</li>
     * </ul>
     *
     * @param principal 用户身份
     * @return 门店ID列表，null表示全部
     */
    private List<String> getAuthorizedStoreIds(Principal principal) {
        if (isOperationsDirector(principal)) {
            // 运营总监可查看所有门店
            return null;
        }

        // 区域经理获取辖区门店
        // 当前为单店系统，默认返回门店 ID=1
        // TODO: 后续从 user_store_relations 表查询真实辖区门店列表
        try {
            LambdaQueryWrapper<StoreNew> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StoreNew::getStatus, 1)
                    .orderByAsc(StoreNew::getStoreId);
            List<StoreNew> activeStores = storeNewMapper.selectList(wrapper);
            if (activeStores == null || activeStores.isEmpty()) {
                return new ArrayList<>();
            }
            return activeStores.stream()
                    .map(s -> String.valueOf(s.getStoreId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("查询辖区门店列表失败，返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 安全解析门店 ID
     */
    private Long parseStoreIdSafely(String storeId) {
        try {
            return Long.parseLong(storeId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
