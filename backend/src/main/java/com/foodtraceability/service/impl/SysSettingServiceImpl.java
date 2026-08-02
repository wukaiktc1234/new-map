package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.mapper.SysSettingMapper;
import com.foodtraceability.service.SysSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统设置Service实现类
 */
@Service
public class SysSettingServiceImpl extends ServiceImpl<SysSettingMapper, SysSetting> implements SysSettingService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SysSettingServiceImpl.class);

    public SysSettingServiceImpl() {
    }

    @Override
    public Result<List<SysSetting>> getSettingsByGroup(String group) {
        try {
            List<SysSetting> settings = baseMapper.selectByGroup(group);
            return Result.success(settings);
        } catch (Exception e) {
            log.error("获取设置列表失败", e);
            return Result.error("获取设置列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<String> getSettingValue(String key, String targetType, String targetId) {
        try {
            String value = null;
            // 1. 先查用户级配置
            if ("user".equals(targetType) && targetId != null) {
                value = getSettingValueByScope(key, "user", targetId);
            }
            // 2. 再查门店级配置
            if (value == null && targetId != null) {
                value = getSettingValueByScope(key, "store", targetId);
            }
            // 3. 最后查全局配置
            if (value == null) {
                SysSetting globalSetting = baseMapper.selectGlobalSetting(key);
                if (globalSetting != null) {
                    value = globalSetting.getSettingValue();
                }
            }
            return Result.success(value);
        } catch (Exception e) {
            log.error("获取设置值失败", e);
            return Result.error("获取设置值失败：" + e.getMessage());
        }
    }

    private String getSettingValueByScope(String key, String scopeType, String scopeId) {
        LambdaQueryWrapper<SysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSetting::getSettingKey, key).eq(SysSetting::getScopeType, scopeType).eq(SysSetting::getScopeId, scopeId).eq(SysSetting::getIsEnabled, 1).last("LIMIT 1");
        SysSetting setting = getOne(wrapper);
        return setting != null ? setting.getSettingValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setGlobalSetting(String key, String value, String currentUser) {
        return saveSetting(key, value, "global", null, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setStoreSetting(String key, String value, Long storeId, String currentUser) {
        return saveSetting(key, value, "store", storeId.toString(), currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> setUserSetting(String key, String value, String userId, String currentUser) {
        return saveSetting(key, value, "user", userId, currentUser);
    }

    private Result<Void> saveSetting(String key, String value, String scopeType, String scopeId, String currentUser) {
        log.info("开始保存设置: key={}, value={}, scopeType={}, scopeId={}, currentUser={}", key, value, scopeType, scopeId, currentUser);
        try {
            // 查询是否已存在
            LambdaQueryWrapper<SysSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysSetting::getSettingKey, key).eq(SysSetting::getScopeType, scopeType);
            if (scopeId != null) {
                wrapper.eq(SysSetting::getScopeId, scopeId);
            } else {
                wrapper.isNull(SysSetting::getScopeId);
            }
            SysSetting existing = getOne(wrapper);
            log.info("查询现有设置结果: existing={}", existing);
            if (existing != null) {
                // 更新
                log.info("更新现有设置: id={}", existing.getId());
                existing.setSettingValue(value);
                existing.setUpdatedBy(currentUser);
                existing.setUpdateTime(LocalDateTime.now());
                boolean updateResult = updateById(existing);
                log.info("更新结果: {}", updateResult);
            } else {
                // 新建
                log.info("创建新设置");
                SysSetting setting = new SysSetting();
                setting.setSettingKey(key);
                setting.setSettingValue(value);
                setting.setScopeType(scopeType);
                setting.setScopeId(scopeId);
                setting.setSettingGroup("basic"); // 默认分组
                setting.setValueType("string"); // 默认类型
                setting.setSettingName(key); // 使用key作为名称
                setting.setCreatedBy(currentUser);
                setting.setUpdatedBy(currentUser);
                setting.setCreateTime(LocalDateTime.now());
                setting.setUpdateTime(LocalDateTime.now());
                setting.setIsEnabled(1);
                setting.setIsSystem(0);
                setting.setSortOrder(0);
                log.info("准备保存设置: {}", setting);
                boolean saveResult = save(setting);
                log.info("保存结果: {}", saveResult);
            }
            return Result.success();
        } catch (Exception e) {
            log.error("保存设置失败: key={}, value={}, scopeType={}, scopeId={}", key, value, scopeType, scopeId, e);
            return Result.error("保存设置失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getSettingsBatch(List<String> keys, String targetType, String targetId) {
        try {
            Map<String, String> result = new HashMap<>();
            for (String key : keys) {
                Result<String> valueResult = getSettingValue(key, targetType, targetId);
                if (valueResult.isSuccess()) {
                    result.put(key, valueResult.getData());
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量获取设置失败", e);
            return Result.error("批量获取设置失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> initDefaultSettings() {
        try {
            // 初始化基础设置
            initSetting("basic", "system_name", "string", "餐饮管理系统", "系统名称", "global", null);
            initSetting("basic", "system_logo", "image", "", "系统Logo", "global", null);
            initSetting("basic", "system_language", "select", "zh-CN", "默认语言", "global", "[{\"label\":\"简体中文\",\"value\":\"zh-CN\"},{\"label\":\"English\",\"value\":\"en-US\"}]");
            initSetting("basic", "timezone", "select", "Asia/Shanghai", "时区", "global", "[{\"label\":\"北京时间\",\"value\":\"Asia/Shanghai\"},{\"label\":\"东京时间\",\"value\":\"Asia/Tokyo\"},{\"label\":\"纽约时间\",\"value\":\"America/New_York\"}]");
            initSetting("basic", "date_format", "select", "yyyy-MM-dd", "日期格式", "global", "[{\"label\":\"yyyy-MM-dd\",\"value\":\"yyyy-MM-dd\"},{\"label\":\"dd/MM/yyyy\",\"value\":\"dd/MM/yyyy\"},{\"label\":\"MM/dd/yyyy\",\"value\":\"MM/dd/yyyy\"}]");
            // 初始化安全设置
            initSetting("security", "password_min_length", "number", "8", "密码最小长度", "global", null);
            initSetting("security", "password_complexity", "select", "medium", "密码复杂度", "global", "[{\"label\":\"低\",\"value\":\"low\"},{\"label\":\"中\",\"value\":\"medium\"},{\"label\":\"高\",\"value\":\"high\"}]");
            initSetting("security", "login_max_attempts", "number", "5", "登录最大尝试次数", "global", null);
            initSetting("security", "login_lock_duration", "number", "30", "登录锁定时间（分钟）", "global", null);
            initSetting("security", "session_timeout", "number", "120", "会话超时时间（分钟）", "global", null);
            // 初始化通知设置
            initSetting("notification", "sms_enabled", "boolean", "false", "短信通知启用", "global", null);
            initSetting("notification", "email_enabled", "boolean", "false", "邮件通知启用", "global", null);
            initSetting("notification", "system_notification", "boolean", "true", "系统消息通知", "global", null);
            return Result.success();
        } catch (Exception e) {
            log.error("初始化默认设置失败", e);
            return Result.error("初始化默认设置失败：" + e.getMessage());
        }
    }

    private void initSetting(String group, String key, String valueType, String defaultValue, String name, String scopeType, String options) {
        LambdaQueryWrapper<SysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysSetting::getSettingKey, key).eq(SysSetting::getScopeType, scopeType).isNull(SysSetting::getScopeId);
        if (count(wrapper) == 0) {
            SysSetting setting = new SysSetting();
            setting.setSettingGroup(group);
            setting.setSettingKey(key);
            setting.setSettingValue(defaultValue);
            setting.setValueType(valueType);
            setting.setSettingName(name);
            setting.setScopeType(scopeType);
            setting.setOptions(options);
            setting.setIsEnabled(1);
            setting.setIsSystem(1);
            setting.setSortOrder(0);
            setting.setCreateTime(LocalDateTime.now());
            setting.setUpdateTime(LocalDateTime.now());
            save(setting);
        }
    }
}
