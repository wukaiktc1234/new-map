package com.foodtraceability.common.constant;

/**
 * 订单状态 / 支付状态码表 —— canonical 唯一口径（表 {@code orders}）。
 *
 * <p>2026-10-23 状态码归一：此前同一个 {@code order_status} 存在两套互相冲突的定义
 * （{@code Order.java} 注释为「0待支付/4已完成/5已取消」，{@code OrderQueryDTO} 为「0待确认/2已完成」），
 * 导致按状态统计时口径分裂（典型后果：日结把「已完成」当「退款」统计）。
 * 现以 {@code OrderNewServiceImpl} 的实际行为为准，统一为下表。</p>
 *
 * <p><b>order_status</b>：0待确认 · 1已确认 · 2已完成 · 3已取消 · 5全额退款</p>
 * <p><b>payment_status</b>：0未支付 · 1支付中 · 2已支付 · 3已退款</p>
 *
 * <p>注意：{@code orders_legacy} 表（实体 {@code Order}）仍使用旧码表
 * 「0待支付/1已支付/2待配送/3配送中/4已完成/5已取消/6退款中/7已退款」，
 * 任何新逻辑禁止再引用该表口径。</p>
 */
public final class OrderStatusCodes {

    private OrderStatusCodes() {
    }

    // ==================== order_status ====================

    /** 待确认（下单初始态） */
    public static final int ORDER_PENDING_CONFIRM = 0;
    /** 已确认（支付成功或人工确认后进入制作） */
    public static final int ORDER_CONFIRMED = 1;
    /** 已完成（后厨完成制作 / 管理端完成） */
    public static final int ORDER_COMPLETED = 2;
    /** 已取消 */
    public static final int ORDER_CANCELLED = 3;
    /** 全额退款 */
    public static final int ORDER_REFUNDED_FULL = 5;

    // ==================== payment_status ====================

    /** 未支付（下单初始态） */
    public static final int PAYMENT_UNPAID = 0;
    /** 支付中（已锁定，等待支付结果） */
    public static final int PAYMENT_PENDING = 1;
    /** 已支付 */
    public static final int PAYMENT_PAID = 2;
    /** 已退款 */
    public static final int PAYMENT_REFUNDED = 3;

    /**
     * 是否为计入营收的订单（营收口径：已支付）。
     */
    public static boolean isBillable(Integer paymentStatus) {
        return paymentStatus != null && paymentStatus == PAYMENT_PAID;
    }

    /**
     * 是否为「已完成但未支付」——应收挂账来源。
     */
    public static boolean isCompletedButUnpaid(Integer orderStatus, Integer paymentStatus) {
        return orderStatus != null && orderStatus == ORDER_COMPLETED
                && (paymentStatus == null || paymentStatus != PAYMENT_PAID);
    }
}
