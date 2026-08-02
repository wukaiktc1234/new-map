package com.foodtraceability.aspect;

import com.foodtraceability.annotation.SensitiveOperation;
import com.foodtraceability.common.LogUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 敏感操作日志切面类
 * 用于处理@SensitiveOperation注解，记录敏感操作日志
 */
@Aspect
@Component
public class SensitiveOperationAspect {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveOperationAspect.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定义切点，拦截所有带有@SensitiveOperation注解的方法
     */
    @Pointcut("@annotation(com.foodtraceability.annotation.SensitiveOperation)")
    public void sensitiveOperationPointcut() {
    }

    /**
     * 环绕通知，记录敏感操作日志
     */
    @Around("sensitiveOperationPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取SensitiveOperation注解
        SensitiveOperation sensitiveOperation = method.getAnnotation(SensitiveOperation.class);
        
        if (sensitiveOperation == null) {
            // 如果没有SensitiveOperation注解，直接执行方法
            return joinPoint.proceed();
        }
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 获取操作人信息
        String username = "unknown";
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                username = (String) principal;
            }
        } catch (Exception e) {
            // 忽略获取用户信息失败的异常
        }
        
        // 获取IP地址
        String ip = request != null ? getIpAddress(request) : "unknown";
        
        // 构建日志信息
        Map<String, Object> logInfo = new HashMap<>();
        logInfo.put("timestamp", LocalDateTime.now().format(formatter));
        logInfo.put("operation", sensitiveOperation.name());
        logInfo.put("type", sensitiveOperation.type());
        logInfo.put("username", username);
        logInfo.put("ip", ip);
        logInfo.put("class", method.getDeclaringClass().getSimpleName());
        logInfo.put("method", method.getName());
        
        // 记录请求参数
        if (sensitiveOperation.logParams()) {
            logInfo.put("params", Arrays.toString(joinPoint.getArgs()));
        }
        
        // 执行方法
        Object result;
        try {
            result = joinPoint.proceed();
            logInfo.put("status", "success");
            
            // 记录返回结果
            if (sensitiveOperation.logResult()) {
                logInfo.put("result", result);
            }
        } catch (Exception e) {
            logInfo.put("status", "error");
            logInfo.put("error", e.getMessage());
            throw e;
        } finally {
            // 记录日志
            logger.info("敏感操作日志: {}", LogUtil.toJsonString(logInfo));
        }
        
        return result;
    }
    
    /**
     * 获取真实IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP地址的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
