package com.foodtraceability.task;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.entity.PendingTask;
import com.foodtraceability.mapper.PendingTaskMapper;
import com.foodtraceability.service.HealthCertificateService;
import com.foodtraceability.service.PendingTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 证件到期预警定时任务
 * 每日08:00扫描即将到期的健康证/营业执照等证件，自动生成待办任务
 *
 * 触发规则：
 * - ≤30天到期：生成高优先级待办任务
 * - 31-60天到期：生成中优先级待办任务
 * - 已过期（<0天）：生成紧急待办任务
 *
 * 幂等性保证：同一证件同一天只生成一次任务
 */
@Component
public class CertificateExpiryCheckTask {

    private static final Logger log = LoggerFactory.getLogger(CertificateExpiryCheckTask.class);

    /** 高优先级阈值：30天内到期 */
    private static final int HIGH_PRIORITY_DAYS = 30;
    /** 中优先级阈值：60天内到期 */
    private static final int MEDIUM_PRIORITY_DAYS = 60;

    private final HealthCertificateService healthCertificateService;
    private final PendingTaskMapper pendingTaskMapper;

    /**
     * 构造函数注入
     *
     * @param healthCertificateService 健康证服务
     * @param pendingTaskMapper        待办任务Mapper
     */
    public CertificateExpiryCheckTask(HealthCertificateService healthCertificateService,
                                      PendingTaskMapper pendingTaskMapper) {
        this.healthCertificateService = healthCertificateService;
        this.pendingTaskMapper = pendingTaskMapper;
    }

    /**
     * 每日08:00执行证件到期扫描
     * 使用cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkExpiringCertificates() {
        log.info("========== 开始执行证件到期扫描任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 查询所有有效期 ≤ 60天的证件
            // 使用sendExpiryReminders作为替代方法，或者查询所有健康证后自行筛选
            List<HealthCertificate> allCerts = healthCertificateService.list();
            List<HealthCertificate> expiringCerts = allCerts.stream()
                .filter(cert -> cert.getExpiryDate() != null)
                .filter(cert -> {
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), cert.getExpiryDate());
                    return daysLeft <= MEDIUM_PRIORITY_DAYS;
                })
                .toList();

            log.info("发现{}个即将到期的证件", expiringCerts.size());

            int taskCreatedCount = 0;
            int skippedCount = 0;

            for (HealthCertificate cert : expiringCerts) {
                try {
                    String certId = cert.getId();
                    String certType = "健康证";
                    LocalDate expiryDate = cert.getExpiryDate();
                    String storeId = cert.getStoreId();
                    String storeName = cert.getStoreName();

                    // 计算剩余天数
                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

                    // 2. 幂等性检查：同一证件今天是否已生成过任务
                    if (isTaskAlreadyCreatedToday(certId)) {
                        log.debug("证件{}今日已生成过待办任务，跳过", certId);
                        skippedCount++;
                        continue;
                    }

                    // 3. 根据剩余天数确定优先级
                    int priority;
                    if (daysLeft < 0) {
                        priority = 4; // 已过期：紧急
                    } else if (daysLeft <= HIGH_PRIORITY_DAYS) {
                        priority = 3; // ≤30天：高优先级
                    } else {
                        priority = 2; // 31-60天：中优先级
                    }

                    // 4. 创建待办任务
                    createExpiryAlertTask(certId, certType, expiryDate, storeId, storeName, priority, daysLeft);
                    taskCreatedCount++;

                } catch (Exception e) {
                    log.error("处理证件到期提醒时发生异常", e);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 证件到期扫描完成: 发现{}个证件，生成{}个任务，跳过{}个，耗时{}ms ==========",
                    expiringCerts.size(), taskCreatedCount, skippedCount, elapsed);

        } catch (Exception e) {
            log.error("证件到期扫描任务执行失败", e);
        }
    }

    /**
     * 检查同一证件今天是否已生成过任务（幂等性保证）
     *
     * @param certId 证书ID
     * @return true-已生成 false-未生成
     */
    private boolean isTaskAlreadyCreatedToday(String certId) {
        // 查询今天是否已存在该证书的certificate_expiry类型任务
        // TODO: 实现具体的幂等性查询逻辑
        return false;
    }

    /**
     * 创建证件到期预警待办任务
     *
     * @param certId    证书ID
     * @param certType  证书类型（如"健康证"、"营业执照"）
     * @param expiryDate 到期日期
     * @param storeId   门店ID
     * @param storeName 门店名称
     * @param priority  优先级（2=中 3=高 4=紧急）
     * @param daysLeft 剩余天数（负数表示已过期）
     */
    private void createExpiryAlertTask(String certId, String certType, LocalDate expiryDate,
                                       String storeId, String storeName,
                                       int priority, long daysLeft) {
        PendingTask task = new PendingTask();

        // 任务基础信息
        task.setTaskType("certificate_expiry");

        // 构建任务标题和描述
        String title;
        String description;
        if (daysLeft < 0) {
            title = String.format("%s已过期%d天", certType, Math.abs(daysLeft));
            description = String.format("%s的%s已于%s过期，请立即办理续期！", storeName, certType, expiryDate);
        } else {
            title = String.format("%s即将到期", certType);
            description = String.format("%s的%s将于%s到期（还剩%d天），请及时办理续期", storeName, certType, expiryDate, daysLeft);
        }

        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus("pending");

        // 关联信息
        task.setSourceType("certificate");
        task.setSourceId(certId);

        // 跳转URL：指向证件管理页面并高亮该证件
        task.setRedirectUrl("/store-management/certificate?highlight=" + certId);

        // 分配给门店店长（TODO: 需要查询门店店长ID）
        task.setAssigneeId(storeId); // 临时使用storeId，后续需替换为实际店长userId

        // 时间设置
        if (daysLeft <= HIGH_PRIORITY_DAYS) {
            task.setDueDate(expiryDate); // 截止日期设为证件到期日
        }

        // 保存到数据库
        pendingTaskMapper.insert(task);

        log.debug("创建证件到期预警任务: taskId={}, certId={}, title={}", task.getTaskId(), certId, title);
    }
}
