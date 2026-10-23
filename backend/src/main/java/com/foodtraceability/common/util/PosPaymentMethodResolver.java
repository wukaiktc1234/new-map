package com.foodtraceability.common.util;

/**
 * POS 支付方式编码 → 中文文案的统一解析器。
 *
 * <p><b>本系统存在两套支付方式编码，含义完全不同，禁止混用</b>：</p>
 * <ul>
 *   <li>新编码 —— {@code order_payment_records.payment_method}：
 *       1现金 / 2微信支付 / 3支付宝 / 4银行卡 / 5余额支付</li>
 *   <li>旧编码 —— {@code orders.payment_method}（与 {@code orders_legacy} 同源）：
 *       0微信支付 / 1支付宝 / 2现金 / 3银行卡 / 4余额支付</li>
 * </ul>
 *
 * <p>历史上 POS 详情/列表/资金流水详情各自翻译，出现过「新编码套旧映射」把现金显示成支付宝的问题。
 * 现统一走本类：先按新编码（支付记录）翻译，取不到再按旧编码（订单主表）兜底，
 * 两者都没有时给出明确提示文案，不再返回「未记录」这类废词。</p>
 */
public final class PosPaymentMethodResolver {

    /** 支付记录与订单主表都取不到支付方式时的提示文案（前端原样展示） */
    public static final String UNRECORDED = "（系统未记录流水，请查单）";

    private PosPaymentMethodResolver() {
    }

    /**
     * 新编码（order_payment_records.payment_method）→ 文案。
     *
     * @return 命中返回中文；null/未知返回 null（交由 {@link #resolve} 兜底）
     */
    public static String fromPaymentRecordCode(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 1 -> "现金";
            case 2 -> "微信支付";
            case 3 -> "支付宝";
            case 4 -> "银行卡";
            case 5 -> "余额支付";
            default -> null;
        };
    }

    /**
     * 旧编码（orders.payment_method / orders_legacy）→ 文案。
     *
     * @return 命中返回中文；null/未知返回 null
     */
    public static String fromOrderLegacyCode(Integer code) {
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

    /**
     * 统一解析：新编码优先（支付记录），旧编码兜底（订单主表），都没有给 {@link #UNRECORDED}。
     *
     * @param paymentRecordCode order_payment_records.payment_method（新编码，可空）
     * @param orderLegacyCode   orders.payment_method（旧编码，可空）
     */
    public static String resolve(Integer paymentRecordCode, Integer orderLegacyCode) {
        String byRecord = fromPaymentRecordCode(paymentRecordCode);
        if (byRecord != null) {
            return byRecord;
        }
        String byOrder = fromOrderLegacyCode(orderLegacyCode);
        return byOrder != null ? byOrder : UNRECORDED;
    }
}
