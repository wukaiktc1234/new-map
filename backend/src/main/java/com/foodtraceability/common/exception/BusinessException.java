package com.foodtraceability.common.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 期望的 HTTP 状态码（可空）。
     *
     * <p>为 null 时保持历史行为（HTTP 200 + 业务 code）；非 null 时由
     * {@code GlobalExceptionHandler} 映射成对应 HTTP 状态，让前端能直接区分类别
     * （409 冲突 / 403 越权 / 400 业务违规）。</p>
     */
    private Integer httpStatus;

    /**
     * 构造函数
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 构造函数
     * @param errorCode 错误码枚举
     * @param message 错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
    }

    /**
     * 构造函数：携带期望 HTTP 状态码（业务可识别的冲突/越权类）。
     *
     * @param code       业务错误码
     * @param message    业务文案（原样返回前端）
     * @param httpStatus 期望的 HTTP 状态码（如 409 / 400 / 403）
     */
    public BusinessException(Integer code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 构造函数：错误码枚举 + 期望 HTTP 状态码。
     */
    public BusinessException(ErrorCode errorCode, String message, int httpStatus) {
        super(message);
        this.code = errorCode.getCode();
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 获取期望 HTTP 状态码（可空）
     */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /**
     * 设置期望 HTTP 状态码
     */
    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 设置错误码
     * @param code 错误码
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * 获取错误信息
     * @return 错误信息
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 设置错误信息
     * @param message 错误信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 重写equals方法
     * @param o 比较对象
     * @return 是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BusinessException that = (BusinessException) o;

        if (getCode() != null ? !getCode().equals(that.getCode()) : that.getCode() != null) return false;
        return getMessage() != null ? getMessage().equals(that.getMessage()) : that.getMessage() == null;
    }

    /**
     * 重写hashCode方法
     * @return hashCode值
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getCode() != null ? getCode().hashCode() : 0);
        result = 31 * result + (getMessage() != null ? getMessage().hashCode() : 0);
        return result;
    }
}