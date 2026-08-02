package com.foodtraceability.dto.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 报表配置DTO
 * 用于报表配置的创建和更新
 */
public class ReportConfigDTO {

    /**
     * 配置ID（更新时使用）
     */
    private Long configId;

    /**
     * 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
     */
    @NotNull(message = "报表类型不能为空")
    private Integer reportType;

    /**
     * 配置项key
     */
    @NotBlank(message = "配置项key不能为空")
    private String configKey;

    /**
     * 配置值
     */
    @Size(max = 5000, message = "配置值长度不能超过5000字符")
    private String configValue;

    // ========== Getter & Setter ==========

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
