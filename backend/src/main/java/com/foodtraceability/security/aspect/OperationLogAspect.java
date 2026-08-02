package com.foodtraceability.security.aspect;

import com.foodtraceability.entity.OperationLogEntity;
import com.foodtraceability.security.annotation.OperationLog;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志记录切面
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogService operationLogService, ObjectMapper objectMapper) {
        this.operationLogService = operationLogService;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@annotation(com.foodtraceability.security.annotation.OperationLog)")
    public void operationLogPointCut() {
    }

    @Around("operationLogPointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            try {
                handleLog(joinPoint, result, exception, duration);
            } catch (Exception e) {
                logger.error("记录操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void handleLog(JoinPoint joinPoint, Object result, Exception exception, long duration) {
        OperationLog annotation = getAnnotation(joinPoint);
        if (annotation == null) {
            return;
        }

        OperationLogEntity log = new OperationLogEntity();
        log.setOperationModule(annotation.module());
        log.setOperationType(annotation.type().name());
        log.setOperationDesc(annotation.desc());
        log.setStatus(exception == null ? "SUCCESS" : "FAIL");
        log.setOperationTime(LocalDateTime.now());
        log.setDuration(duration);

        if (exception != null) {
            log.setErrorMsg(truncateMessage(exception.getMessage()));
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setOperatorIp(getClientIp(request));
        }

        if (annotation.saveParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    String params = objectMapper.writeValueAsString(args[0]);
                    params = maskSensitiveData(params);
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000) + "...(truncated)";
                    }
                    log.setRequestParams(params);
                }
            } catch (Exception e) {
                log.setRequestParams("参数序列化失败");
            }
        }

        if (annotation.saveResult() && result != null) {
            try {
                String resultStr = objectMapper.writeValueAsString(result);
                resultStr = maskSensitiveData(resultStr);
                if (resultStr.length() > 2000) {
                    resultStr = resultStr.substring(0, 2000) + "...(truncated)";
                }
                log.setResponseResult(resultStr);
            } catch (Exception e) {
                log.setResponseResult("结果序列化失败");
            }
        }

        SecurityUser user = getCurrentUser();
        if (user != null) {
            log.setOperatorId(user.getUserId());
            log.setOperatorName(user.getUsername());
        }

        operationLogService.logOperation(log);
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 1000 ? message.substring(0, 1000) + "..." : message;
    }

    private OperationLog getAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(OperationLog.class);
    }

    private SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 敏感数据脱敏处理
     */
    private String maskSensitiveData(String data) {
        if (data == null) {
            return null;
        }
        // 密码脱敏
        data = data.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
        data = data.replaceAll("\"oldPassword\"\\s*:\\s*\"[^\"]*\"", "\"oldPassword\":\"***\"");
        data = data.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"***\"");
        data = data.replaceAll("\"confirmPassword\"\\s*:\\s*\"[^\"]*\"", "\"confirmPassword\":\"***\"");
        data = data.replaceAll("\"pwd\"\\s*:\\s*\"[^\"]*\"", "\"pwd\":\"***\"");
        data = data.replaceAll("\"passwd\"\\s*:\\s*\"[^\"]*\"", "\"passwd\":\"***\"");
        // Token脱敏
        data = data.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
        data = data.replaceAll("\"accessToken\"\\s*:\\s*\"[^\"]*\"", "\"accessToken\":\"***\"");
        data = data.replaceAll("\"refreshToken\"\\s*:\\s*\"[^\"]*\"", "\"refreshToken\":\"***\"");
        data = data.replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"");
        // 身份证号脱敏
        data = data.replaceAll("(\"idCard\"\\s*:\\s*\")\\d{10}(\\d{4}\")", "$1****$2");
        // 手机号脱敏
        data = data.replaceAll("(\"phone\"\\s*:\\s*\")\\d{3}(\\d{4})(\\d{4}\")", "$1***$2$3");
        data = data.replaceAll("(\"mobile\"\\s*:\\s*\")\\d{3}(\\d{4})(\\d{4}\")", "$1***$2$3");
        return data;
    }
}
