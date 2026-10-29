package com.foodtraceability.common.exception;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.ExceptionMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.UUID;

/**
 * 全局异常处理器
 * 统一处理系统中的异常情况
 *
 * 安全与排错平衡说明：
 * - 业务异常、参数校验异常等已知异常：直接返回明确错误信息，便于前端提示用户。
 * - 非预期运行时异常/服务器内部错误：返回统一友好提示，并附带唯一追踪ID；
 *   详细异常类型、消息、堆栈、请求路径均记录到服务端日志，供运维排查。
 * - 所有敏感信息（SQL 语句、数据库连接、密钥等）不会直接返回给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TRACE_ID_KEY = "traceId";
    private static final String GENERIC_SERVER_ERROR = "系统繁忙，请稍后重试";

    private final ExceptionMonitoringService exceptionMonitoringService;

    public GlobalExceptionHandler(ExceptionMonitoringService exceptionMonitoringService) {
        this.exceptionMonitoringService = exceptionMonitoringService;
    }

    /**
     * 当前运行环境（从配置文件读取）
     */
    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * 判断是否为生产环境
     */
    private boolean isProduction() {
        return "prod".equalsIgnoreCase(activeProfile) ||
               "production".equalsIgnoreCase(activeProfile);
    }

    /**
     * 生成唯一追踪ID（用于关联客户端提示与服务器日志）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /**
     * 获取当前请求路径与方法（用于日志记录）
     */
    private String getRequestInfo() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "请求信息不可用";
        }
        HttpServletRequest request = attributes.getRequest();
        String uri = Optional.ofNullable(request.getRequestURI()).orElse("未知URI");
        String method = Optional.ofNullable(request.getMethod()).orElse("未知METHOD");
        return method + " " + uri;
    }

    /**
     * 记录非预期服务器错误到日志与监控服务
     *
     * @param e       异常对象
     * @param traceId 追踪ID
     * @param source  异常来源标识
     */
    private void logServerError(Exception e, String traceId, String source) {
        MDC.put(TRACE_ID_KEY, traceId);
        try {
            log.error("【{}】非预期异常 | traceId={} | 请求={} | 异常类型={} | 异常消息={}",
                    source,
                    traceId,
                    getRequestInfo(),
                    e.getClass().getName(),
                    e.getMessage(),
                    e);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
        recordExceptionToMonitoring(e);
    }

    /**
     * 构建带追踪ID的用户友好错误消息
     */
    private String buildUserMessage(String traceId) {
        return GENERIC_SERVER_ERROR + "（追踪ID：" + traceId + "，请联系运维排查）";
    }

    /**
     * 处理BusinessException异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        Integer httpStatus = resolveHttpStatus(e);
        if (httpStatus != null) {
            // 业务可识别的冲突/越权类：真实 HTTP 状态 + 中文业务原文（不套 500 兜底、不带 traceId）
            return ResponseEntity.status(httpStatus).body(Result.error(httpStatus, e.getMessage()));
        }
        // 其余业务异常保持历史行为（HTTP 200 + 业务 code + 原文），避免影响存量前端判定
        return ResponseEntity.ok(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 解析 BusinessException 应映射的 HTTP 状态码。
     *
     * <p>只对「业务可识别的冲突/越权类」生效，<b>不</b>把全部 BusinessException 一刀切成 409：</p>
     * <ul>
     *   <li>异常显式携带 {@link BusinessException#getHttpStatus()} → 用它。**四眼/权限类按 httpStatus 字段映射**：
     *       业务校验失败=400，状态冲突（如调节表 V8 四眼门禁、并发冲正）=409，权限不足=403；
     *       具体以各 Service 抛出的 httpStatus 为准（P3-3 已把调节表 V8 四眼从 400 改为 409）；</li>
     *   <li>业务 code 恰为 409（历史写法 {@code new BusinessException(409, ...)}）→ 视为冲突 409；</li>
     *   <li>其它（400/404/业务码 …）→ 返回 null，走历史 200 + code 语义。</li>
     * </ul>
     */
    private Integer resolveHttpStatus(BusinessException e) {
        // ★ X-2a（Lead 2026-10-30）：400 / 403 / 404 映射真 HTTP 状态。
        //   修前只映射 409（ConflictException），其余一律 null → HTTP 200 + body.code，
        //   前端于是有两套判断路径（HTTP 与 body.code）—— 今天所有误判都源于「两套表达」。
        //   ⚠ **401 暂不映射**：管理端 PROD 收到真 401 会走 handleSessionExpired（静默刷新+重放），
        //     那 12 处业务 401 是否真是「登录失效」尚未审计 → 现保持 200 + code（临时例外）。
        if (e.getHttpStatus() != null) {
            return e.getHttpStatus();
        }
        if (e.getCode() != null && e.getCode() == ConflictException.CONFLICT) {
            return ConflictException.CONFLICT;
        }
        if (e.getCode() != null) {
            switch (e.getCode()) {
                case 400:
                case 403:
                case 404:
                    return e.getCode();
                default:
                    break;
            }
        }
        // 其它业务码（含 401 临时例外、5xx 类业务码）→ 仍走 HTTP 200 + body.code 历史语义
        return null;
    }

    /**
     * 处理MethodArgumentNotValidException异常（@Validated注解验证失败）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        String message = "请求参数验证失败";
        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            message = fieldError.getDefaultMessage();
        }
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理BindException异常（@ModelAttribute注解验证失败）
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        log.error("BindException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        String message = "请求参数验证失败";
        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            message = fieldError.getDefaultMessage();
        }
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理ConstraintViolationException异常（@Valid注解验证失败）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("ConstraintViolationException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        String message = e.getConstraintViolations().iterator().next().getMessage();
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理MissingServletRequestParameterException异常（缺少请求参数）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("MissingServletRequestParameterException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        String message = "缺少必填参数：" + e.getParameterName();
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理MethodArgumentTypeMismatchException异常（请求参数类型不匹配）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("MethodArgumentTypeMismatchException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        String message = "参数类型不匹配：" + e.getName();
        return Result.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理NoHandlerFoundException异常（资源不存在）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<?>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("NoHandlerFoundException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        // ★ ①（Lead 2026-10-29）：路径不存在 → **真 HTTP 404**（修前是 200 + body.code=404，前端会当成成功）
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(HttpStatus.NOT_FOUND.value(), "资源不存在"));
    }

    /**
     * 处理NoResourceFoundException异常（静态资源不存在）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.error("NoResourceFoundException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        // ★ ①（Lead 2026-10-29）：同上，真 HTTP 404
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(HttpStatus.NOT_FOUND.value(), "资源不存在"));
    }

    /**
     * 处理HttpRequestMethodNotSupportedException异常（请求方法不允许）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        return Result.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不允许");
    }

    /**
     * 处理AccessDeniedException异常（权限不足）
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public org.springframework.http.ResponseEntity<Result<?>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        log.error("AccessDeniedException: {}", e.getMessage(), e);
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.error("Current user: {}, authorities: {}", auth.getName(), auth.getAuthorities());
        } else {
            log.error("No authentication found in SecurityContext");
        }
        recordExceptionToMonitoring(e);

        return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(HttpStatus.FORBIDDEN.value(), "权限不足，无法访问该资源"));
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public Result<?> handleAuthenticationException(org.springframework.security.core.AuthenticationException e) {
        log.error("AuthenticationException: {}", e.getMessage());
        recordExceptionToMonitoring(e);

        return Result.error(HttpStatus.UNAUTHORIZED.value(), "认证失败，请重新登录");
    }

    /**
     * 处理凭证错误异常
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public Result<?> handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException e) {
        log.error("BadCredentialsException: {}", e.getMessage());
        recordExceptionToMonitoring(e);

        return Result.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
    }

    /**
     * 处理NullPointerException异常（空指针异常）
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        String traceId = generateTraceId();
        logServerError(e, traceId, "NullPointerException");
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), buildUserMessage(traceId));
    }

    /**
     * 处理IllegalArgumentException异常（非法参数异常）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage(), e);
        recordExceptionToMonitoring(e);

        return Result.error(HttpStatus.BAD_REQUEST.value(), "非法参数：" + e.getMessage());
    }

    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        String traceId = generateTraceId();
        logServerError(e, traceId, "RuntimeException");
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), buildUserMessage(traceId));
    }

    /**
     * 处理Exception异常（所有未被捕获的异常）
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        String traceId = generateTraceId();
        logServerError(e, traceId, "Exception");
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), buildUserMessage(traceId));
    }

    /**
     * 记录异常到监控服务
     */
    private void recordExceptionToMonitoring(Exception e) {
        try {
            ServletRequestAttributes attributes = Optional.ofNullable(
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                    .orElse(null);

            String requestUri = "未知";
            String method = "未知";

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                requestUri = Optional.ofNullable(request.getRequestURI()).orElse("未知");
                method = Optional.ofNullable(request.getMethod()).orElse("未知");
            }

            exceptionMonitoringService.recordException(e, requestUri, method);
        } catch (Exception ex) {
            log.error("记录异常到监控服务失败: {}", ex.getMessage());
        }
    }
}
