package com.foodtraceability.service.impl;

import com.foodtraceability.entity.FoodNew;
import com.foodtraceability.mapper.FoodNewMapper;
import com.foodtraceability.mapper.TtlOrderCloseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 订单「退回可售数」的唯一实现：{@code order_items.food_id → foods.food_code → foods.stock += quantity}。
 *
 * <p><b>RT-4 [P1]（R7 红队审计）</b>：修前这段逻辑只写在 {@code TtlOrderCloseTxBean} 里，
 * 异常单作废路径觉得自己"没有 location/material 映射"就打了
 * {@code [METRIC] exception_void_inventory_release_skipped} 直接跳过 —— 于是 POS 下单扣掉的
 * <b>foods.stock</b> 永远回不来，长期库存虚减、毛利失真。</p>
 *
 * <p>抽成独立组件后，TTL 关单 / 异常单作废 / 后续其它"取消"路径<b>共用同一份实现</b>：
 * 口径只有一处，改一处即全链路生效（R7 报告"复用 TTL 关单同源实现"的落地）。</p>
 *
 * <p><b>幂等边界：</b>本组件本身不做去重，靠调用方的 CAS 闸门保证"每张单只回补一次"——
 * 订单状态从 0（待确认）跳到 3（已取消）的 CAS 只有一个赢家
 * （{@code TtlOrderCloseMapper.casCancelExpired} / {@code ExceptionOpsMapper.casCancel}），
 * 输的那一方不会走到这里。</p>
 *
 * <p><b>为什么 {@code propagation = MANDATORY}：</b>回补必须与"取消订单"处在同一事务里，
 * 否则取消提交、回补失败就成了半写状态。MANDATORY 让"忘了开事务"的调用在开发期直接报错，
 * 而不是在生产上悄悄半提交。</p>
 */
@Component
public class PosOrderStockRestorer {

    private static final Logger log = LoggerFactory.getLogger(PosOrderStockRestorer.class);

    private final TtlOrderCloseMapper ttlOrderCloseMapper;
    private final FoodNewMapper foodNewMapper;

    public PosOrderStockRestorer(TtlOrderCloseMapper ttlOrderCloseMapper, FoodNewMapper foodNewMapper) {
        this.ttlOrderCloseMapper = ttlOrderCloseMapper;
        this.foodNewMapper = foodNewMapper;
    }

    /**
     * 按订单明细回补 {@code foods.stock}。
     *
     * @param orderId 订单号
     * @param tag     日志标记（如 {@code [TTL]} / {@code [EXC-VOID]}），用于区分来源
     * @return 实际回补成功的菜品行数
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public int restoreForOrder(String orderId, String tag) {
        List<Map<String, Object>> items = ttlOrderCloseMapper.findItems(orderId);
        int ok = 0;
        if (items == null || items.isEmpty()) {
            log.info("{} 无订单明细，跳过库存回补 orderId={}", tag, orderId);
            return 0;
        }
        for (Map<String, Object> it : items) {
            Object fidObj = it.get("food_id");
            Object qtyObj = it.get("quantity");
            if (fidObj == null || qtyObj == null) {
                continue;
            }
            Long fid;
            int qty;
            try {
                fid = Long.valueOf(String.valueOf(fidObj));
                qty = Integer.parseInt(String.valueOf(qtyObj));
            } catch (NumberFormatException e) {
                log.warn("{} 明细数量/菜品ID 非法，跳过 orderId={} foodId={} qty={}", tag, orderId, fidObj, qtyObj);
                continue;
            }
            if (qty <= 0) {
                continue;
            }
            FoodNew food = foodNewMapper.selectById(fid);
            if (food == null || food.getFoodCode() == null) {
                // 菜品被物理删除/编码缺失：不能猜，记 metric 交人工核账（但事务不因此回滚）
                log.warn("{} 找不到菜品编码，跳过该行 orderId={} foodId={}", tag, orderId, fid);
                continue;
            }
            int rows = foodNewMapper.addStock(food.getFoodCode(), qty);
            if (rows == 0) {
                // addStock 自带 stock + qty <= 99999 上限；到顶说明数据异常，不该静默成功
                log.warn("{} addStock 未生效（可能触碰 99999 上限）orderId={} foodCode={} qty={}",
                        tag, orderId, food.getFoodCode(), qty);
                continue;
            }
            ok++;
        }
        log.info("{} 库存回补完成 orderId={} 回补明细数={}", tag, orderId, ok);
        return ok;
    }
}
