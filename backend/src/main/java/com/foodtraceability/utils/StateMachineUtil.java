package com.foodtraceability.utils;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import java.util.Map;
import java.util.Set;

/**
 * 状态机校验工具类（通用）
 *
 * <p>遵循 ADR-002：提供通用的状态流转校验方法，避免在每个 ServiceImpl 中重复编写校验逻辑。
 * 各业务模块（如 RecruitmentQuotaStateMachine / JobOfferStateMachine）只需定义
 * {@code Map<String, Set<String>>} 流转规则，调用本工具类完成校验。</p>
 *
 * <p>非法流转时抛出 {@link BusinessException}，错误码为
 * {@link ErrorCode#INVALID_STATUS_TRANSITION}。</p>
 */
public final class StateMachineUtil {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private StateMachineUtil() {
    }

    /**
     * 通用状态流转校验。
     *
     * <p>校验逻辑：从 transitions 中查找 current 状态允许的目标状态集合，
     * 若 target 不在允许集合中（或 current 无定义），则抛出 BusinessException。</p>
     *
     * @param transitions 状态流转规则 Map（key=当前状态，value=允许的目标状态集合）
     * @param current     当前状态
     * @param target      目标状态
     * @param entityType  实体类型描述（用于异常消息，如 "名额"、"Offer"）
     * @throws BusinessException 当 current 不存在或 target 不被允许时抛出
     */
    public static void validateTransition(Map<String, Set<String>> transitions,
                                          String current, String target,
                                          String entityType) {
        Set<String> allowedTargets = transitions.get(current);
        if (allowedTargets == null || !allowedTargets.contains(target)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("%s状态非法流转: %s → %s", entityType, current, target));
        }
    }
}
