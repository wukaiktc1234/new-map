package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.AlertEntity;

import java.util.List;
import java.util.Optional;

/**
 * 告警服务接口
 * 提供告警的产生、确认、解决等操作，基于数据库持久化存储
 */
public interface AlertService {

    AlertEntity createAlert(AlertEntity alert);

    IPage<AlertEntity> getAlertPage(Page<AlertEntity> page, String severity, String status, String source);

    List<AlertEntity> getAllAlerts();

    Optional<AlertEntity> getAlertById(Long alertId);

    List<AlertEntity> getAlertsByStatus(String status);

    List<AlertEntity> getAlertsBySeverity(String severity);

    List<AlertEntity> getActiveAlerts();

    AlertEntity acknowledgeAlert(Long alertId, String acknowledgedBy);

    AlertEntity resolveAlert(Long alertId, String resolvedBy, String resolveDescription);

    boolean deleteAlert(Long alertId);

    boolean checkAlertCondition(String metricName, double value);

    void sendAlertNotification(AlertEntity alert);

    long countBySeverity(String severity);

    long countByStatus(String status);
}
