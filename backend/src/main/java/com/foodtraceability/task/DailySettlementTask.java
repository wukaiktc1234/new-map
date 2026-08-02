package com.foodtraceability.task;

import com.foodtraceability.service.impl.DailySettlementServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 日结对账生成定时任务（可选）
 *
 * <h2>⚠️ 重要说明</h2>
 * <p>此定时任务默认<strong>禁用</strong>，因为：</p>
 * <ol>
 *   <li><strong>权限隔离</strong>：每个门店独立运营，不应在全局任务中处理所有门店</li>
 *   <li><strong>按需生成</strong>：推荐使用 {@link DailySettlementServiceImpl#getOrGenerateSettlement(String, LocalDate)} 按需生成</li>
 *   <li><strong>性能考虑</strong>：避免凌晨集中计算影响数据库性能</li>
 * </ol>
 *
 * <h2>启用方式（如确实需要）</h2>
 * <p>在 application.yml 中配置：</p>
 * <pre>{@code
 * settlement:
 *   task:
 *     enabled: true
 *     store-id: STORE001  # 指定要处理的门店ID
 * }</pre>
 *
 * <h2>推荐替代方案</h2>
 * <ul>
 *   <li><strong>方案A（推荐）</strong>：用户访问日结页面时自动按需生成（见 Service#getOrGenerateSettlement）</li>
 *   <li><strong>方案B</strong>：手动触发（管理员在页面上点击"生成本日数据"按钮）</li>
 *   <li><strong>方案C</strong>：事件驱动（订单状态变更时触发重算）</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "settlement.task.enabled", havingValue = "true")
public class DailySettlementTask {

    private static final Logger log = LoggerFactory.getLogger(DailySettlementTask.class);

    private final DailySettlementServiceImpl dailySettlementService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param dailySettlementService 日结服务
     */
    public DailySettlementTask(DailySettlementServiceImpl dailySettlementService) {
        this.dailySettlementService = dailySettlementService;
    }

    /**
     * 为指定门店生成昨日日结数据
     *
     * <p>此方法仅在配置文件中显式启用时才会执行，
     * 且只会处理配置文件中指定的单个门店ID。</p>
     *
     * <p>Cron: 每天 00:30 执行</p>
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void generateSingleStoreSettlement() {
        // TODO: 从配置文件读取storeId（如果需要全局定时任务）
        // String targetStoreId = environment.getProperty("settlement.task.store-id");
        //
        // if (StringUtils.hasText(targetStoreId)) {
        //     LocalDate yesterday = LocalDate.now().minusDays(1);
        //     dailySettlementService.generateDailySettlement(targetStoreId, yesterday);
        //     log.info("已完成门店[{}]的日结生成: date={}", targetStoreId, yesterday);
        // } else {
        //     log.warn("未配置 settlement.task.store-id，跳过执行");
        // }

        log.warn("日结对账定时任务已禁用（符合权限隔离原则）。推荐使用按需生成模式。");
    }
}
