package com.foodtraceability.common.util;

/**
 * POS 支付方式编码 → 中文文案的统一解析器（<b>单轨编码</b>）。
 *
 * <p>2026-10-24 起，全系统只有一套支付方式编码（唯一真相源）：</p>
 * <ul>
 *   <li>{@code order_payment_records.payment_method}（权威）与 {@code orders.payment_method}（冗余列）
 *       统一为：<b>0未记录 / 1现金 / 2微信支付 / 3支付宝 / 4银行卡 / 5余额支付</b>。</li>
 *   <li><b>0 = 未记录</b>：历史补账单渠道不可考，是合法值（不是「空」也不是「现金」），
 *       展示为 {@link #UNRECORDED}。</li>
 *   <li>历史旧编码（0微信/1支付宝/2现金/3银行卡/4余额）已由
 *       {@code scripts/db-fix/2026-10-24-unify-order-payment-method.sql} 全量迁移，
 *       并由 {@code ck_orders_payment_method_range CHECK (payment_method BETWEEN 0 AND 5)} 卡死。</li>
 * </ul>
 *
 * <p><b>兼容边界：</b>遗留表 {@code orders_legacy}（及读取它的 POS 历史单分支）仍是旧编码，
 * 只有那条链路允许调用 {@link #fromLegacyOrderCode(Integer)}；新表链路一律走 {@link #fromCode(Integer)}，
 * 不得再出现「按旧编码翻译新表字段」的猜测。</p>
 */
public final class PosPaymentMethodResolver {

    /** 支付记录与订单列都取不到支付方式时的占位文案（前端原样展示） */
    public static final String UNRECORDED = "（系统未记录流水，请查单）";

    private PosPaymentMethodResolver() {
    }

    /**
     * 统一编码 → 文案：0未记录 / 1现金 / 2微信支付 / 3支付宝 / 4银行卡 / 5余额支付。
     *
     * <p>0 是「未记录」的<b>确定值</b>（历史补账单），不是空值：这里直接返回占位文案，
     * 使 {@link #resolve} 不会把它当成「没取到」而继续往下猜。</p>
     *
     * @param code 编码（可为 null）
     * @return 命中返回文案；null 或超出 0..5 返回 null（交由 {@link #resolve} 兜底）
     */
    public static String fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> UNRECORDED;
            case 1 -> "现金";
            case 2 -> "微信支付";
            case 3 -> "支付宝";
            case 4 -> "银行卡";
            case 5 -> "余额支付";
            default -> null;
        };
    }

    /**
     * 单一真相源解析：支付记录（新编码 0..5）优先；订单列 {@code orders.payment_method}
     * <b>按同一套新编码解释</b>；两者都取不到才返回 {@link #UNRECORDED}。
     *
     * <p>注意：0（未记录）是有效取值，会被 {@link #fromCode} 转成占位文案并在此处直接返回，
     * 不会「穿透」去读订单列，避免把「未记录」又猜成别的渠道。</p>
     *
     * @param paymentRecordCode order_payment_records.payment_method（权威，可空）
     * @param orderCode         orders.payment_method（同一套编码，可空）
     */
    public static String resolve(Integer paymentRecordCode, Integer orderCode) {
        String byRecord = fromCode(paymentRecordCode);
        if (byRecord != null) {
            return byRecord;
        }
        String byOrder = fromCode(orderCode);
        return byOrder != null ? byOrder : UNRECORDED;
    }

    /**
     * <b>【仅限遗留表 orders_legacy 分支使用】</b>旧编码 → 文案：
     * 0微信支付 / 1支付宝 / 2现金 / 3银行卡 / 4余额支付。
     *
     * <p>严禁用于 {@code orders}（新表）或 {@code order_payment_records}：新表已统一为新编码，
     * 用本方法翻译会把现金显示成支付宝。</p>
     */
    public static String fromLegacyOrderCode(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 0 -> "微信支付";
            case 1 -> "支付宝";
            case 2 -> "现金";
            case 3 -> "银行卡";
            case 4 -> "余额支付";
            default -> null;
        };
    }
}
