package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.event.OrderCompletedEvent;
import com.foodtraceability.service.KitchenOrderService;
import com.foodtraceability.service.OrderLifecycleService;
import com.foodtraceability.service.OrderNewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * {@link OrderLifecycleService} 实现：后厨完成 → 扣料 → 主订单完成 → 事件发布，全部在同一事务内。
 *
 * <p><b>为什么不直接调 {@code OrderNewServiceImpl.completeOrder}：</b>
 * {@code completeOrder} 内部 {@code deductInventoryAndCalculateCost} 会按 BOM 调用
 * {@code inventoryService.decreaseStockAtLocation(...)} 扣库存；而口径 A 指定的
 * {@code deductMaterialsForServe} 同样会扣同一份库存（其内部第 6 步亦为 {@code decreaseStockAtLocation}）。
 * 两者叠加即<b>双扣</b>。故此处改用 {@code markCompletedFromKitchen}：只做状态跃迁 + 解锁桌台，
 * 库存唯一由 {@code deductMaterialsForServe} 扣减（该方法自带 {@code material_consumed} 原子幂等占位与失败审计）。
 * 若后续改为以 completeOrder 为唯一扣减源，只需把第 2、3 步对调并删除扣料调用。</p>
 */
@Service
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleServiceImpl.class);

    /** 后厨单已完成状态值（与 KitchenOrderServiceImpl.completeMake 保持一致）。 */
    private static final String STATUS_COMPLETED = "completed";

    private final KitchenOrderService kitchenOrderService;
    private final OrderNewService orderNewService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OrderNewMapper orderNewMapper;
    private final OrderPaymentRecordNewMapper orderPaymentRecordNewMapper;

    public OrderLifecycleServiceImpl(KitchenOrderService kitchenOrderService,
                                     OrderNewService orderNewService,
                                     ApplicationEventPublisher applicationEventPublisher,
                                     OrderNewMapper orderNewMapper,
                                     OrderPaymentRecordNewMapper orderPaymentRecordNewMapper) {
        this.kitchenOrderService = kitchenOrderService;
        this.orderNewService = orderNewService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.orderNewMapper = orderNewMapper;
        this.orderPaymentRecordNewMapper = orderPaymentRecordNewMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean onKitchenCompleted(String kitchenOrderId) {
        if (kitchenOrderId == null || kitchenOrderId.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "后厨单号不能为空");
        }

        KitchenOrder kitchenOrder = kitchenOrderService.getByKitchenOrderId(kitchenOrderId);
        if (kitchenOrder == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "后厨单不存在: " + kitchenOrderId);
        }

        // 幂等：已完成直接返回，不重复扣料
        if (STATUS_COMPLETED.equals(kitchenOrder.getStatus())) {
            log.info("完成制作幂等命中（后厨单已是 completed）: kitchenOrderId={}", kitchenOrderId);
            return true;
        }

        // 1) 后厨单 → completed（内含追溯码生成）
        boolean completed = kitchenOrderService.completeMake(kitchenOrderId);
        if (!completed) {
            log.warn("完成制作被拒绝（状态不允许）: kitchenOrderId={}, status={}",
                    kitchenOrderId, kitchenOrder.getStatus());
            return false;
        }

        // 2) 扣料（口径 A：完成制作即扣料；material_consumed 原子幂等占位，重复调用会被拒绝并审计）
        //    返回值为本次出库成本（分），用于第 2.5 步落 cost_record。
        String sourceRef = "KDS完成制作扣料 - 订单:" + kitchenOrder.getOrderNumber();
        long totalCostFen = orderNewService.deductMaterialsForServe(
                kitchenOrderId, sourceRef, kitchenOrder.getTrayCode());

        // 2.5) 成本落库（cost_record）：此前 KDS 完成路径从不写成本，日结只有收入没有成本
        orderNewService.persistOrderCostFromDeduction(kitchenOrder.getOrderId(), totalCostFen);

        // 3) 主订单联动：状态 → 已完成（未确认的先置已确认）+ 解锁桌台；不重复扣库存
        orderNewService.markCompletedFromKitchen(kitchenOrder.getOrderId());

        // 4) 事务内发布完成事件 —— 必须是本事务边界内发布，
        //    监听器 @TransactionalEventListener(AFTER_COMMIT) 才不会被静默丢弃
        publishCompletedEvent(kitchenOrder, totalCostFen);

        log.info("订单生命周期：完成制作闭环成功, kitchenOrderId={}, orderId={}",
                kitchenOrderId, kitchenOrder.getOrderId());
        return true;
    }

    /**
     * 构造并发布订单完成事件。
     *
     * <p>2026-10-23 修复：金额改为以 canonical {@code orders} 为唯一权威来源
     * （{@code orders.final_amount}，单位分 → 事件单位元）。
     * 原实现取 {@code kitchen_order.totalAmount}，而该列在后厨单创建时从未写入，
     * 导致事件金额恒为 null → 财务收入记录 0 → 应收挂账 0。</p>
     *
     * <p>关于 {@code materialCost}：**刻意不设置**。成本已由
     * {@code persistOrderCostFromDeduction} 在同一事务内写入 {@code cost_records}；
     * 而监听器 {@code recordOrderCost} 一旦发现 materialCost > 0 会再写一条，
     * 造成成本重复入账。此处仅通过 {@code totalCost} 携带成本供日志/下游参考。</p>
     */
    private void publishCompletedEvent(KitchenOrder kitchenOrder, long totalCostFen) {
        OrderCompletedEvent event = new OrderCompletedEvent();
        event.setEventId("EVT" + System.currentTimeMillis());
        event.setOrderId(kitchenOrder.getOrderId());
        event.setOrderNumber(kitchenOrder.getOrderNumber());
        event.setOrderType(kitchenOrder.getOrderType());
        event.setStoreId(kitchenOrder.getStoreId());
        event.setStoreName(kitchenOrder.getStoreName());
        event.setTableNumber(kitchenOrder.getTableNumber());
        event.setCompleteTime(LocalDateTime.now());
        event.setOrderTime(kitchenOrder.getCreateTime());

        // 金额：以 orders.final_amount 为准（分 → 元）
        BigDecimal amountYuan = BigDecimal.ZERO;
        OrderNew order = kitchenOrder.getOrderId() == null ? null
                : orderNewMapper.selectById(kitchenOrder.getOrderId());
        if (order != null && order.getFinalAmount() != null) {
            amountYuan = BigDecimal.valueOf(order.getFinalAmount())
                    .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
        } else if (kitchenOrder.getTotalAmount() != null) {
            // 兜底：orders 缺失时退回后厨单金额（可能为 null 场景，保持兼容）
            amountYuan = kitchenOrder.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        }
        event.setOrderAmount(amountYuan);
        event.setActualAmount(amountYuan);

        // 支付方式：取 order_payment_records 最早一条（支付流水）
        if (kitchenOrder.getOrderId() != null) {
            try {
                java.util.List<OrderPaymentRecordNew> payments =
                        orderPaymentRecordNewMapper.selectByOrderId(kitchenOrder.getOrderId());
                if (payments != null && !payments.isEmpty()
                        && payments.get(0).getPaymentMethod() != null) {
                    event.setPaymentMethod(payments.get(0).getPaymentMethod());
                }
            } catch (Exception e) {
                log.warn("读取支付方式失败（不影响主流程）: orderId={}, error={}",
                        kitchenOrder.getOrderId(), e.getMessage());
            }
        }

        // 成本（分 → 元），仅供参考；materialCost 保持 null 以避免重复入账
        if (totalCostFen > 0L) {
            event.setTotalCost(BigDecimal.valueOf(totalCostFen)
                    .divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP));
        }

        applicationEventPublisher.publishEvent(event);
        log.info("订单完成事件已发布（事务内）, kitchenOrderId={}, orderId={}, amountYuan={}, paymentMethod={}, totalCostYuan={}",
                kitchenOrder.getKitchenOrderId(), kitchenOrder.getOrderId(), amountYuan,
                event.getPaymentMethod(), event.getTotalCost());
    }
}
