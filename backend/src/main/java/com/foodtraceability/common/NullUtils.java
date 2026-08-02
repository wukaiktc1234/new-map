package com.foodtraceability.common;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 空值处理工具类
 * 提供Optional包装和空值检查的方法
 */
public class NullUtils {

    /**
     * 将可能为null的值包装为Optional
     * @param value 可能为null的值
     * @param <T> 值的类型
     * @return Optional包装后的值
     */
    public static <T> Optional<T> ofNullable(T value) {
        return Optional.ofNullable(value);
    }

    /**
     * 获取值，如果为null则返回默认值
     * @param value 可能为null的值
     * @param defaultValue 默认值
     * @param <T> 值的类型
     * @return 非null的值
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    /**
     * 获取值，如果为null则通过Supplier获取默认值
     * @param value 可能为null的值
     * @param supplier 默认值提供者
     * @param <T> 值的类型
     * @return 非null的值
     */
    public static <T> T getOrElseGet(T value, Supplier<? extends T> supplier) {
        return Optional.ofNullable(value).orElseGet(supplier);
    }

    /**
     * 获取值，如果为null则抛出异常
     * @param value 可能为null的值
     * @param exceptionSupplier 异常提供者
     * @param <T> 值的类型
     * @param <X> 异常类型
     * @return 非null的值
     * @throws X 如果值为null
     */
    public static <T, X extends Throwable> T getOrElseThrow(T value, Supplier<? extends X> exceptionSupplier) throws X {
        return Optional.ofNullable(value).orElseThrow(exceptionSupplier);
    }

    /**
     * 检查值是否为null
     * @param value 可能为null的值
     * @return 如果值为null则返回true，否则返回false
     */
    public static boolean isNull(Object value) {
        return value == null;
    }

    /**
     * 检查值是否不为null
     * @param value 可能为null的值
     * @return 如果值不为null则返回true，否则返回false
     */
    public static boolean nonNull(Object value) {
        return value != null;
    }

    /**
     * 检查字符串是否为null或空
     * @param str 可能为null的字符串
     * @return 如果字符串为null或空则返回true，否则返回false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 检查字符串是否不为null且不为空
     * @param str 可能为null的字符串
     * @return 如果字符串不为null且不为空则返回true，否则返回false
     */
    public static boolean nonEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * 安全地获取对象的属性值
     * @param obj 对象
     * @param getter 属性获取器
     * @param defaultValue 默认值
     * @param <T> 对象类型
     * @param <R> 属性类型
     * @return 属性值或默认值
     */
    public static <T, R> R safeGet(T obj, java.util.function.Function<T, R> getter, R defaultValue) {
        return Optional.ofNullable(obj).map(getter).orElse(defaultValue);
    }
}
