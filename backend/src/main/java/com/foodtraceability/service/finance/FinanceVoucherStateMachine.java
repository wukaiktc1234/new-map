package com.foodtraceability.service.finance;

import com.foodtraceability.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 记账凭证独立状态机组件（F-023）
 *
 * <p>将凭证状态流转从 voucherStatus 字段硬编码升级为独立状态机，集中管理合法状态转换。
 * 状态码采用 Integer（与 FinanceVoucher.voucherStatus 字段类型一致）：
 * <ul>
 *   <li>DRAFT(0) — 暂存</li>
 *   <li>APPROVED(1) — 已审核</li>
 *   <li>POSTED(2) — 已过账</li>
 *   <li>VOID(3) — 已作废</li>
 * </ul>
 *
 * <p>合法状态流转：
 * <ul>
 *   <li>DRAFT → APPROVED（审核）</li>
 *   <li>APPROVED → POSTED（过账）</li>
 *   <li>APPROVED → DRAFT（反审核）</li>
 *   <li>POSTED → DRAFT（反过账）</li>
 *   <li>DRAFT → VOID（作废）</li>
 *   <li>POSTED → VOID（作废，需特殊处理）</li>
 * </ul>
 *
 * <p>说明：项目中已存在 {@code com.foodtraceability.entity.enums.VoucherStatus} 枚举，
 * 但其语义为"电子凭证处理状态"（PENDING/VERIFIED/PROCESSED/ARCHIVED），
 * 与财务记账凭证状态（暂存/已审核/已过账/已作废）不同，故本状态机使用 Integer 状态码，
 * 不复用该枚举，避免语义冲突。
 */
@Component
public class FinanceVoucherStateMachine {

    private static final Logger log = LoggerFactory.getLogger(FinanceVoucherStateMachine.class);

    /** 暂存 */
    public static final Integer DRAFT = 0;
    /** 已审核 */
    public static final Integer APPROVED = 1;
    /** 已过账 */
    public static final Integer POSTED = 2;
    /** 已作废 */
    public static final Integer VOID = 3;

    /**
     * 合法状态流转表：key=起始状态，value=可到达的目标状态集合
     */
    private static final Map<Integer, Set<Integer>> TRANSITIONS;

    static {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        map.put(DRAFT, new HashSet<>(Arrays.asList(APPROVED, VOID)));
        map.put(APPROVED, new HashSet<>(Arrays.asList(POSTED, DRAFT)));
        map.put(POSTED, new HashSet<>(Arrays.asList(DRAFT, VOID)));
        map.put(VOID, Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * 检查状态流转是否合法
     *
     * @param from 起始状态码
     * @param to   目标状态码
     * @return 合法返回 true，非法或状态码无效返回 false
     */
    public boolean canTransition(Integer from, Integer to) {
        if (from == null || to == null) {
            return false;
        }
        Set<Integer> targets = TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * 执行状态流转校验，非法流转抛出 BusinessException
     *
     * @param from 起始状态码
     * @param to   目标状态码
     * @throws BusinessException 当状态码无效或流转不合法时抛出
     */
    public void transition(Integer from, Integer to) {
        if (!isValidStatus(from)) {
            throw new BusinessException("无效的凭证状态码：" + from);
        }
        if (!isValidStatus(to)) {
            throw new BusinessException("无效的凭证状态码：" + to);
        }
        if (!canTransition(from, to)) {
            throw new BusinessException(
                    "非法的凭证状态流转：" + getStatusName(from) + " → " + getStatusName(to));
        }
        log.debug("凭证状态流转：{} → {}", getStatusName(from), getStatusName(to));
    }

    /**
     * 判断状态码是否有效
     *
     * @param status 状态码
     * @return 有效返回 true
     */
    public boolean isValidStatus(Integer status) {
        return status != null && TRANSITIONS.containsKey(status);
    }

    /**
     * 获取状态名称（用于日志和异常信息）
     *
     * @param status 状态码
     * @return 状态中文名称，未知状态返回 "未知"
     */
    public String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "暂存";
            case 1:
                return "已审核";
            case 2:
                return "已过账";
            case 3:
                return "已作废";
            default:
                return "未知";
        }
    }
}
