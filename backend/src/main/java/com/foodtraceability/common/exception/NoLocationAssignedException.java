package com.foodtraceability.common.exception;

/**
 * 未分配归属位置异常（P1-USER-LOCATION-001）。
 *
 * <p><b>语义</b>：当前登录用户没有任何归属位置（{@code users.location_id} 为 null），
 * 或归属位置不满足该操作的位置类型要求（如仓库员工访问门店单据，见 design-002 §7）。
 *
 * <p><b>更名来源</b>：-001 §6 的 {@code NoStoreAssignedException} → -002 §5 修订二统一更名
 * {@code NoLocationAssignedException}，语义从"未分配门店"扩为"未分配位置（门店/仓库）"。
 *
 * <p><b>错误码</b>：403（拒绝），文案区分两种场景之一——「用户归属缺失」。
 * 继承 {@link BusinessException} 以复用既有全局异常处理器注册
 * （-002 §5 修订二：**全局异常处理器注册不变**）。
 */
public class NoLocationAssignedException extends BusinessException {

    /** 错误码：403（拒绝） */
    public static final Integer CODE = 403;

    /** 默认文案：用户归属缺失场景 */
    public static final String DEFAULT_MESSAGE = "当前用户未分配位置，请联系管理员在用户管理中分配";

    public NoLocationAssignedException() {
        super(CODE, DEFAULT_MESSAGE);
    }

    public NoLocationAssignedException(String message) {
        super(CODE, message);
    }
}
