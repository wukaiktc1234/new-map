package com.foodtraceability.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * KDS 认证配置属性。
 * <p>
 * 从 application.yml 读取 KDS 设备凭证：
 * <pre>
 * app.kds:
 *   enabled: true
 *   tokens:
 *     "1": "kds-store1-secret-abc"
 *     "2": "kds-store2-secret-def"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.kds")
public class KdsAuthProperties {

    private boolean enabled = false;
    private Map<String, String> tokens = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, String> tokens) {
        this.tokens = tokens;
    }

    /**
     * 校验 storeId 对应的 secret 是否匹配。
     */
    public boolean isValidToken(String storeId, String secret) {
        if (storeId == null || secret == null) {
            return false;
        }
        String expectedSecret = tokens.get(storeId);
        if (expectedSecret == null) {
            return false;
        }
        return expectedSecret.equals(secret);
    }
}
