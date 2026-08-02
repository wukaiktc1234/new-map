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