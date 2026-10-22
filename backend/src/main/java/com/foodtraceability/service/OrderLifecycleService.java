package com.foodtraceability.service;

/**
 * 订单生命周期统一入口。
 *
 * <p>背景（2026-10-23 状态机断裂修复）：此前「完成制作」只在后厨端更新 {@code kitchen_order}，
 * 主订单表 {@code orders} 完全不动，导致 POS / 后厨 / 管理端三端状态不一致；
 * 同时完成事件在 Controller 层发布，因无事务边界被 {@code @TransactionalEventListener(AFTER_COMMIT)}
 * 静默丢弃，财务/应收/成本联动全部失效。</p>
 *
 * <p>规则：任何端的订单状态跃迁必须经由本入口，禁止各端自行散写。</p>
 */
public interface OrderLifecycleService {

    /**
     * 后厨「完成制作」闭环（口径 A：完成制作即出餐、即扣料）。
     *
     * <p>单一事务内完成：后厨单置 completed（含追溯码生成）→ 扣减原料库存（幂等）→
     * 主订单状态置已完成并解锁桌台 → 发布 {@code OrderCompletedEvent}。</p>
     *
     * @param kitchenOrderId 后厨单号（KO...）
     * @return true=完成成功；false=后厨单状态不允许完成
     */
    boolean onKitchenCompleted(String kitchenOrderId);
}
