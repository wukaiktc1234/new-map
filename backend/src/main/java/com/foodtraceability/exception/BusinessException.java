package com.foodtraceability.exception;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    // 错误代码
    private int code;

    /**
     * 构造方法
     * @param code 错误代码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造方法
     * @param message 错误消息
     * @param cause 异常原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    /**
     * 构造方法
     * @param code 错误代码
     * @param message 错误消息
     * @param cause 异常原因
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(final int code) {
        this.code = code;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "BusinessException(code=" + this.getCode() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BusinessException)) return false;
        final BusinessException other = (BusinessException) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!super.equals(o)) return false;
        if (this.getCode() != other.getCode()) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof BusinessException;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        result = result * PRIME + this.getCode();
        return result;
    }
}
