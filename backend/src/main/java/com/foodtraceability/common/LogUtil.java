package com.foodtraceability.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 统一日志工具类
 * 提供标准化的日志记录方法，支持MDC上下文信息
 */
public class LogUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);
    
    // MDC key常量
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_REQUEST_URI = "requestUri";
    public static final String MDC_METHOD = "method";
    
    /**
     * 生成请求ID
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 设置请求ID到MDC
     */
    public static void setRequestId(String requestId) {
        MDC.put(MDC_REQUEST_ID, requestId);
    }
    
    /**
     * 设置用户信息到MDC
     */
    public static void setUserId(String userId) {
        MDC.put(MDC_USER_ID, userId);
    }
    
    /**
     * 设置请求信息到MDC
     */
    public static void setRequestInfo(String requestUri, String method) {
        MDC.put(MDC_REQUEST_URI, requestUri);
        MDC.put(MDC_METHOD, method);
    }
    
    /**
     * 清理MDC信息
     */
    public static void clearMDC() {
        MDC.clear();
    }
    
    /**
     * 记录业务操作日志
     */
    public static void logBusiness(String operation, String description, Object... params) {
        logger.info("[BUSINESS] {} - {} - 参数: {}", operation, description, formatParams(params));
    }
    
    /**
     * 记录业务操作成功日志
     */
    public static void logBusinessSuccess(String operation, String description, Object... params) {
        logger.info("[BUSINESS] [成功] {} - {} - 参数: {}", operation, description, formatParams(params));
    }
    
    /**
     * 记录业务操作失败日志
     */
    public static void logBusinessError(String operation, String description, String errorMsg, Object... params) {
        logger.error("[BUSINESS] [失败] {} - {} - 错误: {} - 参数: {}", operation, description, errorMsg, formatParams(params));
    }
    
    /**
     * 记录系统操作日志
     */
    public static void logSystem(String operation, String description, Object... params) {
        logger.info("[SYSTEM] {} - {} - 参数: {}", operation, description, formatParams(params));
    }
    
    /**
     * 记录系统错误日志
     */
    public static void logSystemError(String operation, String description, Throwable throwable, Object... params) {
        logger.error("[SYSTEM] [错误] {} - {} - 异常: {} - 参数: {}", operation, description, throwable.getMessage(), formatParams(params), throwable);
    }
    
    /**
     * 记录安全相关日志
     */
    public static void logSecurity(String operation, String description, String userId, Object... params) {
        logger.warn("[SECURITY] {} - {} - 用户: {} - 参数: {}", operation, description, userId, formatParams(params));
    }
    
    /**
     * 记录安全警告日志
     */
    public static void logSecurityWarning(String operation, String description, String userId, String warning, Object... params) {
        logger.warn("[SECURITY] [警告] {} - {} - 用户: {} - 警告: {} - 参数: {}", operation, description, userId, warning, formatParams(params));
    }
    
    /**
     * 记录性能相关日志
     */
    public static void logPerformance(String operation, long startTime, Object... params) {
        long duration = System.currentTimeMillis() - startTime;
        logger.info("[PERFORMANCE] {} - 耗时: {}ms - 参数: {}", operation, duration, formatParams(params));
    }
    
    /**
     * 记录慢查询日志
     */
    public static void logSlowQuery(String operation, long startTime, long threshold, Object... params) {
        long duration = System.currentTimeMillis() - startTime;
        if (duration > threshold) {
            logger.warn("[SLOW_QUERY] {} - 耗时: {}ms (阈值: {}ms) - 参数: {}", operation, duration, threshold, formatParams(params));
        }
    }
    
    /**
     * 记录API调用日志
     */
    public static void logApi(String method, String uri, String description, Object... params) {
        logger.info("[API] {} {} - {} - 参数: {}", method, uri, description, formatParams(params));
    }
    
    /**
     * 记录API错误日志（带请求ID）
     */
    public static void logApiError(String method, String uri, String description, String errorMsg, String requestId, Object... params) {
        logger.error("[API] [错误] {} {} - {} - 错误: {} - 请求ID: {} - 参数: {}", method, uri, description, errorMsg, requestId, formatParams(params));
    }
    
    /**
     * 记录数据库操作日志
     */
    public static void logDatabase(String operation, String table, Object... params) {
        logger.info("[DATABASE] {} - 表: {} - 参数: {}", operation, table, formatParams(params));
    }
    
    /**
     * 记录数据库错误日志
     */
    public static void logDatabaseError(String operation, String description, String errorMsg, Object... params) {
        logger.error("[DATABASE] [错误] {} - {} - 错误: {} - 参数: {}", operation, description, errorMsg, formatParams(params));
    }
    
    /**
     * 记录文件操作日志
     */
    public static void logFile(String operation, String fileName, Object... params) {
        logger.info("[FILE] {} - 文件: {} - 参数: {}", operation, fileName, formatParams(params));
    }
    
    /**
     * 记录文件操作错误日志
     */
    public static void logFileError(String operation, String description, String errorMsg, Object... params) {
        logger.error("[FILE] [错误] {} - {} - 错误: {} - 参数: {}", operation, description, errorMsg, formatParams(params));
    }
    
    /**
     * 格式化参数
     */
    private static String formatParams(Object... params) {
        if (params == null || params.length == 0) {
            return "无";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (params[i] == null) {
                sb.append("null");
            } else {
                sb.append(params[i].toString());
            }
        }
        return sb.toString();
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return com.alibaba.fastjson2.JSON.toJSONString(obj);
        } catch (Exception e) {
            logger.error("对象转JSON失败: {}", e.getMessage());
            return obj.toString();
        }
    }
    
    /**
     * 获取Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取Logger实例
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }

    /**
     * 获取异常的堆栈信息
     * @param throwable 异常对象
     * @return 异常堆栈信息字符串
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * 记录错误日志，支持可变参数
     */
    public static void error(Logger logger, String operation, Object... params) {
        Object[] newParams = new Object[params.length + 1];
        newParams[0] = operation;
        System.arraycopy(params, 0, newParams, 1, params.length);
        logger.error("[{}] {}", newParams);
    }

    /**
     * 记录信息日志，支持可变参数
     */
    public static void info(Logger logger, String operation, Object... params) {
        Object[] newParams = new Object[params.length + 1];
        newParams[0] = operation;
        System.arraycopy(params, 0, newParams, 1, params.length);
        logger.info("[{}] {}", newParams);
    }
}