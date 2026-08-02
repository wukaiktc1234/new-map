package com.foodtraceability.statemachine;

import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.utils.StateMachineUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 招聘需求状态机（兼容现状 + 新增反馈流）
 *
 * <p>遵循 ADR-002：使用 {@code Map<String, Set<String>>} 定义状态流转规则，
 * 校验逻辑委托给 {@link StateMachineUtil#validateTransition}。</p>
 *
 * <p>状态流转规则：</p>
 * <ul>
 *   <li>open → feedback_given / approved / rejected / closed / filled</li>
 *   <li>feedback_given → open（重新提交后回到 open）</li>
 *   <li>approved → filled / closed</li>
 *   <li>submitted → open（兼容现有 type=hr 流程的提交流）</li>
 *   <li>rejected / closed / filled 为终态</li>
 * </ul>
 *
 * <p>注意：{@code submitted} 状态用于兼容现有 HR 创建需求的提交流，
 * 经 Phase 4 spec 决策（spec user decision 1）保留 open 作为主要进行中状态。</p>
 */
public final class RecruitmentRequirementStateMachine {

    private RecruitmentRequirementStateMachine() {
    }

    /**
     * 状态流转规则 Map（key=当前状态，value=允许的目标状态集合）。
     *
     * <p>使用 HashMap 而非 Map.of 以支持超过 10 个 entry。</p>
     */
    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        // open：进行中（兼容现状的主状态）→ 反馈/通过/拒绝/关闭/招满
        TRANSITIONS.put(RecruitmentConstants.REQ_OPEN, Set.of(
                RecruitmentConstants.REQ_FEEDBACK_GIVEN,
                RecruitmentConstants.REQ_APPROVED,
                RecruitmentConstants.REQ_REJECTED,
                RecruitmentConstants.REQ_CLOSED,
                RecruitmentConstants.REQ_FILLED));
        // feedback_given：HR 已反馈 → 重新提交回到 open
        TRANSITIONS.put(RecruitmentConstants.REQ_FEEDBACK_GIVEN,
                Set.of(RecruitmentConstants.REQ_OPEN));
        // approved：HR 通过 → 招满/关闭
        TRANSITIONS.put(RecruitmentConstants.REQ_APPROVED, Set.of(
                RecruitmentConstants.REQ_FILLED,
                RecruitmentConstants.REQ_CLOSED));
        // submitted：兼容现有 HR 提交流 → open
        TRANSITIONS.put(RecruitmentConstants.REQ_SUBMITTED,
                Set.of(RecruitmentConstants.REQ_OPEN));
        // rejected / closed / filled 为终态，无后续流转
    }

    /**
     * 校验招聘需求状态流转是否合法。
     *
     * @param current 当前状态
     * @param target  目标状态
     * @throws com.foodtraceability.common.exception.BusinessException 非法流转时抛出
     */
    public static void validateTransition(String current, String target) {
        StateMachineUtil.validateTransition(TRANSITIONS, current, target, "招聘需求");
    }
}
