package com.foodtraceability.service;

/**
 * POS 叫号号（callNumber）服务。
 *
 * <p>叫号号与订单号是两套编号：
 * <ul>
 *   <li>订单号 {@code T20261023031} —— 财务/对账口径，保持不变；</li>
 *   <li>叫号号 {@code A001} —— 顾客侧口径（KFC 式），按门店每天从 1 自增、跨天重置。</li>
 * </ul>
 *
 * <p>取号入口刻意放在状态机之外：POS 下单服务收尾方法（{@code PosOrderCreateServiceImpl}）、
 * 支付成功旁路（{@code PosOrderPaymentServiceImpl}）、叫号屏同步（{@code CallNumberServiceImpl}）
 * 三处共用本服务，任何一处先到达即取号，后续调用读回同一个号（幂等）。</p>
 */
public interface PosCallNumberService {

    /**
     * 确保订单拥有叫号号（幂等）。
     *
     * @param orderId   订单ID（orders 表主键）
     * @param storeId   门店ID（号段按门店隔离，为空按 0 处理）
     * @param orderType 订单类型：0堂食(A) 1外卖(B) 2自提(C)，其它按 A
     * @return 叫号号，如 {@code A001}；订单不存在或取号失败返回 {@code null}
     */
    String ensureCallNumber(String orderId, Long storeId, Integer orderType);

    /**
     * 确保订单拥有叫号号（门店/类型从 orders 表自动解析；orders 无该行则返回 null，不浪费号段）。
     */
    String ensureCallNumber(String orderId);

    /** 只读：查询订单已落库的叫号号，不做取号 */
    String peekCallNumber(String orderId);

    /** 把已取号的叫号号同步到叫号屏记录（call_record），幂等 */
    void syncToCallRecord(String orderId, String callNumber);
}
