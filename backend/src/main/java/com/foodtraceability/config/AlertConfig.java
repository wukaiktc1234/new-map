package com.foodtraceability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 告警规则配置类
 * 用于加载和管理系统告警规则
 */
@Component
@ConfigurationProperties(prefix = "alert")
public class AlertConfig {
    
    /**
     * 是否启用告警系统
     */
    private boolean enabled;
    
    /**
     * 告警规则配置
     */
    private Map<String, AlertRule> rules;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, AlertRule> getRules() {
        return rules;
    }

    public void setRules(Map<String, AlertRule> rules) {
        this.rules = rules;
    }
    
    /**
     * 告警规则内部类
     */
    public static class AlertRule {
        /**
         * 是否启用该规则
         */
        private boolean enabled;
        
        /**
         * 告警阈值
         */
        private double threshold;
        
        /**
         * 持续时间（秒）
         */
        private int duration;
        
        /**
         * 告警级别
         */
        private String severity;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }
}
