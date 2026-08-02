package com.foodtraceability.aspect;

import com.foodtraceability.annotation.*;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.service.DataPermissionService;
import com.foodtraceability.service.PermissionVerifyService;
import com.foodtraceability.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * 权限验证切面
 * 安全增强版：
 * 1. 拦截@RequiresPermission、@RequiresPermissions、@RequiresRole、@RequiresDataScope注解
 * 2. 进行权限验证和数据权限过滤
 * 3. 添加Token完整性验证和安全审计
 */
@Aspect
@Component
public class PermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");


    public PermissionAspect(PermissionVerifyService permissionVerifyService, DataPermissionService dataPermissionService, JwtUtil jwtUtil) {
        this.permissionVerifyService = permissionVerifyService;
        this.dataPermissionService = dataPermissionService;
        this.jwtUtil = jwtUtil;
    }

    private final PermissionVerifyService permissionVerifyService;

    private final DataPermissionService dataPermissionService;

    private final JwtUtil jwtUtil;

    /**
     * Token验证缓存（避免同一请求重复解析）
     * 使用ThreadLocal确保线程安全
     */
    private static final ThreadLocal<Claims> tokenClaimsCache = new ThreadLocal<>();

    /**
     * 拦截@RequiresPermission注解
     * 验证用户是否具有指定权限
     */
    @Around("@annotation(com.foodtraceability.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 验证Token完整性
        String userId = validateTokenAndGetUserId();

        // 2. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);

        if (annotation != null) {
            String permission = annotation.value();

            boolean hasPermission = permissionVerifyService.hasPermission(userId, permission);

            if (!hasPermission) {
                logSecurityEvent(userId, "PERMISSION_DENIED",
                    "权限验证失败: permission=" + permission +
                    ", method=" + method.getName());
                throw new BusinessException(403, "权限不足: " + permission);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 拦截@RequiresPermissions注解
     * 验证用户是否具有指定权限（支持AND/OR逻辑）
     */
    @Around("@annotation(com.foodtraceability.annotation.RequiresPermissions)")
    public Object checkPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 验证Token完整性
        String userId = validateTokenAndGetUserId();

        // 2. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermissions annotation = method.getAnnotation(RequiresPermissions.class);

        if (annotation != null) {
            String[] permissions = annotation.value();
            Logical logical = annotation.logical();

            if (permissions.length == 0) {
                return joinPoint.proceed();
            }

            List<String> permissionList = Arrays.asList(permissions);
            boolean hasPermission;

            if (logical == Logical.AND) {
                hasPermission = permissionVerifyService.hasAllPermissions(userId, permissionList);
            } else {
                hasPermission = permissionVerifyService.hasAnyPermission(userId, permissionList);
            }

            if (!hasPermission) {
                logSecurityEvent(userId, "PERMISSION_DENIED",
                    "权限验证失败: permissions=" + Arrays.toString(permissions) +
                    ", logical=" + logical +
                    ", method=" + method.getName());
                throw new BusinessException(403, "权限不足: " + String.join(
                    logical == Logical.AND ? " AND " : " OR ", permissions));
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 拦截@RequiresRole注解
     * 验证用户是否具有指定角色
     */
    @Around("@annotation(com.foodtraceability.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 验证Token完整性
        String userId = validateTokenAndGetUserId();

        // 2. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);

        if (annotation != null) {
            String role = annotation.value();

            boolean hasRole = permissionVerifyService.hasRole(userId, role);

            if (!hasRole) {
                logSecurityEvent(userId, "ROLE_DENIED",
                    "角色验证失败: role=" + role +
                    ", method=" + method.getName());
                throw new BusinessException(403, "角色不足: " + role);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 拦截@RequiresDataScope注解
     * 验证用户的数据权限范围
     */
    @Before("@annotation(com.foodtraceability.annotation.RequiresDataScope)")
    public void checkDataScope(JoinPoint joinPoint) {
        // 1. 验证Token完整性
        String userId = validateTokenAndGetUserId();

        // 2. 获取注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresDataScope annotation = method.getAnnotation(RequiresDataScope.class);

        if (annotation != null) {
            // 检查是否启用数据权限
            if (!annotation.enabled()) {
                return;
            }

            String resourceType = annotation.resourceType();
            String userScope = permissionVerifyService.getUserDataScope(userId);

            // 数据权限范围优先级比较
            if (!checkDataScopeLevel(userScope, resourceType)) {
                logSecurityEvent(userId, "DATA_SCOPE_DENIED",
                    "数据权限验证失败: resourceType=" + resourceType +
                    ", userScope=" + userScope +
                    ", method=" + method.getName());
                throw new BusinessException(403, "数据权限不足: 需要 " + resourceType + " 权限");
            }
        }
    }

    /**
     * 验证Token完整性并获取用户ID
     * 安全增强：
     * 1. 从请求头提取Token
     * 2. 使用JwtUtil进行完整的安全验证
     * 3. 检查Token过期时间
     * 4. 记录安全审计日志
     *
     * @return 用户ID
     * @throws BusinessException 如果Token无效或用户未登录
     */
    private String validateTokenAndGetUserId() throws BusinessException {
        try {
            // 1. 从Spring Security获取认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 2. 基本认证检查
            if (authentication == null || !authentication.isAuthenticated()) {
                logSecurityEvent(null, "UNAUTHENTICATED", "用户未认证或认证信息为空");
                throw new BusinessException(401, "用户未登录");
            }

            if ("anonymousUser".equals(authentication.getPrincipal())) {
                logSecurityEvent(null, "ANONYMOUS_ACCESS", "匿名用户尝试访问受保护资源");
                throw new BusinessException(401, "用户未登录");
            }

            // 3. 获取用户ID
            // 注意：authentication.getName() 返回的是 username（如 "admin"），
            // 但 PermissionVerifyService 需要 userId（数字字符串如 "1"）。
            // 修复：从 SecurityUser principal 提取 userId。
            String userId;
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.foodtraceability.security.model.SecurityUser) {
                userId = ((com.foodtraceability.security.model.SecurityUser) principal).getUserId();
            } else {
                userId = authentication.getName();
            }

            // 4. 从HTTP请求中提取Token并进行二次验证
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String token = extractTokenFromRequest(request);
                if (token != null) {
                    // 使用JwtUtil进行完整的安全验证
                    Claims claims = jwtUtil.parseJwt(token);
                    if (claims == null) {
                        logSecurityEvent(userId, "INVALID_TOKEN",
                            "Token安全验证失败，可能是签名被篡改或已过期");
                        throw new BusinessException(401, "Token已失效，请重新登录");
                    }

                    // 缓存Token声明，避免重复解析
                    tokenClaimsCache.set(claims);

                    // 检查Token即将过期（提前5分钟警告）
                    long timeUntilExpiry = claims.getExpiration().getTime() - System.currentTimeMillis();
                    if (timeUntilExpiry > 0 && timeUntilExpiry < 300000) { // 5分钟
                        logger.debug("Token即将过期: userId={}, 剩余{}秒", userId, timeUntilExpiry / 1000);
                    }
                } else {
                    logger.warn("无法从请求中提取Token，仅依赖SecurityContext: userId={}", userId);
                }
            }

            return userId;
        } catch (BusinessException e) {
            throw e;  // 重新抛出业务异常
        } catch (ExpiredJwtException e) {
            logSecurityEvent(null, "TOKEN_EXPIRED", "Token已过期: " + e.getMessage());
            throw new BusinessException(401, "Token已过期，请重新登录");
        } catch (Exception e) {
            logger.error("Token验证过程异常: {}", e.getMessage(), e);
            logSecurityEvent(null, "TOKEN_VALIDATION_ERROR", "Token验证异常: " + e.getClass().getSimpleName());
            throw new BusinessException(401, "Token验证失败，请重新登录");
        }
    }

    /**
     * 从HTTP请求中提取Bearer Token
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            logger.debug("无法获取当前HTTP请求: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前登录用户ID（兼容旧代码）
     * @deprecated 建议使用 validateTokenAndGetUserId()
     */
    @Deprecated
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            return authentication.getName();
        } catch (Exception e) {
            logger.error("获取当前用户ID失败", e);
            return null;
        }
    }

    /**
     * 记录安全审计事件
     * 用于追踪权限验证失败和安全事件
     *
     * @param userId 用户ID
     * @param eventType 事件类型
     * @param eventDetails 事件详情
     */
    private void logSecurityEvent(String userId, String eventType, String eventDetails) {
        try {
            // 构建安全事件日志
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("[安全审计] ")
                     .append("事件类型=").append(eventType)
                     .append(", 用户ID=").append(userId != null ? userId : "UNKNOWN")
                     .append(", 时间戳=").append(java.time.Instant.now())
                     .append(", 详情=").append(eventDetails);

            // 尝试获取请求信息
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                logMessage.append(", 请求URI=").append(request.getRequestURI())
                         .append(", 请求方法=").append(request.getMethod())
                         .append(", 客户端IP=").append(getClientIp(request));
            }

            // 记录到安全审计日志（使用专门的logger）
            securityLogger.warn(logMessage.toString());

            // 同时记录到主日志（WARN级别便于告警）
            logger.warn("安全事件: type={}, userId={}", eventType, userId);
        } catch (Exception e) {
            logger.error("记录安全审计事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 安全增强：验证IP格式防止X-Forwarded-For头伪造攻击
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = null;
        
        // 优先从可信代理头获取（需配合Nginx/Apache配置）
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        
        for (String header : headers) {
            ip = request.getHeader(header);
            if (isValidIpAddress(ip)) {
                break;
            }
            ip = null;
        }
        
        // 如果所有代理头都无效，使用直接连接IP
        if (!isValidIpAddress(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 验证IP地址格式是否合法
     * 防止X-Forwarded-For头注入恶意内容
     *
     * @param ip IP地址字符串
     * @return 是否为有效的IPv4/IPv6地址
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        
        // 基本长度检查（IPv4最大15字符，IPv6最大45字符）
        if (ip.length() > 50) {
            return false;
        }
        
        // IPv4正则验证
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        
        // IPv6简化验证（允许压缩格式）
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^::$|^([0-9a-fA-F]{1,4}:){1,7}:$|^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$";
        
        return ip.matches(ipv4Pattern) || ip.matches(ipv6Pattern);
    }

    /**
     * 检查数据权限范围级别
     * 数据权限范围优先级：all > company > region > stores/departments > store > department > self
     */
    private boolean checkDataScopeLevel(String userScope, String requiredScope) {
        // 管理员拥有所有权限
        if ("all".equals(userScope)) {
            return true;
        }

        // 获取权限级别
        int userLevel = getDataScopeLevel(userScope);
        int requiredLevel = getDataScopeLevel(requiredScope);

        // 用户权限级别必须大于等于所需权限级别
        return userLevel >= requiredLevel;
    }

    /**
     * 获取数据权限范围级别
     */
    private int getDataScopeLevel(String scope) {
        if (scope == null) {
            return 0;
        }

        switch (scope) {
            case "all":
                return 100;
            case "company":
                return 80;
            case "region":
                return 70;
            case "stores":
            case "departments":
                return 60;
            case "store":
            case "department":
                return 40;
            case "self":
                return 20;
            default:
                return 0;
        }
    }

    /**
     * 清理ThreadLocal资源
     * 应该在请求结束时调用
     */
    public static void clearTokenCache() {
        tokenClaimsCache.remove();
    }
}
