package com.foodtraceability.scheduler;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行结果封装对象
 * 统一封装所有任务执行器的返回结果，包含执行状态、消息、数据等
 */
public class TaskExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 执行是否成功 */
    private boolean success;

    /** 结果消息（成功时为描述信息，失败时为错误信息） */
    private String message;

    /** 结果数据（JSON格式或具体业务数据） */
    private Object data;

    /** 执行开始时间 */
    private LocalDateTime startTime;

    /** 执行结束时间 */
    private LocalDateTime endTime;

    /** 执行耗时（毫秒） */
    private long durationMs;

    /** 错误异常信息 */
    private Throwable error;

    /** 重试建议：true表示可以重试，false表示不可重试 */
    private boolean retryable = true;

    // ==================== 构造方法 ====================

    public TaskExecutionResult() {
        this.startTime = LocalDateTime.now();
    }

    public TaskExecutionResult(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
        this.endTime = LocalDateTime.now();
        this.durationMs = calculateDuration();
    }

    public TaskExecutionResult(boolean success, String message, Object data) {
        this(success, message);
        this.data = data;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功结果
     * @param message 成功消息
     * @return 执行结果
     */
    public static TaskExecutionResult success(String message) {
        return new TaskExecutionResult(true, message);
    }

    /**
     * 创建带数据的成功结果
     * @param message 成功消息
     * @param resultData 结果数据
     * @return 执行结果
     */
    public static TaskExecutionResult success(String message, Object resultData) {
        return new TaskExecutionResult(true, message, resultData);
    }

    /**
     * 创建失败结果
     * @param message 失败消息
     * @return 执行结果
     */
    public static TaskExecutionResult failure(String message) {
        return new TaskExecutionResult(false, message);
    }

    /**
     * 创建带异常的失败结果
     * @param message 失败消息
     * @param throwable 异常对象
     * @return 执行结果
     */
    public static TaskExecutionResult failure(String message, Throwable throwable) {
        TaskExecutionResult result = new TaskExecutionResult(false, message);
        result.setError(throwable);
        return result;
    }

    /**
     * 创建不可重试的失败结果（用于严重错误，如配置错误）
     * @param message 失败消息
     * @return 执行结果
     */
    public static TaskExecutionResult nonRetryableFailure(String message) {
        TaskExecutionResult result = new TaskExecutionResult(false, message);
        result.setRetryable(false);
        return result;
    }

    // ==================== Getter/Setter ====================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (this.startTime != null && this.endTime != null) {
            this.durationMs = calculateDuration();
        }
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 计算执行耗时（毫秒）
     */
    private long calculateDuration() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }

    /**
     * 标记执行结束并计算耗时
     */
    public void markCompleted() {
        this.endTime = LocalDateTime.now();
        this.durationMs = calculateDuration();
    }

    @Override
    public String toString() {
        return "TaskExecutionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", durationMs=" + durationMs +
                ", retryable=" + retryable +
                (error != null ? ", error=" + error.getClass().getSimpleName() : "") +
                '}';
    }
}
