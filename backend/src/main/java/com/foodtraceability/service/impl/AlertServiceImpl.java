package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.config.AlertConfig;
import com.foodtraceability.entity.AlertEntity;
import com.foodtraceability.mapper.AlertEntityMapper;
import com.foodtraceability.service.AlertNotificationService;
import com.foodtraceability.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警服务实现类
 * 基于数据库持久化存储，替代原内存HashMap存储
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";

    private final AlertEntityMapper alertEntityMapper;
    private final AlertConfig alertConfig;

    public AlertServiceImpl(AlertEntityMapper alertEntityMapper, AlertConfig alertConfig) {
        this.alertEntityMapper = alertEntityMapper;
        this.alertConfig = alertConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertEntity createAlert(AlertEntity alert) {
        alert.setAlertTime(LocalDateTime.now());
        alert.setStatus(STATUS_ACTIVE);
        alertEntityMapper.insert(alert);
        sendAlertNotification(alert);
        log.info("创建告警: alertId={}, name={}, severity={}", alert.getAlertId(), alert.getName(), alert.getSeverity());
        return alert;
    }

    @Override
    public IPage<AlertEntity> getAlertPage(Page<AlertEntity> page, String severity, String status, String source) {
        return alertEntityMapper.selectAlertPage(page, severity, status, source);
    }

    @Override
    public List<AlertEntity> getAllAlerts() {
        LambdaQueryWrapper<AlertEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AlertEntity::getAlertTime);
        wrapper.last("LIMIT 500");
        return alertEntityMapper.selectList(wrapper);
    }

    @Override
    public Optional<AlertEntity> getAlertById(Long alertId) {
        return Optional.ofNullable(alertEntityMapper.selectById(alertId));
    }

    @Override
    public List<AlertEntity> getAlertsByStatus(String status) {
        LambdaQueryWrapper<AlertEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertEntity::getStatus, status);
        return alertEntityMapper.selectList(wrapper);
    }

    @Override
    public List<AlertEntity> getAlertsBySeverity(String severity) {
        LambdaQueryWrapper<AlertEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertEntity::getSeverity, severity);
        return alertEntityMapper.selectList(wrapper);
    }

    @Override
    public List<AlertEntity> getActiveAlerts() {
        return alertEntityMapper.selectActiveAlerts();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertEntity acknowledgeAlert(Long alertId, String acknowledgedBy) {
        AlertEntity alert = alertEntityMapper.selectById(alertId);
        if (alert != null) {
            alert.setStatus(STATUS_ACKNOWLEDGED);
            alert.setAcknowledgeTime(LocalDateTime.now());
            alert.setAcknowledgedBy(acknowledgedBy);
            alertEntityMapper.updateById(alert);
            log.info("确认告警: alertId={}, acknowledgedBy={}", alertId, acknowledgedBy);
        }
        return alert;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertEntity resolveAlert(Long alertId, String resolvedBy, String resolveDescription) {
        AlertEntity alert = alertEntityMapper.selectById(alertId);
        if (alert != null) {
            alert.setStatus(STATUS_RESOLVED);
            alert.setResolveTime(LocalDateTime.now());
            alert.setResolvedBy(resolvedBy);
            alert.setResolveDescription(resolveDescription);
            alertEntityMapper.updateById(alert);
            log.info("解决告警: alertId={}, resolvedBy={}", alertId, resolvedBy);
        }
        return alert;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAlert(Long alertId) {
        int rows = alertEntityMapper.deleteById((java.io.Serializable) alertId);
        log.info("删除告警: alertId={}", alertId);
        return rows > 0;
    }

    @Override
    public boolean checkAlertCondition(String metricName, double value) {
        if (!alertConfig.isEnabled()) {
            return false;
        }
        AlertConfig.AlertRule rule = alertConfig.getRules().get(metricName);
        if (rule == null || !rule.isEnabled()) {
            return false;
        }
        return value > rule.getThreshold();
    }

    @Override
    public void sendAlertNotification(AlertEntity alert) {
        log.warn("发送告警通知: [{}] {} - 告警值: {}, 阈值: {}",
                alert.getSeverity(), alert.getDescription(), alert.getValue(), alert.getThreshold());
    }

    @Override
    public long countBySeverity(String severity) {
        return alertEntityMapper.countBySeverity(severity);
    }

    @Override
    public long countByStatus(String status) {
        return alertEntityMapper.countByStatus(status);
    }
}
