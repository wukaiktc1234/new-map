package com.foodtraceability.service.approval.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批策略工厂
 *
 * <p>通过 Spring 自动注入所有 ApprovalStrategy 实现，按 strategyType 路由。
 * 客户在后台切换"组织模式"时，系统底层只是切换了使用的策略实现类。
 *
 * <p>使用方式：
 * <pre>
 *   ApprovalStrategy strategy = factory.getStrategy("role_based");
 *   List&lt;String&gt; approvers = strategy.resolveApprovers(actionDTO, nodeId, "store_manager");
 * </pre>
 *
 * <p>这是 RBAC+动态审批链第四层（策略模式）的路由入口。
 */
@Component
public class ApprovalStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(ApprovalStrategyFactory.class);
    private static final String DEFAULT_STRATEGY_TYPE = "role_based";

    private final Map<String, ApprovalStrategy> strategyMap;

    /**
     * 构造函数注入：Spring 会自动注入所有 ApprovalStrategy 类型的 Bean
     * @param strategies 所有审批策略实现
     */
    public ApprovalStrategyFactory(List<ApprovalStrategy> strategies) {
        this.strategyMap = new HashMap<>();
        for (ApprovalStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyType(), strategy);
            log.info("注册审批策略: type={}, class={}",
                    strategy.getStrategyType(), strategy.getClass().getSimpleName());
        }
        log.info("审批策略工厂初始化完成，共注册 {} 个策略", strategyMap.size());
    }

    /**
     * 获取策略
     * @param strategyType 策略类型（superior/role_based/chain）
     * @return 策略实现（不存在时返回默认策略 role_based）
     */
    public ApprovalStrategy getStrategy(String strategyType) {
        if (strategyType == null || strategyType.isEmpty()) {
            return strategyMap.get(DEFAULT_STRATEGY_TYPE);
        }
        ApprovalStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            log.warn("未找到审批策略: type={}, 回退到默认策略: {}", strategyType, DEFAULT_STRATEGY_TYPE);
            return strategyMap.get(DEFAULT_STRATEGY_TYPE);
        }
        return strategy;
    }

    /**
     * 获取所有已注册的策略类型
     * @return 策略类型列表
     */
    public List<String> getRegisteredTypes() {
        return List.copyOf(strategyMap.keySet());
    }
}
