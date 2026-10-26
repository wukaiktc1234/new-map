package com.foodtraceability.common.finance;

/**
 * 资金流水类别（fund_flows.flow_category）**唯一权威定义**（P5 / NEW-21）。
 *
 * <p>历史教训：旧代码在 {@code PaymentServiceImpl} 里用字面量 2 表示「采购付款」，
 * 而 P4-5 把 2 定义为「退款」，两者冲突会让退款巡检（A30/A31）把采购付款误算成退款。
 * 因此：**新代码禁止写字面量分类号，一律引用本类常量**。</p>
 *
 * <p>取值表：</p>
 * <ul>
 *   <li>{@link #SALE}=1 —— POS 销售入账（收入侧）</li>
 *   <li>{@link #REFUND}=2 —— 退款出账（P4-5 专用）；<b>2 不再被采购付款复用（NEW-21）</b></li>
 *   <li>{@link #INTERNAL_TRANSFER}=5 —— 存款/内部转账（存行、账户间划转）</li>
 *   <li>{@link #PURCHASE_PAYMENT}=6 —— 采购付款（付款单）</li>
 *   <li>{@link #PURCHASE_PAYMENT_RETURN}=7 —— 采购付款红冲/作废的反向收入流水（direction=1，款退回账户）</li>
 * </ul>
 */
public final class FundFlowCategory {

    /** 1 = POS 销售入账（收入侧） */
    public static final int SALE = 1;

    /** 2 = 退款出账（退款专用，不复用；采购付款已迁至 6，见 NEW-21） */
    public static final int REFUND = 2;

    /** 5 = 存款/内部转账（存行、账户间划转） */
    public static final int INTERNAL_TRANSFER = 5;

    /** 6 = 采购付款（付款单出账）—— NEW-21：旧代码曾用 2，已迁出 */
    public static final int PURCHASE_PAYMENT = 6;

    /**
     * 7 = 采购付款**红冲/作废**生成的**反向收入流水**（direction=1：付款单作废后款项退回账户）。
     *
     * <p><b>不是</b>采购退货退款（那是供应商侧的应收冲减，走应付/采购域，不在资金流水这边）。</p>
     */
    public static final int PURCHASE_PAYMENT_RETURN = 7;

    private FundFlowCategory() {
    }
}
