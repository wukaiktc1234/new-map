package com.foodtraceability.dto.aimodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置视图对象
 * 用于返回 AI 模型配置的展示数据（apiKey 已脱敏）
 */
@Schema(description = "AI 模型配置视图对象")
public class AIModelConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Schema(description = "主键ID")
    private Integer id;

    /** 业务编码 */
    @Schema(description = "业务编码", example = "model-001")
    private String modelCode;

    /** 模型名称 */
    @Schema(description = "模型名称")
    private String modelName;

    /** 模型类型（local/api） */
    @Schema(description = "模型类型（local/api）")
    private String modelType;

    /** 服务商标识 */
    @Schema(description = "服务商标识")
    private String provider;

    /** 接入地址 */
    @Schema(description = "接入地址")
    private String endpoint;

    /** API 密钥（脱敏后：保留前3位+****） */
    @Schema(description = "API 密钥（已脱敏）")
    private String apiKey;

    /** 状态（active/inactive） */
    @Schema(description = "状态（active/inactive）")
    private String status;

    /** 置信度（high/medium/low） */
    @Schema(description = "置信度（high/medium/low）")
    private String confidence;

    /** 描述 */
    @Schema(description = "描述")
    private String description;

    /** 最后同步时间 */
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    /** 本地模型路径（仅 local 类型） */
    @Schema(description = "本地模型路径（仅 local 类型）")
    private String modelPath;

    /** 本地模型参数（仅 local 类型） */
    @Schema(description = "本地模型参数（仅 local 类型）")
    private String modelParams;

    /** API 超时秒数（仅 api 类型） */
    @Schema(description = "API 超时秒数（仅 api 类型）")
    private Integer timeout;

    /** 模型版本（仅 api 类型） */
    @Schema(description = "模型版本（仅 api 类型）")
    private String modelVersion;

    /** 默认模型名称（仅 api 类型） */
    @Schema(description = "默认模型名称（仅 api 类型）")
    private String defaultModel;

    /** 温度参数（仅 api 类型） */
    @Schema(description = "温度参数（仅 api 类型）")
    private Double temperature;

    /** 最大 token 数（仅 api 类型） */
    @Schema(description = "最大 token 数（仅 api 类型）")
    private Integer maxTokens;

    /** 上下文长度（仅 api 类型） */
    @Schema(description = "上下文长度（仅 api 类型）")
    private Integer contextLength;

    /** 重试次数 */
    @Schema(description = "重试次数")
    private Integer maxRetries;

    /** 创建人ID */
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 更新人ID */
    @Schema(description = "更新人ID")
    private String updatedBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // ==================== Getter & Setter ====================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getModelParams() {
        return modelParams;
    }

    public void setModelParams(String modelParams) {
        this.modelParams = modelParams;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getContextLength() {
        return contextLength;
    }

    public void setContextLength(Integer contextLength) {
        this.contextLength = contextLength;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
