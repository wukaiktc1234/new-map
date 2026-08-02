package com.foodtraceability.statemachine;

import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.utils.StateMachineUtil;

import java.util.Map;
import java.util.Set;

/**
 * Offer 状态机（6 状态）
 *
 * <p>遵循 ADR-002：使用 {@code Map<String, Set<String>>} 定义状态流转规则，
 * 校验逻辑委托给 {@link StateMachineUtil#validateTransition}。</p>
 *
 * <p>状态流转规则：</p>
 * <ul>
 *   <li>pending → sent / withdrawn</li>
 *   <li>sent → accepted / rejected / withdrawn</li>
 *   <li>accepted → onboarded</li>
 *   <li>rejected / withdrawn / onboarded 为终态</li>
 * </ul>
 */
public final class JobOfferStateMachine {

    private JobOfferStateMachine() {
    }

    /**
     * 状态流转规则 Map（key=当前状态，value=允许的目标状态集合）。
     */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            RecruitmentConstants.OFFER_PENDING,
            Set.of(RecruitmentConstants.OFFER_SENT, RecruitmentConstants.OFFER_WITHDRAWN),
            RecruitmentConstants.OFFER_SENT,
            Set.of(RecruitmentConstants.OFFER_ACCEPTED,
                    RecruitmentConstants.OFFER_REJECTED,
                    RecruitmentConstants.OFFER_WITHDRAWN),
            RecruitmentConstants.OFFER_ACCEPTED,
            Set.of(RecruitmentConstants.OFFER_ONBOARDED)
            // REJECTED / WITHDRAWN / ONBOARDED 为终态，无后续流转
    );

    /**
     * 校验 Offer 状态流转是否合法。
     *
     * @param current 当前状态
     * @param target  目标状态
     * @throws com.foodtraceability.common.exception.BusinessException 非法流转时抛出
     */
    public static void validateTransition(String current, String target) {
        StateMachineUtil.validateTransition(TRANSITIONS, current, target, "Offer");
    }
}
