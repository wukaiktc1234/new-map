package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.SystemConfigDTO;
import com.foodtraceability.entity.SystemConfig;
import com.foodtraceability.mapper.SystemConfigMapper;
import com.foodtraceability.service.SystemConfigService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemConfigServiceImpl.class);
    private final SystemConfigMapper systemConfigMapper;
    @Value("${config.encrypt.key:}")
    private String encryptKey;
    private static final String AES_ALGORITHM = "AES";

    @Override
    public Map<String, String> getTaxPlatformConfig() {
        Map<String, String> config = new HashMap<>();
        Long tenantId = getCurrentTenantId();
        SystemConfig enabledConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_ENABLED);
        SystemConfig testModeConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_TEST_MODE);
        SystemConfig urlConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_URL);
        SystemConfig appKeyConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_APP_KEY);
        SystemConfig appSecretConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_APP_SECRET);
        SystemConfig timeoutConfig = getConfig(tenantId, SystemConfigDTO.KEY_TAX_PLATFORM_TIMEOUT);
        config.put("enabled", enabledConfig != null ? enabledConfig.getConfigValue() : "true");
        config.put("testMode", testModeConfig != null ? testModeConfig.getConfigValue() : "true");
        config.put("url", urlConfig != null ? urlConfig.getConfigValue() : "https://inv-veri.chinatax.gov.cn");
        config.put("appKey", appKeyConfig != null ? decryptIfNeeded(appKeyConfig) : "");
        config.put("appSecret", appSecretConfig != null ? decryptIfNeeded(appSecretConfig) : "");
        config.put("timeout", timeoutConfig != null ? timeoutConfig.getConfigValue() : "30000");
        return config;
    }

    /**
     * 如果配置已加密，则解密返回
     */
    private String decryptIfNeeded(SystemConfig config) {
        if (config == null || config.getConfigValue() == null) {
            return "";
        }
        if (Boolean.TRUE.equals(config.getEncrypted())) {
            try {
                return decrypt(config.getConfigValue());
            } catch (Exception e) {
                log.warn("[配置解密] 解密失败: {}", e.getMessage());
                return config.getConfigValue();
            }
        }
        return config.getConfigValue();
    }

    /**
     * AES加密
     */
    private String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("[配置加密] 加密失败: {}", e.getMessage());
            return plainText;
        }
    }

    /**
     * AES解密
     */
    private String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[配置解密] 解密失败: {}", e.getMessage());
            return encryptedText;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTaxPlatformConfig(Map<String, String> config) {
        Long tenantId = getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        saveOrUpdateConfig(tenantId, userId, SystemConfigDTO.KEY_TAX_PLATFORM_ENABLED, config.getOrDefault("enabled", "true"), SystemConfigDTO.TYPE_TAX_PLATFORM, "税务平台启用状态", false);
        saveOrUpdateConfig(tenantId, userId, SystemConfigDTO.KEY_TAX_PLATFORM_URL, config.getOrDefault("url", "https://inv-veri.chinatax.gov.cn"), SystemConfigDTO.TYPE_TAX_PLATFORM, "税务平台URL", false);
        saveOrUpdateConfig(tenantId, userId, SystemConfigDTO.KEY_TAX_PLATFORM_APP_KEY, config.getOrDefault("appKey", ""), SystemConfigDTO.TYPE_TAX_PLATFORM, "税务平台API密钥", true);
        saveOrUpdateConfig(tenantId, userId, SystemConfigDTO.KEY_TAX_PLATFORM_APP_SECRET, config.getOrDefault("appSecret", ""), SystemConfigDTO.TYPE_TAX_PLATFORM, "税务平台API密钥密码", true);
        saveOrUpdateConfig(tenantId, userId, SystemConfigDTO.KEY_TAX_PLATFORM_TIMEOUT, config.getOrDefault("timeout", "30000"), SystemConfigDTO.TYPE_TAX_PLATFORM, "税务平台超时时间(毫秒)", false);
        log.info("[系统配置] 税务平台配置已保存");
    }

    @Override
    public String getConfigValue(String configKey) {
        Long tenantId = getCurrentTenantId();
        SystemConfig config = getConfig(tenantId, configKey);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setConfigValue(String configKey, String configValue, String configType, String description, Boolean encrypted) {
        Long tenantId = getCurrentTenantId();
        Long userId = SecurityUtils.getCurrentUserId();
        saveOrUpdateConfig(tenantId, userId, configKey, configValue, configType, description, encrypted);
    }

    @Override
    public List<SystemConfig> getAllConfigs() {
        Long tenantId = getCurrentTenantId();
        return systemConfigMapper.selectByTenant(tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SystemConfig config = systemConfigMapper.selectById(id);
        if (config != null) {
            config.setDeleted(1);
            config.setUpdatedBy(SecurityUtils.getCurrentUserId());
            config.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(config);
        }
    }

    @Override
    public boolean isTaxPlatformConfigured() {
        Map<String, String> config = getTaxPlatformConfig();
        String appKey = config.get("appKey");
        String appSecret = config.get("appSecret");
        return appKey != null && !appKey.isEmpty() && appSecret != null && !appSecret.isEmpty();
    }

    @Override
    public Map<String, Object> getTaxPlatformStatus() {
        Map<String, Object> status = new HashMap<>();
        Map<String, String> config = getTaxPlatformConfig();
        boolean configured = isTaxPlatformConfigured();
        status.put("configured", configured);
        status.put("enabled", Boolean.parseBoolean(config.getOrDefault("enabled", "true")));
        status.put("url", config.get("url"));
        status.put("hasAppKey", config.get("appKey") != null && !config.get("appKey").isEmpty());
        status.put("hasAppSecret", config.get("appSecret") != null && !config.get("appSecret").isEmpty());
        status.put("timeout", Integer.parseInt(config.getOrDefault("timeout", "30000")));
        if (configured) {
            status.put("statusDescription", "已配置税务平台API，可进行真实验真");
        } else {
            status.put("statusDescription", "未配置税务平台API，使用本地验证模式");
        }
        return status;
    }

    private SystemConfig getConfig(Long tenantId, String configKey) {
        return systemConfigMapper.selectByTenantAndKey(tenantId, configKey);
    }

    private void saveOrUpdateConfig(Long tenantId, Long userId, String configKey, String configValue, String configType, String description, Boolean encrypted) {
        SystemConfig config = getConfig(tenantId, configKey);
        if (config == null) {
            config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(configKey);
            config.setCreatedBy(userId);
            config.setCreatedAt(LocalDateTime.now());
        }
        boolean shouldEncrypt = Boolean.TRUE.equals(encrypted);
        String valueToSave = configValue;
        if (shouldEncrypt && configValue != null && !configValue.isEmpty()) {
            valueToSave = encrypt(configValue);
            log.info("[配置保存] 配置项 {} 已加密存储", configKey);
        }
        config.setConfigValue(valueToSave);
        config.setConfigType(configType);
        config.setDescription(description);
        config.setEncrypted(shouldEncrypt);
        config.setUpdatedBy(userId);
        config.setUpdatedAt(LocalDateTime.now());
        config.setDeleted(0);
        if (config.getId() == null) {
            systemConfigMapper.insert(config);
        } else {
            systemConfigMapper.updateById(config);
        }
    }

    private Long getCurrentTenantId() {
        try {
            return SecurityUtils.getCurrentTenantId();
        } catch (Exception e) {
            return 0L;
        }
    }

    public SystemConfigServiceImpl(final SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
