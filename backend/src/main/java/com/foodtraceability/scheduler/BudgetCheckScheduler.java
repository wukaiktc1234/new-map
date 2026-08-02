package com.foodtraceability.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.finance.Budget;
import com.foodtraceability.mapper.finance.BudgetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 预算执行检查定时任务
 *
 * 定期检查预算执行情况，更新实际金额和差异
 */
@Component
public class BudgetCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(BudgetCheckScheduler.class);

    private final BudgetMapper budgetMapper;

    public BudgetCheckScheduler(BudgetMapper budgetMapper) {
        this.budgetMapper = budgetMapper;
    }

    /**
     * 预算执行检查任务
     * 每天凌晨3点执行，检查当前月份的预算执行情况
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkBudgetExecution() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        log.info("========== 开始预算执行检查: {}-{} ==========", year, month);

        try {
            // 查询当月所有预算记录
            LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Budget::getBudgetYear, year)
                   .eq(Budget::getBudgetMonth, month);
            List<Budget> budgets = budgetMapper.selectList(wrapper);

            if (budgets.isEmpty()) {
                log.info("当月无预算数据");
                return;
            }

            int checkedCount = 0;
            for (Budget budget : budgets) {
                // 这里可以根据业务逻辑计算实际金额
                // 例如：从收支记录/成本记录中按类别汇总
                // 当前仅做日志输出，实际金额由各业务模块主动调用 updateActualAmount 更新
                log.debug("预算检查: type={}, category={}, budget={}, actual={}, variance={}",
                        budget.getBudgetType(), budget.getCategoryId(),
                        budget.getBudgetAmount(), budget.getActualAmount(),
                        budget.getVariance());
                checkedCount++;
            }

            log.info("预算执行检查完成，共检查 {} 条记录", checkedCount);

        } catch (Exception e) {
            log.error("预算执行检查失败", e);
        }
    }
}
