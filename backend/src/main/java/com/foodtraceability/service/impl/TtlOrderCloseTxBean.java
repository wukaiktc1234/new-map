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
 * TTL 关单：单笔一个独立事务（REQUIRES_NEW）。
 *
 * <p><b>为什么 REQUIRES_NEW：</b>整批 500 笔里某一笔失败（如菜品已删除）不应回滚其它 499 笔；
 * 每笔独立提交，失败单独记 metric。</p>
 *
 * <p><b>库存回补口径：</b>POS 下单扣的是 <b>foods.stock（目录可售数）</b>，退款时已实现
 * {@code recoverStockForRefundNew}（order_items → food_id → food_code → foodNewMapper.addStock）。
 * 本类**复用同一路径**，仅备注改为 {@code [TTL] 订单超时关单回补} 以示区分。</p>
 *
 * <p><b>不动 inventory.locked_quantity：</b>实测该列全仓 0 处代码引用、恒为 0（本项目无原料锁定实现），
 * 因此**不做任何「释放」动作**，只打 metric 说明。</p>
 */
@Component
public class TtlOrderCloseTxBean {

    private static final Logger log = LoggerFactory.getLogger(TtlOrderCloseTxBean.class);

    public static final String REASON_AUTO = "EXPIRED_AUTO";

    private final TtlOrderCloseMapper closeMapper;
    private final FoodNewMapper foodNewMapper;

    public TtlOrderCloseTxBean(TtlOrderCloseMapper closeMapper, FoodNewMapper foodNewMapper) {
        this.closeMapper = closeMapper;
        this.foodNewMapper = foodNewMapper;
    }

    /** @return true=本次关单成功；false=CAS 未命中（已被支付/处理），放弃不覆盖 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean closeOne(String orderId) {
        int rows = closeMapper.casCancelExpired(orderId, REASON_AUTO);
        if (rows == 0) {
            log.info("[TTL] CAS 未命中（顾客刚好已支付），放弃不覆盖 orderId={}", orderId);
            return false;
        }
        // 库存回补：与 recoverStockForRefundNew 同源（order_items.food_id 是数字 id，需换 food_code）
        List<Map<String, Object>> items = closeMapper.findItems(orderId);
        int ok = 0;
        if (items != null) {
            for (Map<String, Object> it : items) {
                Object fidObj = it.get("food_id");
                Object qtyObj = it.get("quantity");
                if (fidObj == null || qtyObj == null) {
                    continue;
                }
                Long fid = Long.valueOf(String.valueOf(fidObj));
                Integer qty = Integer.valueOf(String.valueOf(qtyObj));
                if (qty <= 0) {
                    continue;
                }
                FoodNew food = foodNewMapper.selectById(fid);
                if (food == null || food.getFoodCode() == null) {
                    log.warn("[TTL] 找不到菜品编码，跳过回补 orderId={} foodId={}", orderId, fid);
                    continue;
                }
                foodNewMapper.addStock(food.getFoodCode(), qty);
                ok++;
            }
        }
        log.info("[TTL] 订单超时关单回补 orderId={} 回补明细数={} remark=[TTL] 订单超时关单回补", orderId, ok);
        // 明确记录：本项目无原料锁定实现，无需释放（不假装释放）
        log.info("[METRIC] ttl_close_inventory_locked_untouched orderId={} reason=no_lock_implementation", orderId);
        return true;
    }
}
