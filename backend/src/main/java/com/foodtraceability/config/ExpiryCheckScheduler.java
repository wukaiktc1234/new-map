package com.foodtraceability.config;

import com.foodtraceability.entity.ExpiryWarningRecord;
import com.foodtraceability.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 临期预警定时任务
 * 定期扫描临期物品并生成预警记录
 */
@Component
public class ExpiryCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiryCheckScheduler.class);

    private final PurchaseLedgerMapper purchaseLedgerMapper;
    private final CertificateManagementMapper certificateManagementMapper;
    private final ExpiryWarningRuleMapper expiryWarningRuleMapper;
    private final ExpiryWarningRecordMapper expiryWarningRecordMapper;

    public ExpiryCheckScheduler(PurchaseLedgerMapper purchaseLedgerMapper,
                                 CertificateManagementMapper certificateManagementMapper,
                                 ExpiryWarningRuleMapper expiryWarningRuleMapper,
                                 ExpiryWarningRecordMapper expiryWarningRecordMapper) {
        this.purchaseLedgerMapper = purchaseLedgerMapper;
        this.certificateManagementMapper = certificateManagementMapper;
        this.expiryWarningRuleMapper = expiryWarningRuleMapper;
        this.expiryWarningRecordMapper = expiryWarningRecordMapper;
    }

    /**
     * 每日检查临期物品（每天凌晨2点执行）
     * 扫描所有启用的预警规则，检查是否满足预警条件
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiringItems() {
        log.info("========== 开始执行临期预警检查任务 ==========");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有启用的预警规则
            List<com.foodtraceability.entity.ExpiryWarningRule> rules = expiryWarningRuleMapper.selectAllEnabled();

            for (com.foodtraceability.entity.ExpiryWarningRule rule : rules) {
                processWarningRule(rule);
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("========== 临期预警检查任务完成，耗时{}ms ==========", costTime);
        } catch (Exception e) {
            log.error("临期预警检查任务执行失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个预警规则
     */
    private void processWarningRule(com.foodtraceability.entity.ExpiryWarningRule rule) {
        int warningType = rule.getWarningType();
        int advanceDays = rule.getAdvanceDays();

        switch (warningType) {
            case 1:
                checkMaterialExpiry(rule, advanceDays);
                break;
            case 2:
                checkCertificateExpiry(rule, advanceDays);
                break;
            case 3:
                // 健康证临期检查 - TODO
                break;
            case 4:
                // 合同到期检查 - TODO
                break;
            default:
                log.warn("未知的预警类型: {}", warningType);
        }

        // 更新最后检查时间
        rule.setLastCheckTime(LocalDateTime.now());
        expiryWarningRuleMapper.updateById(rule);
    }

    /**
     * 检查原料临期
     */
    private void checkMaterialExpiry(com.foodtraceability.entity.ExpiryWarningRule rule, int advanceDays) {
        List<com.foodtraceability.entity.PurchaseLedger> expiringList =
                purchaseLedgerMapper.selectExpiringWithinDays(advanceDays);

        for (com.foodtraceability.entity.PurchaseLedger ledger : expiringList) {
            if (ledger.getExpiryDate() == null) continue;

            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), ledger.getExpiryDate());

            // 检查是否已有未处理的相同预警记录
            List<ExpiryWarningRecord> existingRecords =
                    expiryWarningRecordMapper.selectByTarget(1, ledger.getLedgerId());
            boolean alreadyWarned = existingRecords.stream()
                    .anyMatch(r -> !r.getIsHandled());

            if (!alreadyWarned && daysRemaining <= advanceDays) {
                createWarningRecord(rule, 1, ledger.getLedgerId(),
                        ledger.getMaterialName() + "(" + ledger.getBatchNo() + ")",
                        ledger.getExpiryDate(), (int) daysRemaining);
            }
        }
    }

    /**
     * 检查证件临期
     */
    private void checkCertificateExpiry(com.foodtraceability.entity.ExpiryWarningRule rule, int advanceDays) {
        List<com.foodtraceability.entity.CertificateManagement> expiringList =
                certificateManagementMapper.selectNeedReminder(LocalDate.now());

        for (com.foodtraceability.entity.CertificateManagement cert : expiringList) {
            if (cert.getExpiryDate() == null) continue;

            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), cert.getExpiryDate());

            List<ExpiryWarningRecord> existingRecords =
                    expiryWarningRecordMapper.selectByTarget(2, cert.getCertId());
            boolean alreadyWarned = existingRecords.stream()
                    .anyMatch(r -> !r.getIsHandled());

            if (!alreadyWarned && daysRemaining <= advanceDays) {
                createWarningRecord(rule, 2, cert.getCertId(),
                        cert.getCertName(),
                        cert.getExpiryDate(), (int) daysRemaining);
            }
        }
    }

    /**
     * 创建预警记录
     */
    private void createWarningRecord(com.foodtraceability.entity.ExpiryWarningRule rule,
                                     int targetType, Long targetId,
                                     String targetName, LocalDate expiryDate,
                                     int daysRemaining) {
        ExpiryWarningRecord record = new ExpiryWarningRecord();
        record.setRuleId(rule.getRuleId());
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        record.setTargetName(targetName);
        record.setExpiryDate(expiryDate);
        record.setDaysRemaining(daysRemaining);
        record.setWarningLevel(calculateWarningLevel(daysRemaining));
        record.setMessage(buildWarningMessage(targetType, targetName, daysRemaining));
        record.setIsHandled(false);

        expiryWarningRecordMapper.insert(record);
        log.info("生成预警记录: type={}, name={}, remaining={}天",
                targetType, targetName, daysRemaining);
    }

    /**
     * 计算预警级别
     */
    private Integer calculateWarningLevel(int daysRemaining) {
        if (daysRemaining < 0) return 3; // 已过期 - 严重
        if (daysRemaining <= 3) return 3; // 3天内 - 严重
        if (daysRemaining <= 7) return 2; // 7天内 - 紧急
        return 1; // 一般
    }

    /**
     * 构建预警消息
     */
    private String buildWarningMessage(int type, String name, int daysRemaining) {
        String typeDesc;
        switch (type) {
            case 1: typeDesc = "原料"; break;
            case 2: typeDesc = "证件"; break;
            case 3: typeDesc = "健康证"; break;
            case 4: typeDesc = "合同"; break;
            default: typeDesc = "项目"; break;
        }

        if (daysRemaining < 0) {
            return String.format("%s[%s]已过期%d天，请立即处理！", typeDesc, name, Math.abs(daysRemaining));
        }
        return String.format("%s[%s]将在%d天后到期，请及时关注！", typeDesc, name, daysRemaining);
    }
}
