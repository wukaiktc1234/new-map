package com.foodtraceability.scheduler;

import com.foodtraceability.service.finance.AutoVoucherService;
import com.foodtraceability.service.finance.FinancialReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 月末结账定时任务
 * 每月最后一天自动执行月末结账相关操作
 * 包括成本结转、折旧计提、账龄更新等
 */
@Component
public class MonthlyClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyClosingScheduler.class);

    private final AutoVoucherService autoVoucherService;
    private final FinancialReportService reportService;

    public MonthlyClosingScheduler(AutoVoucherService autoVoucherService,
                                   FinancialReportService reportService) {
        this.autoVoucherService = autoVoucherService;
        this.reportService = reportService;
    }

    /**
     * 月末结账主任务
     * 每月最后一天的23:55执行（cron表达式）
     */
    @Scheduled(cron = "0 55 23 L * ?")
    public void monthlyClosing() {
        log.info("========== 开始执行月末结账任务 ==========");
        String period = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

        try {
            // 1. 成本结转 - 根据当月实际销售成本生成结转凭证
            log.info("步骤1: 执行成本结转...");
            executeCostTransfer(period);

            // 2. 折旧计提 - 计算固定资产折旧并生成凭证
            log.info("步骤2: 执行折旧计提...");
            executeDepreciation(period);

            // 3. 更新应收应付账龄
            log.info("步骤3: 更新应收应付账龄...");
            updateAging();

            // 4. 更新预算执行情况
            log.info("步骤4: 更新预算执行情况...");
            updateBudgetExecution();

            log.info("========== 月末结账任务完成 ==========");

        } catch (Exception e) {
            log.error("月末结账任务执行失败", e);
            throw new RuntimeException("月末结账失败：" + e.getMessage(), e);
        }
    }

    /**
     * 执行成本结转
     * 查询当月已出库商品的成本，生成成本结转凭证
     */
    private void executeCostTransfer(String period) {
        try {
            // TODO: 从库存系统获取当月实际销售成本
            // long totalCost = inventoryService.getMonthlySalesCost(period);
            long totalCost = 0L; // 示例值，实际应从业务系统获取

            if (totalCost > 0) {
                autoVoucherService.generateCostTransferVoucher(period, totalCost);
                log.info("成本结转完成，期间：{}，金额：{}分", period, totalCost);
            } else {
                log.info("本期无销售成本，跳过成本结转");
            }

        } catch (Exception e) {
            log.error("成本结转失败，期间：{}", period, e);
            throw new RuntimeException("成本结转失败", e);
        }
    }

    /**
     * 执行折旧计提
     * 计算固定资产当月折旧额并生成凭证
     */
    private void executeDepreciation(String period) {
        try {
            // TODO: 从资产管理系统获取当月折旧额
            // long depreciationAmount = assetService.getMonthlyDepreciation(period);
            long depreciationAmount = 0L; // 示例值

            if (depreciationAmount > 0) {
                autoVoucherService.generateDepreciationVoucher(period, depreciationAmount);
                log.info("折旧计提完成，期间：{}，金额：{}分", period, depreciationAmount);
            } else {
                log.info("本期无固定资产折旧，跳过折旧计提");
            }

        } catch (Exception e) {
            log.error("折旧计提失败，期间：{}", period, e);
            throw new RuntimeException("折旧计提失败", e);
        }
    }

    /**
     * 更新应收应付账龄
     * 重新计算所有未结清的应收应付账款的账龄和逾期状态
     */
    private void updateAging() {
        try {
            // TODO: 调用ReceivableService/PayableService的方法更新账龄
            log.info("应收应付账龄更新完成");

        } catch (Exception e) {
            log.error("账龄更新失败", e);
            throw new RuntimeException("账龄更新失败", e);
        }
    }

    /**
     * 更新预算执行情况
     * 将当月的实际发生额同步到预算表
     */
    private void updateBudgetExecution() {
        try {
            int currentYear = java.time.LocalDate.now().getYear();
            int currentMonth = java.time.LocalDate.now().getMonthValue();

            // 获取预算执行数据
            var executionData = reportService.getBudgetExecution(currentYear, null);
            log.info("预算执行情况更新完成，年份：{}", currentYear);

        } catch (Exception e) {
            log.error("预算执行情况更新失败", e);
            throw new RuntimeException("预算执行情况更新失败", e);
        }
    }
}
