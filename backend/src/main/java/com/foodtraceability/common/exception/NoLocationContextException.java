package com.foodtraceability.common.exception;

/**
 * 位置上下文缺失异常（P1-USER-LOCATION-001）。
 *
 * <p><b>语义</b>：<b>操作对象</b>缺位置上下文——请求未显式携带 locationId，且无法从操作对象
 * 推导出位置（design-002 §5 修订二的解析链第 3 步）。
 *
 * <p>与 {@link NoLocationAssignedException} 的区别（-002 §5 修订二明确区分）：
 * <ul>
 *   <li>{@code NoLocationAssignedException} = <b>用户归属缺失</b>（"当前用户未分配位置"）</li>
 *   <li>{@code NoLocationContextException} = <b>操作对象缺上下文</b>（"无法确定操作的位置上下文"）</li>
 * </ul>
 *
 * <p><b>错误码</b>：400。继承 {@link BusinessException} 复用既有全局异常处理器注册。
 */
public class NoLocationContextException extends BusinessException {

    /** 错误码：400 */
    public static final Integer CODE = 400;

    /** 默认文案：操作对象缺位置上下文场景 */
    public static final String DEFAULT_MESSAGE = "无法确定操作的位置上下文";

    public NoLocationContextException() {
        super(CODE, DEFAULT_MESSAGE);
    }

    public NoLocationContextException(String message) {
        super(CODE, message);
    }
}
