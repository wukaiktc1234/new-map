package com.foodtraceability.config;

import com.foodtraceability.service.SalesAnalysisService;
import com.foodtraceability.service.InventoryAnalysisService;
import com.foodtraceability.service.CustomerAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 报表生成定时任务
 * 自动生成日报、周报、月报等分析报表
 */
@Component
public class ReportGenerationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationScheduler.class);

    private final SalesAnalysisService salesAnalysisService;
    private final InventoryAnalysisService inventoryAnalysisService;
    private final CustomerAnalysisService customerAnalysisService;

    public ReportGenerationScheduler(
            SalesAnalysisService salesAnalysisService,
            InventoryAnalysisService inventoryAnalysisService,
            CustomerAnalysisService customerAnalysisService) {
        this.salesAnalysisService = salesAnalysisService;
        this.inventoryAnalysisService = inventoryAnalysisService;
        this.customerAnalysisService = customerAnalysisService;
    }

    /**
     * 每天凌晨2点生成销售日报和库存分析报表
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateDailyReports() {
        logger.info("========== 开始执行每日报表生成任务 ==========");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        try {
            // 1. 生成销售日报
            logger.info("开始生成销售日报，日期: {}", yesterday);
            salesAnalysisService.generateDailySalesReport(yesterday);
            logger.info("销售日报生成完成");

            // 2. 生成库存分析报表
            logger.info("开始生成库存分析报表，日期: {}", yesterday);
            inventoryAnalysisService.generateInventoryReport(yesterday);
            logger.info("库存分析报表生成完成");

            logger.info("========== 每日报表生成任务完成 ==========");
        } catch (Exception e) {
            logger.error("每日报表生成任务失败", e);
        }
    }

    /**
     * 每周一凌晨3点生成周报
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void generateWeeklyReports() {
        logger.info("========== 开始执行每周报表生成任务 ==========");
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7); // 最近7天

        try {
            // 1. 生成销售周报
            logger.info("开始生成销售周报，期间: {} - {}", weekStart, today);
            salesAnalysisService.generateWeeklySalesReport(weekStart, today);
            logger.info("销售周报生成完成");

            // TODO: 2. 生成客户分析周报
            // String period = "W" + today.format(DateTimeFormatter.BASIC_ISO_DATE);
            // customerAnalysisService.generateCustomerReport(period);

            logger.info("========== 每周报表生成任务完成 ==========");
        } catch (Exception e) {
            logger.error("每周报表生成任务失败", e);
        }
    }

    /**
     * 每月1号凌晨4点生成月报
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void generateMonthlyReports() {
        logger.info("========== 开始执行每月报表生成任务 ==========");
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1); // 本月1号

        try {
            // TODO: 生成各类月度分析报表
            logger.info("开始生成月度报表，月份: {}", today.getMonth());

            logger.info("========== 每月报表生成任务完成 ==========");
        } catch (Exception e) {
            logger.error("每月报表生成任务失败", e);
        }
    }

    /**
     * 执行自定义定时报表（每5分钟检查一次）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void executeScheduledCustomReports() {
        // TODO: 查询有schedule_cron配置且到期的自定义报表并执行
        // 1. 查询 CustomReport where schedule_cron is not null and is_public = true
        // 2. 判断是否到达执行时间
        // 3. 调用 customReportService.executeCustomReport()
    }
}
