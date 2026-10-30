package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.dto.finance.FundFlowCreateDTO;
import com.foodtraceability.entity.ExceptionOps;
import com.foodtraceability.entity.KitchenOrder;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.OrderPaymentRecordNew;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.mapper.ExceptionOpsMapper;
import com.foodtraceability.mapper.KitchenOrderMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.mapper.OrderPaymentRecordNewMapper;
import com.foodtraceability.service.finance.FundFlowService;
import com.foodtraceability.service.finance.PosCashAccountResolver;
import com.foodtraceability.service.pos.ResolveResult;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 异常单补收款 / 作废的事务执行体（Lead 2026-10-29）。
 *
 * <p><b>为什么单独一个 Bean：</b>Service 内部 {@code this.doResolveInTx()} 不经过 Spring 代理，
 * {@code @Transactional} 会失效（本项目实测过）。</p>
 *
 * <p><b>账户解析必须在写任何资金相关表之前</b>：否则订单已置 paid 才发现没账户，回滚范围更大。</p>
 *
 * <p><b>R7 整改（2026-10-30，本类改动三处）</b></p>
 * <ol>
 *   <li><b>RT-3</b>：{@code casCancel} 补 {@code order_status = 0} 谓词；CAS 0 行时按当前状态返回 409
 *       （二次作废 → "订单已是【已取消】状态，无需重复作废"），而不是等唯一索引炸出 500；
 *       {@link DataIntegrityViolationException} 一律转成规范业务 409，底层 SQL 不出后端。</li>
 *   <li><b>RT-4</b>：厨房单取消失败<b>不再 try-warn 吞掉</b>，直接抛 → 整单作废回滚（事务内一致）；
 *       库存回补复用 {@link PosOrderStockRestorer}（与 TTL 关单同一份实现），不再"打 metric 跳过"。</li>
 *   <li>作废成功后的库存回补<b>只可能发生一次</b>：CAS（order_status 0→3）是唯一闸门。</li>
 * </ol>
 */
@Component
public class PosExceptionTxBean {

    private static final Logger log = LoggerFactory.getLogger(PosExceptionTxBean.class);

    /** 抄自 PosSaleFinanceRecorder:53 FLOW_DIRECTION_INCOME */
    private static final int FLOW_DIRECTION_INCOME = 1;
    /** 抄自 PosSaleFinanceRecorder:55 FLOW_CATEGORY_SALE */
    private static final int FLOW_CATEGORY_SALE = 1;

    /** 订单状态：0 待确认 · 1 已确认 · 2 已完成 · 3 已取消 · 5 全额退款（OrderStatusCodes:11） */
    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CONFIRMED = 1;
    private static final int ORDER_STATUS_COMPLETED = 2;
    private static final int ORDER_STATUS_CANCELLED = 3;
    private static final int ORDER_STATUS_REFUNDED = 5;

    /** 业务码 409：冲突（唯一键/状态机竞态），前端按 409 展示「刷新后重试」，不按 500 报障 */
    private static final int CODE_CONFLICT = 409;
    private static final int HTTP_CONFLICT = 409;

    private final OrderNewMapper orderNewMapper;
    private final OrderPaymentRecordNewMapper paymentRecordMapper;
    private final ExceptionOpsMapper exceptionOpsMapper;
    private final PosExceptionOpsAttempt opsAttempt;
    private final PosCashAccountResolver cashAccountResolver;
    private final FundFlowService fundFlowService;
    private final com.foodtraceability.service.pos.PosExceptionService self;
    /** 作废：kitchen_order 关联（失败必须回滚，见 RT-4） */
    private final KitchenOrderMapper kitchenOrderMapper;
    /** 作废：库存回补（与 TTL 关单同一份实现，见 RT-4） */
    private final PosOrderStockRestorer stockRestorer;


    public PosExceptionTxBean(OrderNewMapper orderNewMapper,
                              OrderPaymentRecordNewMapper paymentRecordMapper,
                              ExceptionOpsMapper exceptionOpsMapper,
                              PosExceptionOpsAttempt opsAttempt,
                              PosCashAccountResolver cashAccountResolver,
                              FundFlowService fundFlowService,
                              @org.springframework.context.annotation.Lazy com.foodtraceability.service.pos.PosExceptionService self,
                              KitchenOrderMapper kitchenOrderMapper,
                              PosOrderStockRestorer stockRestorer) {
        this.orderNewMapper = orderNewMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.exceptionOpsMapper = exceptionOpsMapper;
        this.opsAttempt = opsAttempt;
        this.cashAccountResolver = cashAccountResolver;
        this.fundFlowService = fundFlowService;
        this.self = self;
        this.kitchenOrderMapper = kitchenOrderMapper;
        this.stockRestorer = stockRestorer;
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
            if (osNow != null && (osNow == ORDER_STATUS_CANCELLED || osNow == ORDER_STATUS_REFUNDED)) {
                String label = osNow == ORDER_STATUS_CANCELLED ? "已取消" : "已全额退款";
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

        // 异常单记录：走保存点，撞唯一键只回滚这一段（RT-3：冲突已转成规范 409，不再漏 SQL 原文）
        ExceptionOps ops = new ExceptionOps();
        ops.setOrderId(orderId);
        ops.setOpType("RESOLVE");
        ops.setIdempotencyKey(idempotencyKey);
        ops.setAmountFen(amountFen);
        ops.setPayType(payType);
        ops.setCashReceivedFen(cashReceivedFen);
        ops.setOperatorId(String.valueOf(SecurityUtils.getCurrentUserId()));   // P0-D: 不信任传入值，自己取 SecurityContext
        ops.setShiftId(order.getShiftId());
        ops.setFlowNo(flowNo);
        ops.setCreateTime(LocalDateTime.now());
        ops.setUpdateTime(LocalDateTime.now());
        ops.setDeleted(0);
        insertOpsOrThrow409(ops, orderId, "RESOLVE");

        return ResolveResult.ok("补收款成功");
    }

    /** 交易号前缀：抄自 PosOrderPaymentServiceImpl.generateTransactionId 的既有约定 */
    private String genTxnNo(Integer paymentMethod) {
        String prefix = paymentMethod != null && paymentMethod == 1 ? "CSH-" : "WX-";
        return prefix + System.currentTimeMillis();
    }

    /**
     * 异常单作废（VOID）—— Lead 2026-10-29；R7 RT-3/RT-4 整改 2026-10-30。
     *
     * <p>与 resolve 的差别：**不产生任何资金流水**（fund_flows 零新增），金额相关列全部为 NULL。</p>
     *
     * <p><b>执行顺序（每一步都不能省）</b>：</p>
     * <ol>
     *   <li>已支付拦截（钱已收 → 只能退款）；</li>
     *   <li><b>CAS 0→3</b>：唯一的并发闸门，也是库存回补"至多一次"的保证；0 行 → 查状态 → 409；</li>
     *   <li><b>厨房单取消</b>：同事务，失败即抛 → 整单回滚（RT-4）；</li>
     *   <li><b>库存回补</b>：{@link PosOrderStockRestorer}（与 TTL 关单同源，RT-4）；</li>
     *   <li>写 exception_ops（唯一键冲突 → 规范 409）。</li>
     * </ol>
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

        // CAS：order_status -> 3（已取消）；★ RT-3：谓词含 order_status = 0（SQL 见 ExceptionOpsMapper.casCancel）
        int casRows = exceptionOpsMapper.casCancel(orderId);
        if (casRows == 0) {
            Integer osNow = exceptionOpsMapper.orderStatusOf(orderId);
            log.info("[METRIC] exception_void_rejected_cas_miss orderId={} orderStatus={}", orderId, osNow);
            return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, voidRejectMessage(osNow));
        }

        // 厨房联动：同一事务内，失败必须抛（RT-4：修前是 try-warn → 订单 cancelled 而厨房仍 pending）
        cancelKitchenOrderOrThrow(orderId);

        // 库存回补：与 TTL 关单同源（RT-4：修前直接打 metric 跳过 → foods.stock 永久虚减）
        int restored = stockRestorer.restoreForOrder(orderId, "[EXC-VOID]");
        log.info("[METRIC] exception_void_stock_restored orderId={} lines={}", orderId, restored);

        ExceptionOps ops = new ExceptionOps();
        ops.setOrderId(orderId);
        ops.setOpType("VOID");
        ops.setReason(reason);
        ops.setNote(note);
        ops.setIdempotencyKey(idempotencyKey);
        ops.setOperatorId(String.valueOf(SecurityUtils.getCurrentUserId()));   // P0-D: 同上
        ops.setShiftId(order.getShiftId());
        // VOID 不产生资金流水，金额与 flow_no 一律 NULL
        ops.setAmountFen(null);
        ops.setPayType(null);
        ops.setCashReceivedFen(null);
        ops.setFlowNo(null);
        ops.setCreateTime(LocalDateTime.now());
        ops.setUpdateTime(LocalDateTime.now());
        ops.setDeleted(0);
        insertOpsOrThrow409(ops, orderId, "VOID");

        return ResolveResult.ok("作废成功");
    }

    /**
     * CAS 未命中时的用户文案（RT-3）：按**当前真实状态**分流，不笼统说"已被处理"。
     *
     * <p>二次作废（最常见：收银员双击）必须落到第一句 —— 这是可预期的幂等结果，不是系统故障。</p>
     */
    private String voidRejectMessage(Integer osNow) {
        if (osNow == null) {
            return "订单不存在或已删除，无法作废";
        }
        switch (osNow) {
            case ORDER_STATUS_CANCELLED:
                return "订单已是【已取消】状态，无需重复作废";
            case ORDER_STATUS_REFUNDED:
                return "订单已是【已全额退款】状态，无需作废；如确需调整请走退款流程";
            case ORDER_STATUS_COMPLETED:
                return "订单已完成，不能作废，请走退款流程";
            case ORDER_STATUS_CONFIRMED:
                return "订单已确认，不能作废（仅待确认订单可作废），请走退单流程";
            case ORDER_STATUS_PENDING:
                // 理论不可达：CAS 条件正是 order_status = 0
                return "订单状态并发变更，请刷新后重试";
            default:
                return "该订单当前状态（" + osNow + "）不支持作废，请刷新后重试";
        }
    }

    /**
     * 取消关联的厨房单（RT-4）。
     *
     * <p><b>修前</b>：{@code try { ... } catch (Exception e) { log.warn(...); }}</b> → 厨房侧失败被吞，
     * 订单照样 cancelled 提交，后厨会照做一张已经不存在的单（物料照耗），且用户看到"作废成功"。</p>
     *
     * <p><b>修后</b>：取消失败直接抛 {@link com.foodtraceability.common.exception.BusinessException}（409），
     * 由外层 {@code @Transactional(rollbackFor = Exception.class)} 回滚整单作废 —— 要么都成功，要么都不发生。</p>
     *
     * <p>幂等：厨房单已经是 {@code cancelled} 时直接跳过（重复作废时不该因为"已经是取消态"而报错）。</p>
     */
    private void cancelKitchenOrderOrThrow(String orderId) {
        KitchenOrder ko = kitchenOrderMapper.selectOne(new LambdaQueryWrapper<KitchenOrder>()
                .eq(KitchenOrder::getOrderId, orderId).last("LIMIT 1"));
        if (ko == null) {
            log.info("[EXC-VOID] 无关联厨房单，跳过厨房联动 orderId={}", orderId);
            return;
        }
        if ("cancelled".equalsIgnoreCase(ko.getStatus())) {
            log.info("[EXC-VOID] 厨房单已是取消态，幂等跳过 orderId={} kitchenOrderId={}", orderId, ko.getKitchenOrderId());
            return;
        }
        KitchenOrder patch = new KitchenOrder();
        patch.setStatus("cancelled");
        patch.setCancelTime(LocalDateTime.now());
        patch.setCancelReason("异常单作废");
        patch.setMaterialLocked(0);
        // 条件更新：只有「还没取消」的行才会被改到 → 并发下天然幂等
        int rows = kitchenOrderMapper.update(patch, new LambdaUpdateWrapper<KitchenOrder>()
                .eq(KitchenOrder::getId, ko.getId())
                .ne(KitchenOrder::getStatus, "cancelled"));
        if (rows == 0) {
            // 查一次当前状态：真并发（别人刚取消）算幂等成功；其它情况视为联动失败 → 回滚
            KitchenOrder now = kitchenOrderMapper.selectById(ko.getId());
            if (now != null && "cancelled".equalsIgnoreCase(now.getStatus())) {
                log.info("[EXC-VOID] 厨房单已被并发取消，幂等通过 orderId={}", orderId);
                return;
            }
            log.error("[EXC-VOID] 厨房单取消失败，回滚整单作废 orderId={} kitchenOrderId={}", orderId, ko.getKitchenOrderId());
            throw new com.foodtraceability.common.exception.BusinessException(
                    CODE_CONFLICT, "厨房单取消失败（可能已被其它终端修改），本次作废已回滚，请刷新后重试", HTTP_CONFLICT);
        }
        log.info("[EXC-VOID] 厨房单已取消 orderId={} kitchenOrderId={}", orderId, ko.getKitchenOrderId());
    }

    /**
     * 写 exception_ops，并把「唯一键冲突」翻译成规范业务 409（RT-3）。
     *
     * <p><b>修前</b>：{@code insertWithinSavepoint} 的 {@link DataIntegrityViolationException} 一路穿透到
     * Controller 的 {@code catch (Exception)}，前端拿到 500 + 裸 PostgreSQL 原文
     * （实测：{@code 重复键违反唯一约束 "uk_exception_ops_order"... ### SQL: INSERT INTO exception_ops ...}）。</p>
     *
     * <p><b>修后</b>：保存点回滚这一段 → 抛业务 409 → 外层事务整体回滚 → Controller 返回真 409 + 中文文案。
     * 底层 SQL/约束名不再出现在任何响应里。</p>
     */
    private void insertOpsOrThrow409(ExceptionOps ops, String orderId, String opType) {
        try {
            opsAttempt.insertWithinSavepoint(ops);
        } catch (DataIntegrityViolationException e) {
            log.warn("[METRIC] exception_ops_conflict orderId={} opType={} err={}", orderId, opType, e.getMostSpecificCause().getMessage());
            throw new com.foodtraceability.common.exception.BusinessException(
                    CODE_CONFLICT,
                    "该订单已存在处理记录（可能已被其它终端处理），本次" + ("VOID".equals(opType) ? "作废" : "补收款") + "未生效，请刷新后重试",
                    HTTP_CONFLICT);
        }
    }
}
