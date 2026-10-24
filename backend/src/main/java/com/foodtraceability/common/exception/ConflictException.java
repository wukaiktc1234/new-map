package com.foodtraceability.common.exception;

/**
 * 业务冲突异常（HTTP 409 Conflict）。
 *
 * <p>用于「请求本身合法，但与当前数据状态冲突」的场景：</p>
 * <ul>
 *   <li>乐观锁/版本冲突重试耗尽（如 {@code AtomicBalanceUpdater#addBalance}）；</li>
 *   <li>状态机冲突（如「当前状态不允许存行」，或并发导致状态已被他人变更）。</li>
 * </ul>
 *
 * <p>message 为面向用户的中文业务文案，{@code GlobalExceptionHandler} 会以
 * HTTP 409 + 原文返回，<b>不</b>经过 500 的「系统繁忙 + traceId」兜底。</p>
 */
public class ConflictException extends BusinessException {

    /** 冲突类统一业务码 / HTTP 状态码 */
    public static final int CONFLICT = 409;

    public ConflictException(String message) {
        super(CONFLICT, message, CONFLICT);
    }

    public ConflictException(String message, Throwable cause) {
        super(CONFLICT, message, CONFLICT);
        if (cause != null) {
            initCause(cause);
        }
    }
}
