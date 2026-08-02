package com.foodtraceability.event;

import java.util.List;
import com.foodtraceability.entity.HealthCertificate;
import com.foodtraceability.entity.HealthCertificateExpense;
import com.foodtraceability.service.FinanceSystemService;
import com.foodtraceability.service.HealthCertificateExpenseService;
import com.foodtraceability.service.HrSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 健康证事件监听器
 * 用于处理健康证数据变更事件，确保数据同步和一致性
 */
@Component
public class HealthCertificateEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCertificateEventListener.class);
    

    public HealthCertificateEventListener(HrSystemService hrSystemService, FinanceSystemService financeSystemService, HealthCertificateExpenseService healthCertificateExpenseService) {
        this.hrSystemService = hrSystemService;
        this.financeSystemService = financeSystemService;
        this.healthCertificateExpenseService = healthCertificateExpenseService;
    }

    private final HrSystemService hrSystemService;
    
    private final FinanceSystemService financeSystemService;
    
    private final HealthCertificateExpenseService healthCertificateExpenseService;
    
    /**
     * 异步监听健康证创建事件
     * @param event 健康证事件
     */
    @EventListener
    @Async
    public void handleHealthCertificateCreateEvent(HealthCertificateEvent event) {
        if (HealthCertificateEvent.EventType.CREATE.equals(event.getEventType())) {
            handleHealthCertificateChange(event);
        }
    }
    
    /**
     * 异步监听健康证更新事件
     * @param event 健康证事件
     */
    @EventListener
    @Async
    public void handleHealthCertificateUpdateEvent(HealthCertificateEvent event) {
        if (HealthCertificateEvent.EventType.UPDATE.equals(event.getEventType())) {
            handleHealthCertificateChange(event);
        }
    }
    
    /**
     * 异步监听健康证删除事件
     * @param event 健康证事件
     */
    @EventListener
    @Async
    public void handleHealthCertificateDeleteEvent(HealthCertificateEvent event) {
        if (HealthCertificateEvent.EventType.DELETE.equals(event.getEventType())) {
            handleHealthCertificateChange(event);
        }
    }
    
    /**
     * 异步监听健康证审核通过事件
     * 审核通过后同步报销数据至财务系统
     * @param event 健康证审核通过事件
     */
    @EventListener
    @Async
    public void handleHealthCertificateApprovedEvent(HealthCertificateApprovedEvent event) {
        HealthCertificate healthCertificate = event.getHealthCertificate();
        log.info("处理健康证审核通过事件，健康证ID: {}", healthCertificate.getId());
        syncExpenseToFinance(healthCertificate);
    }
    
    /**
     * 异步监听健康证拒绝事件
     * @param event 健康证事件
     */
    @EventListener
    @Async
    public void handleHealthCertificateRejectEvent(HealthCertificateEvent event) {
        if (HealthCertificateEvent.EventType.REJECT.equals(event.getEventType())) {
            handleHealthCertificateChange(event);
        }
    }
    
    /**
     * 处理健康证数据变更事件
     * @param event 健康证事件
     */
    private void handleHealthCertificateChange(HealthCertificateEvent event) {
        HealthCertificate healthCertificate = event.getHealthCertificate();
        String eventType = event.getEventType().name();
        String operator = event.getOperator();
        String comment = event.getComment();

        log.info("处理健康证事件，事件类型: {}, 健康证ID: {}, 操作人: {}, 备注: {}",
                eventType, healthCertificate.getId(), operator, comment);

        try {
            // 同步至HR系统（财务同步由 HealthCertificateApprovedEvent 专门处理，避免重复）
            boolean hrSyncResult = hrSystemService.syncHealthCertificateToHrSystem(healthCertificate);
            if (hrSyncResult) {
                log.info("健康证同步至HR系统成功，健康证ID: {}", healthCertificate.getId());
            } else {
                log.error("健康证同步至HR系统失败，健康证ID: {}", healthCertificate.getId());
            }

            log.info("健康证事件处理完成，健康证ID: {}", healthCertificate.getId());

        } catch (Exception e) {
            log.error("处理健康证事件失败，健康证ID: {}, 错误信息: {}",
                    healthCertificate.getId(), e.getMessage(), e);
        }
    }

    /**
     * 同步健康证报销数据至财务系统
     * 根据健康证ID查找对应的报销记录并逐条同步
     * @param healthCertificate 健康证数据
     */
    private void syncExpenseToFinance(HealthCertificate healthCertificate) {
        try {
            // 根据健康证ID查找对应的报销记录
            List<HealthCertificateExpense> expenses = healthCertificateExpenseService.getByHealthCertificateId(healthCertificate.getId());
            if (expenses != null && !expenses.isEmpty()) {
                // 遍历所有报销记录并同步
                for (HealthCertificateExpense expense : expenses) {
                    boolean financeSyncResult = financeSystemService.syncHealthCertificateExpenseToFinance(expense);
                    if (financeSyncResult) {
                        log.info("健康证报销数据同步至财务系统成功，健康证ID: {}, 报销ID: {}", healthCertificate.getId(), expense.getId());
                    } else {
                        log.error("健康证报销数据同步至财务系统失败，健康证ID: {}, 报销ID: {}", healthCertificate.getId(), expense.getId());
                    }
                }
            } else {
                log.warn("未找到健康证对应的报销记录，健康证ID: {}", healthCertificate.getId());
            }
        } catch (Exception e) {
            log.error("同步健康证报销数据至财务系统失败，健康证ID: {}, 错误信息: {}",
                    healthCertificate.getId(), e.getMessage(), e);
        }
    }
}