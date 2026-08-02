package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.entity.report.ReportConfig;
import com.foodtraceability.mapper.ReportConfigMapper;
import com.foodtraceability.service.ReportConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报表配置服务实现类
 * 提供报表用户配置的增删改查
 */
@Service
public class ReportConfigServiceImpl implements ReportConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ReportConfigServiceImpl.class);

    private final ReportConfigMapper reportConfigMapper;

    /**
     * 本地缓存，用于减少数据库查询压力
     * 结构：userId -> reportType -> configKey -> configValue
     */
    private final Map<Long, Map<Integer, Map<String, String>>> configCache = new ConcurrentHashMap<>();

    public ReportConfigServiceImpl(ReportConfigMapper reportConfigMapper) {
        this.reportConfigMapper = reportConfigMapper;
    }

    @Override
    public Map<String, String> getUserConfig(Long userId, Integer reportType) {
        logger.debug("获取用户配置，用户ID: {}, 报表类型: {}", userId, reportType);

        // 先查缓存
        Map<Integer, Map<String, String>> userCache = configCache.get(userId);
        if (userCache != null) {
            Map<String, String> cached = userCache.get(reportType);
            if (cached != null) {
                logger.debug("命中本地缓存，用户ID: {}, 报表类型: {}", userId, reportType);
                return new HashMap<>(cached);
            }
        }

        QueryWrapper<ReportConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("report_type", reportType);
        List<ReportConfig> configs = reportConfigMapper.selectList(queryWrapper);

        Map<String, String> result = new HashMap<>();
        for (ReportConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }

        // 写入缓存
        configCache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(reportType, new HashMap<>(result));

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(Long userId, Integer reportType, String configKey, String configValue) {
        logger.info("保存报表配置，用户ID: {}, 报表类型: {}, key: {}", userId, reportType, configKey);

        // 清除缓存
        clearUserConfigCache(userId, reportType);

        // 查询是否已存在
        QueryWrapper<ReportConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("report_type", reportType)
                .eq("config_key", configKey);
        ReportConfig existing = reportConfigMapper.selectOne(queryWrapper);

        if (existing != null) {
            // 更新
            existing.setConfigValue(configValue);
            reportConfigMapper.updateById(existing);
            logger.debug("更新配置成功，configId: {}", existing.getConfigId());
        } else {
            // 新增
            ReportConfig config = new ReportConfig();
            config.setUserId(userId);
            config.setReportType(reportType);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            reportConfigMapper.insert(config);
            logger.debug("新增配置成功，configId: {}", config.getConfigId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveConfig(Long userId, Integer reportType, Map<String, String> configs) {
        logger.info("批量保存报表配置，用户ID: {}, 报表类型: {}, 数量: {}", userId, reportType, configs.size());

        // 清除缓存
        clearUserConfigCache(userId, reportType);

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            saveConfig(userId, reportType, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Map<String, String> getDefaultConfig(Integer reportType) {
        logger.debug("获取默认配置，报表类型: {}", reportType);

        Map<String, String> defaults = new HashMap<>();
        defaults.put("date_range", "last_30_days");
        defaults.put("store_ids", "all");
        defaults.put("channel", "all");
        defaults.put("compare_type", "yoy");
        defaults.put("kpi_order", "revenue,order_count,avg_check,profit");

        return defaults;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long userId, Integer reportType, String configKey) {
        logger.info("删除报表配置，用户ID: {}, 报表类型: {}, key: {}", userId, reportType, configKey);

        // 清除缓存
        clearUserConfigCache(userId, reportType);

        QueryWrapper<ReportConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("report_type", reportType)
                .eq("config_key", configKey);
        reportConfigMapper.delete(queryWrapper);
    }

    /**
     * 清除用户配置缓存
     * @param userId 用户ID
     * @param reportType 报表类型
     */
    private void clearUserConfigCache(Long userId, Integer reportType) {
        Map<Integer, Map<String, String>> userCache = configCache.get(userId);
        if (userCache != null) {
            userCache.remove(reportType);
            logger.debug("清除用户配置缓存，用户ID: {}, 报表类型: {}", userId, reportType);
        }
    }
}
