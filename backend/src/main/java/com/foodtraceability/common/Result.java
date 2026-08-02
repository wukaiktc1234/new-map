package com.foodtraceability.common;

import java.io.Serializable;

/**
 * 统一API响应格式
 * @param <T> 响应数据类型
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    // 响应代码：0成功，非0失败
    private Integer code;
    // 响应消息
    private String message;
    // 响应数据
    private T data;
    // 时间戳
    private long timestamp;

    /**
     * 构造方法
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code(0).message("操作成功");
        return result;
    }

    /**
     * 成功响应，带数据
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code(0).message("操作成功").data(data);
        return result;
    }

    /**
     * 成功响应，带消息和数据
     * @param data 响应数据
     * @param message 响应消息
     * @param <T> 响应数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.code(0).message(message).data(data);
        return result;
    }

    /**
     * 失败响应
     * @param <T> 响应数据类型
     * @param code 错误代码
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.code(code).message(message);
        return result;
    }

    /**
     * 失败响应，使用默认错误代码
     * @param <T> 响应数据类型
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    /**
     * 失败响应，带数据
     * @param <T> 响应数据类型
     * @param message 错误消息
     * @param data 错误详情数据
     * @return 失败响应对象
     */
    public static <T> Result<T> error(String message, T data) {
        Result<T> result = new Result<>();
        result.code(500).message(message).data(data);
        return result;
    }

    /**
     * 失败响应，带代码和数据
     * @param <T> 响应数据类型
     * @param code 错误代码
     * @param message 错误消息
     * @param data 错误详情数据
     * @return 失败响应对象
     */
    public static <T> Result<T> error(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.code(code).message(message).data(data);
        return result;
    }

    /**
     * 失败响应，使用默认错误代码和消息
     * @param <T> 响应数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error() {
        return error(500, "操作失败");
    }

    /**
     * 设置响应代码
     * @param code 响应代码
     * @return 当前响应对象
     */
    public Result<T> code(Integer code) {
        this.code = code;
        return this;
    }

    /**
     * 设置响应消息
     * @param message 响应消息
     * @return 当前响应对象
     */
    public Result<T> message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 设置响应数据
     * @param data 响应数据
     * @return 当前响应对象
     */
    public Result<T> data(T data) {
        this.data = data;
        return this;
    }

    /**
     * 判断是否成功
     * @return true-成功，false-失败
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 0;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public T getData() {
        return this.data;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setCode(final Integer code) {
        this.code = code;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setData(final T data) {
        this.data = data;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Result)) return false;
        final Result<?> other = (Result<?>) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTimestamp() != other.getTimestamp()) return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$data = this.getData();
        final java.lang.Object other$data = other.getData();
        if (this$data == null ? other$data != null : !this$data.equals(other$data)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Result;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $timestamp = this.getTimestamp();
        result = result * PRIME + (int) ($timestamp >>> 32 ^ $timestamp);
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $data = this.getData();
        result = result * PRIME + ($data == null ? 43 : $data.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Result(code=" + this.getCode() + ", message=" + this.getMessage() + ", data=" + this.getData() + ", timestamp=" + this.getTimestamp() + ")";
    }
}
