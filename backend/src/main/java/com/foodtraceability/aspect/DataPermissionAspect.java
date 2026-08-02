package com.foodtraceability.aspect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.User;
import com.foodtraceability.security.model.UserPermissionInfo;
import com.foodtraceability.security.service.UserPermissionCacheService;
import com.foodtraceability.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;


/**
 * 数据权限切面，用于实现数据隔离
 * 确保用户只能访问自己有权限的数据
 */
@Aspect
@Component
public class DataPermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataPermissionAspect.class);


    public DataPermissionAspect(UserService userService, UserPermissionCacheService userPermissionCacheService) {
        this.userService = userService;
        this.userPermissionCacheService = userPermissionCacheService;
    }

    private final UserService userService;

    private final UserPermissionCacheService userPermissionCacheService;

    /**
     * 定义切点：匹配所有ServiceImpl类中的select、list、page方法
     */
    @Pointcut("execution(* com.foodtraceability.service.impl.*ServiceImpl.select*(..)) || " +
            "execution(* com.foodtraceability.service.impl.*ServiceImpl.list*(..)) || " +
            "execution(* com.foodtraceability.service.impl.*ServiceImpl.page*(..))")
    public void dataPermissionPointcut() {
    }

    /**
     * 前置通知：在方法执行前添加数据过滤条件
     */
    @Before("dataPermissionPointcut()")
    public void before(JoinPoint joinPoint) {
        try {
            // 获取当前认证信息
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getPrincipal())) {
                // 未认证或匿名用户，不进行数据权限过滤
                return;
            }

            // 获取当前登录用户信息
            String username = authentication.getName();
            if (username == null || username.isEmpty()) {
                return;
            }

            User currentUser = userService.getUserByUsername(username);

            if (currentUser == null) {
                return;
            }

            // 从缓存获取用户权限信息
            Optional<UserPermissionInfo> permissionInfoOpt = userPermissionCacheService.getUserPermissionInfo(String.valueOf(currentUser.getId()));
            
            if (permissionInfoOpt.isEmpty()) {
                logger.warn("User permission info not found: userId={}", currentUser.getId());
                return;
            }
            
            UserPermissionInfo permissionInfo = permissionInfoOpt.get();

            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return;
            }

            // 遍历参数，找到QueryWrapper对象并添加数据过滤条件
            for (Object arg : args) {
                if (arg instanceof QueryWrapper) {
                    QueryWrapper<?> queryWrapper = (QueryWrapper<?>) arg;
                    addDataFilter(queryWrapper, permissionInfo);
                }
            }
        } catch (Exception e) {
            // 数据权限过滤失败不影响业务逻辑，只记录日志
            logger.error("数据权限过滤失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 添加数据过滤条件
     * @param queryWrapper 查询条件包装器
     * @param permissionInfo 用户权限信息
     */
    private void addDataFilter(QueryWrapper<?> queryWrapper, UserPermissionInfo permissionInfo) {
        // 获取用户角色和权限
        String storeId = permissionInfo.getStoreId();
        String departmentId = permissionInfo.getDepartmentId();
        Boolean isAdmin = permissionInfo.getIsAdmin();

        // 系统管理员可以访问所有数据，不需要过滤
        if (isAdmin != null && isAdmin) {
            return;
        }

        // 获取当前查询的实体类类型
        Class<?> entityClass = queryWrapper.getEntityClass();
        if (entityClass == null) {
            return;
        }

        String className = entityClass.getSimpleName();

        // 根据不同的实体类添加不同的数据过滤条件
        // 1. 门店相关数据过滤
        if (className.contains("Store") || className.contains("Inventory") || 
            className.contains("Order") || className.contains("Product") ||
            className.contains("Finance") || className.contains("Hr") ||
            className.contains("Health") || className.contains("Trace") ||
            className.contains("Marketing") || className.contains("Cashier")) {

            // 添加门店ID过滤条件
            if (StringUtils.hasText(storeId)) {
                queryWrapper.eq("store_id", storeId);
            }
        }

        // 2. 部门相关数据过滤
        if (className.contains("User") || className.contains("Department") ||
            className.contains("Employee") || className.contains("HealthCertificate")) {

            // 添加部门ID过滤条件
            if (StringUtils.hasText(departmentId)) {
                queryWrapper.eq("department_id", departmentId);
            }
        }

        // 3. 用户相关数据过滤（只能查看自己的数据）
        boolean isRegularUser = isAdmin == null || !isAdmin;
        if (className.contains("User") && isRegularUser) {
            queryWrapper.eq("id", permissionInfo.getUserId());
        }

        // 4. 健康证相关数据过滤
        if (className.contains("HealthCertificate")) {
            // 门店管理者只能查看自己门店的健康证数据
            if (StringUtils.hasText(storeId)) {
                queryWrapper.eq("store_id", storeId);
            }
        }
    }
}