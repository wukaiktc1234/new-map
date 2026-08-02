package com.foodtraceability.statemachine;

import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.utils.StateMachineUtil;

import java.util.Map;
import java.util.Set;

/**
 * 招聘名额状态机（7 状态）
 *
 * <p>遵循 ADR-002：使用 {@code Map<String, Set<String>>} 定义状态流转规则，
 * 校验逻辑委托给 {@link StateMachineUtil#validateTransition}。</p>
 *
 * <p>状态流转规则：</p>
 * <ul>
 *   <li>draft → issued</li>
 *   <li>issued → active / rejected / adjustment_requested / closed</li>
 *   <li>adjustment_requested → active / issued</li>
 *   <li>active → exhausted / closed</li>
 *   <li>exhausted → closed</li>
 *   <li>rejected / closed 为终态</li>
 * </ul>
 */
public final class RecruitmentQuotaStateMachine {

    private RecruitmentQuotaStateMachine() {
    }

    /**
     * 状态流转规则 Map（key=当前状态，value=允许的目标状态集合）。
     */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            RecruitmentConstants.QUOTA_DRAFT,
            Set.of(RecruitmentConstants.QUOTA_ISSUED),
            RecruitmentConstants.QUOTA_ISSUED,
            Set.of(RecruitmentConstants.QUOTA_ACTIVE,
                    RecruitmentConstants.QUOTA_REJECTED,
                    RecruitmentConstants.QUOTA_ADJUSTMENT_REQUESTED,
                    RecruitmentConstants.QUOTA_CLOSED),
            RecruitmentConstants.QUOTA_ADJUSTMENT_REQUESTED,
            Set.of(RecruitmentConstants.QUOTA_ACTIVE, RecruitmentConstants.QUOTA_ISSUED),
            RecruitmentConstants.QUOTA_ACTIVE,
            Set.of(RecruitmentConstants.QUOTA_EXHAUSTED, RecruitmentConstants.QUOTA_CLOSED),
            RecruitmentConstants.QUOTA_EXHAUSTED,
            Set.of(RecruitmentConstants.QUOTA_CLOSED)
            // REJECTED / CLOSED 为终态,无后续流转
    );

    /**
     * 校验名额状态流转是否合法。
     *
     * @param current 当前状态
     * @param target  目标状态
     * @throws com.foodtraceability.common.exception.BusinessException 非法流转时抛出
     */
    public static void validateTransition(String current, String target) {
        StateMachineUtil.validateTransition(TRANSITIONS, current, target, "名额");
    }
}
