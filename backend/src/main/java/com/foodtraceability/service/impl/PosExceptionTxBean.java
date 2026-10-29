package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.finance.FundFlowCreateDTO;
import com.foodtraceability.entity.ExceptionOps;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.ExceptionOpsMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.PosCashAccountResolver;
import com.foodtraceability.service.pos.ResolveResult;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 异常单补收款的事务执行体（Lead 2026-10-29）。
 *
 * <p><b>为什么单独一个 Bean：</b>Service 内部 {@code this.doResolveInTx()} 不经过 Spring 代理，
 * {@code @Transactional} 会失效（本项目实测过）。</p>
 *
 * <p><b>账户解析必须在写任何资金相关表之前</b>：否则订单已置 paid 才发现没账户，回滚范围更大。</p>
 */
@Component
public class PosExceptionTxBean {

    private static final Logger log = LoggerFactory.getLogger(PosExceptionTxBean.class);

    /** 抄自 PosSaleFinanceRecorder:53 FLOW_DIRECTION_INCOME */
    private static final int FLOW_DIRECTION_INCOME = 1;
    /** 抄自 PosSaleFinanceRecorder:55 FLOW_CATEGORY_SALE */
    private static final int FLOW_CATEGORY_SALE = 1;

    private final OrderNewMapper orderNewMapper;
    private final OrderPaymentRecordNewMapper paymentRecordMapper;
    private final ExceptionOpsMapper exceptionOpsMapper;
    private final PosExceptionOpsAttempt opsAttempt;
    private final PosCashAccountResolver cashAccountResolver;
    private final FundFlowService fundFlowService;
    private final com.foodtraceability.service.pos.PosExceptionService self;
    /** 作废：kitchen_order 关联 + 库存释放（走 InventoryService，禁直接 UPDATE） */
    private final com.foodtraceability.mapper.KitchenOrderMapper kitchenOrderMapper;
    private final com.foodtraceability.service.InventoryService inventoryService;


    public PosExceptionTxBean(OrderNewMapper orderNewMapper,
                              OrderPaymentRecordNewMapper paymentRecordMapper,
                              ExceptionOpsMapper exceptionOpsMapper,
                              PosExceptionOpsAttempt opsAttempt,
                              PosCashAccountResolver cashAccountResolver,
                              FundFlowService fundFlowService,
                              @org.springframework.context.annotation.Lazy com.foodtraceability.service.pos.PosExceptionService self,
                              com.foodtraceability.mapper.KitchenOrderMapper kitchenOrderMapper,
                              @org.springframework.context.annotation.Lazy com.foodtraceability.service.InventoryService inventoryService) {
        this.orderNewMapper = orderNewMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.exceptionOpsMapper = exceptionOpsMapper;
        this.opsAttempt = opsAttempt;
        this.cashAccountResolver = cashAccountResolver;
        this.fundFlowService = fundFlowService;
        this.self = self;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.inventoryService = inventoryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResolveResult execute(String orderId, Long amountFen, String payType, Long cashReceivedFen,
                                 String idempotencyKey, String operatorId) {
        OrderNew order = orderNewMapper.selectOne(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getOrderId, orderId).eq(OrderNew::getDeleted, 0).last("LIMIT 1"));
        if (order == null) {
            return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "订单不存在或已删除：" + orderId);
        }

        long receivableFen = order.getFinalAmount() != null ? order.getFinalAmount()
                : (order.getTotalAmount() != null ? order.getTotalAmount() : 0L);
        if (amountFen == null || amountFen != receivableFen) {
            return ResolveResult.fail(ResolveResult.Status.AMOUNT_MISMATCH,
                    "金额不符：应收 " + receivableFen + " 分，请求 " + amountFen + " 分");
        }

        Integer paymentMethod = "CASH".equalsIgnoreCase(payType) ? 1 : 2;

        // ★ 账户解析必须在写任何资金表之前（Lead 2026-10-29 裁定 b）：解析不到 → 409 且整体回滚
        PosCashAccountResolver.Resolved pick = cashAccountResolver.resolve(
                order.getStoreId(), paymentMethod, orderId, "EXCEPTION_RESOLVE");
        if (pick == null) {
            log.warn("[METRIC] exception_resolve_no_account orderId={} storeId={} payType={}",
                    orderId, order.getStoreId(), payType);
            throw new BusinessException(40903, "本店未配置资金账户，无法入账，请联系店长配置");
        }

        // CAS：payment_status <> 2 且 order_status = 0（★ P0-C 2026-10-30：加 order_status 条件）
        int casRows = exceptionOpsMapper.casMarkPaid(orderId, amountFen, paymentMethod);
        if (casRows == 0) {
            // 区分语义：已取消/已退款 与 已被他端处理，文案必须不同（否则用户不知道该做什么）
            Integer osNow = exceptionOpsMapper.orderStatusOf(orderId);
            if (osNow != null && (osNow == 3 || osNow == 5)) {
                String label = osNow == 3 ? "已取消" : "已全额退款";
                log.info("[METRIC] exception_resolve_rejected_terminal_state orderId={} orderStatus={}", orderId, osNow);
                return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED,
                        "订单" + label + "，不能补收款（如顾客确实已付，请走订单重开/线下登记流程）");
            }
            return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "该订单已被其它终端处理，请刷新后重试");
        }

        // 支付流水（字段照抄 PosOrderPaymentServiceImpl:741-748）
        OrderPaymentRecordNew record = new OrderPaymentRecordNew();
        record.setOrderId(orderId);
        record.setPaymentMethod(paymentMethod);
        record.setPaymentAmount(amountFen);
        record.setTransactionNo(genTxnNo(paymentMethod));
        record.setPaymentTime(LocalDateTime.now());
        record.setOperatorId(SecurityUtils.getCurrentUserId());   // 抄自 :739/:747（userId，不是 employee_code）
        record.setRemark("POS 异常单补收款");
        if (cashReceivedFen != null) {
            record.setCashReceived(cashReceivedFen);
        }
        record.setCreateTime(LocalDateTime.now());
        paymentRecordMapper.insert(record);

        // 资金流水：编码照抄 PosSaleFinanceRecorder:184-192（改那边要同步这里）
        FundFlowCreateDTO dto = new FundFlowCreateDTO();
        dto.setAccountId(pick.accountId());
        dto.setFlowDirection(FLOW_DIRECTION_INCOME);
        dto.setFlowCategory(FLOW_CATEGORY_SALE);
        dto.setAmount(amountFen);
        dto.setBusinessDate(LocalDate.now());
        dto.setCounterpartyName("散客");
        dto.setRemark("POS 异常单补收款：" + order.getOrderNumber());
        dto.setOrderId(orderId);
        dto.setCreateUserId(SecurityUtils.getCurrentUserId() != null ? SecurityUtils.getCurrentUserId() : 0L);
        String flowNo = null;
        try {
            var vo = fundFlowService.create(dto);
            flowNo = vo != null ? vo.getFlowNo() : null;
        } catch (Exception e) {
            // 入账失败必须整体回滚：钱收了但账上没有 = 静默，绝不允许
            log.error("[METRIC] exception_resolve_flow_failed orderId={} err={}", orderId, e.getMessage());
            throw new BusinessException(40903, "资金入账失败，已回滚：" + e.getMessage());
        }

        // 异常单记录：走保存点，撞唯一键只回滚这一段
        ExceptionOps ops = new ExceptionOps();
        ops.setOrderId(orderId);
        ops.setOpType("RESOLVE");
        ops.setIdempotencyKey(idempotencyKey);
        ops.setAmountFen(amountFen);
        ops.setPayType(payType);
        ops.setCashReceivedFen(cashReceivedFen);
        ops.setOperatorId(operatorId);
        ops.setShiftId(order.getShiftId());
        ops.setFlowNo(flowNo);
        ops.setCreateTime(LocalDateTime.now());
        ops.setUpdateTime(LocalDateTime.now());
        ops.setDeleted(0);
        opsAttempt.insertWithinSavepoint(ops);

        return ResolveResult.ok("补收款成功");
    }

    /** 交易号前缀：抄自 PosOrderPaymentServiceImpl.generateTransactionId 的既有约定 */
    private String genTxnNo(Integer paymentMethod) {
        String prefix = paymentMethod != null && paymentMethod == 1 ? "CSH-" : "WX-";
        return prefix + System.currentTimeMillis();
    }

    /**
     * 异常单作废（VOID）—— Lead 2026-10-29。
     *
     * <p>与 resolve 的差别：**不产生任何资金流水**（fund_flows 零新增），金额相关列全部为 NULL。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResolveResult executeVoid(String orderId, String reason, String note,
                                     String idempotencyKey, String operatorId) {
        OrderNew order = orderNewMapper.selectOne(new LambdaQueryWrapper<OrderNew>()
                .eq(OrderNew::getOrderId, orderId).eq(OrderNew::getDeleted, 0).last("LIMIT 1"));
        if (order == null) {
            return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "订单不存在或已删除：" + orderId);
        }
        // 已支付单不能作废（钱已收，需要走退款而不是作废）
        Integer payStatus = exceptionOpsMapper.paymentStatusOf(orderId);
        if (payStatus != null && payStatus == 2) {
            return ResolveResult.fail(ResolveResult.Status.ALREADY_PAID, "该订单已收款，不能作废，请走退款流程");
        }

        // CAS：order_status -> 3（已取消），抄 OrderStatusCodes:31-32 / OrderMapper:92,:131
        int casRows = exceptionOpsMapper.casCancel(orderId);
        if (casRows == 0) {
            return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "该订单已被其它终端处理，请刷新后重试");
        }

        // kitchen_order 置 cancelled（按 order_id 关联，不是 order_number；@TableId 是 id）
        try {
            com.foodtraceability.entity.KitchenOrder ko = kitchenOrderMapper.selectOne(
                    new LambdaQueryWrapper<com.foodtraceability.entity.KitchenOrder>()
                            .eq(com.foodtraceability.entity.KitchenOrder::getOrderId, orderId).last("LIMIT 1"));
            if (ko != null) {
                com.foodtraceability.entity.KitchenOrder ku = new com.foodtraceability.entity.KitchenOrder();
                ku.setId(ko.getId());
                ku.setKitchenOrderId(ko.getKitchenOrderId());
                ku.setStatus("cancelled");
                ku.setCancelTime(LocalDateTime.now());
                ku.setCancelReason("异常单作废");
                ku.setMaterialLocked(0);
                kitchenOrderMapper.updateById(ku);
            }
        } catch (Exception e) {
            log.warn("[EXC-VOID] kitchen_order 取消失败 orderId={} err={}", orderId, e.getMessage());
        }

        // 库存释放：InventoryService **没有** releaseLockedByOrder（实测其接口只有 lockInventory /
        // unlockInventory(inventoryId, qty) / increaseStockAtLocation …）。按物料维度反查才能调 unlock，
        // 而 order_items 不带 location_id/material_id（同 ② 退款联动的已知缺口），故此处**不做猜测性释放**，
        // 只记 metric，交由 product 决策（与 dish_ingredient 映射缺失同一个待办）。
        log.warn("[METRIC] exception_void_inventory_release_skipped orderId={} reason=no_location_material_mapping", orderId);

        ExceptionOps ops = new ExceptionOps();
        ops.setOrderId(orderId);
        ops.setOpType("VOID");
        ops.setReason(reason);
        ops.setNote(note);
        ops.setIdempotencyKey(idempotencyKey);
        ops.setOperatorId(operatorId);
        ops.setShiftId(order.getShiftId());
        // VOID 不产生资金流水，金额与 flow_no 一律 NULL
        ops.setAmountFen(null);
        ops.setPayType(null);
        ops.setCashReceivedFen(null);
        ops.setFlowNo(null);
        ops.setCreateTime(LocalDateTime.now());
        ops.setUpdateTime(LocalDateTime.now());
        ops.setDeleted(0);
        opsAttempt.insertWithinSavepoint(ops);

        return ResolveResult.ok("作废成功");
    }
}
