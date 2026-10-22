package com.foodtraceability.service.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.KitchenOrder;
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

    public OrderLifecycleServiceImpl(KitchenOrderService kitchenOrderService,
                                     OrderNewService orderNewService,
                                     ApplicationEventPublisher applicationEventPublisher) {
        this.kitchenOrderService = kitchenOrderService;
        this.orderNewService = orderNewService;
        this.applicationEventPublisher = applicationEventPublisher;
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
        publishCompletedEvent(kitchenOrder);

        log.info("订单生命周期：完成制作闭环成功, kitchenOrderId={}, orderId={}",
                kitchenOrderId, kitchenOrder.getOrderId());
        return true;
    }

    /**
     * 构造并发布订单完成事件。金额字段单位为「元」（事件语义），后厨单 totalAmount 亦按元传入。
     */
    private void publishCompletedEvent(KitchenOrder kitchenOrder) {
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

        if (kitchenOrder.getTotalAmount() != null) {
            // 后厨单 totalAmount 为元（BigDecimal）；事件语义同为元
            BigDecimal amount = kitchenOrder.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP);
            event.setOrderAmount(amount);
            event.setActualAmount(amount);
        }

        applicationEventPublisher.publishEvent(event);
        log.info("订单完成事件已发布（事务内）, kitchenOrderId={}, orderId={}",
                kitchenOrder.getKitchenOrderId(), kitchenOrder.getOrderId());
    }
}
