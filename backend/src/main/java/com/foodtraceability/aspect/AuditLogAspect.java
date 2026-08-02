package com.foodtraceability.aspect;

import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogAspect.class);


    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    private final AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, com.foodtraceability.annotation.AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        com.foodtraceability.entity.AuditLog logEntity = new com.foodtraceability.entity.AuditLog();
        logEntity.setOperation(auditLog.value());
        logEntity.setModule(auditLog.module());
        logEntity.setOperationType(auditLog.operationType().name());
        logEntity.setSensitiveFlag(auditLog.sensitive() ? 1 : 0);
        logEntity.setBusinessKey(auditLog.businessKey());

        try {
            fillRequestInfo(logEntity);
            fillUserInfo(logEntity);

            Object result = joinPoint.proceed();

            logEntity.setResponseStatus("SUCCESS");

            if (auditLog.sensitive()) {
                logEntity.setRequestParams("[SENSITIVE]");
            } else {
                logEntity.setRequestParams(buildRequestParams(joinPoint));
            }

            // 记录操作后值（after_value），用于防篡改证明（会计法/网络安全法要求）
            // 仅在非敏感操作时记录返回值，敏感操作的返回值可能包含密码等敏感信息
            if (!auditLog.sensitive()) {
                logEntity.setAfterValue(buildResultSnapshot(result));
            }

            return result;
        } catch (Exception e) {
            logEntity.setResponseStatus("FAILED");
            logEntity.setErrorMessage(e.getMessage() != null && e.getMessage().length() > 500 ? 
                e.getMessage().substring(0, 500) : e.getMessage());
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logEntity.setExecutionTime(executionTime);
            logEntity.setRiskLevel(determineRiskLevel(auditLog.operationType(), executionTime));
            logEntity.setCreatedAt(LocalDateTime.now());

            saveAuditLogAsync(logEntity);
        }
    }

    /**
     * 构建方法返回值的 JSON 快照（用于 after_value 防篡改字段）。
     * 仅记录可序列化的返回值，跳过 HttpServletResponse 等不可序列化对象。
     */
    private String buildResultSnapshot(Object result) {
        if (result == null) {
            return null;
        }
        try {
            // 跳过不可序列化的响应对象
            if (result instanceof HttpServletResponse || result instanceof MultipartFile) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(result);
            // 限制长度，避免过大日志
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "...[TRUNCATED]";
            }
            return json;
        } catch (Exception e) {
            logger.debug("序列化返回值失败: {}", e.getMessage());
            return null;
        }
    }

    private void fillRequestInfo(com.foodtraceability.entity.AuditLog logEntity) {
        logEntity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setIp(getClientIp(request));
                logEntity.setUserAgent(request.getHeader("User-Agent"));
                logEntity.setRequestUrl(request.getRequestURI());
                logEntity.setRequestMethod(request.getMethod());
            }
        } catch (Exception e) {
            logger.debug("获取请求信息失败: {}", e.getMessage());
        }
    }

    private void fillUserInfo(com.foodtraceability.entity.AuditLog logEntity) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                    Object userObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                    if (userObj instanceof com.foodtraceability.security.model.SecurityUser) {
                        com.foodtraceability.security.model.SecurityUser securityUser = (com.foodtraceability.security.model.SecurityUser) userObj;
                        logEntity.setUserId(securityUser.getUserId());
                        logEntity.setUsername(securityUser.getUsername());
                    }
                } else if (principal instanceof String) {
                    logEntity.setUsername((String) principal);
                    logEntity.setUserId((String) principal);
                }
            }
        } catch (Exception e) {
            logger.debug("获取用户信息失败: {}", e.getMessage());
        }
    }

    private void saveAuditLogAsync(com.foodtraceability.entity.AuditLog logEntity) {
        try {
            auditLogService.saveAsync(logEntity);
        } catch (Exception e) {
            logger.error("保存审计日志失败: {}", e.getMessage(), e);
        }
    }

    private String buildRequestParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg == null || arg instanceof MultipartFile || 
                    arg instanceof HttpServletRequest || 
                    arg instanceof HttpServletResponse ||
                    arg.getClass().getName().contains("BindingResult")) {
                    continue;
                }
                params.put("arg" + i, arg);
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(params);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    private String determineRiskLevel(OperationType type, long executionTime) {
        switch (type) {
            case LOGIN:
            case AUTH:
                return "HIGH";
            case DELETE:
            case EXPORT:
                return "MEDIUM";
            case CREATE:
            case UPDATE:
                return executionTime > 1000 ? "MEDIUM" : "LOW";
            case SYSTEM:
                return executionTime > 3000 ? "HIGH" : "MEDIUM";
            case SECURITY:
                return "CRITICAL";
            case VIEW:
                return "LOW";
            default:
                return "LOW";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
