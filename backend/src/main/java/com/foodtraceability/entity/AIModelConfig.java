package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 模型配置实体
 * 用于管理智能补货建议的 AI 模型接入，支持 local（本地规则引擎）和 api（外部 AI 服务）两种类型
 */
@TableName("ai_model_configs")
@Schema(description = "AI 模型配置实体")
public class AIModelConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Integer id;

    /**
     * 业务编码（唯一）
     */
    @TableField("model_code")
    @Schema(description = "业务编码", example = "model-001")
    private String modelCode;

    /**
     * 模型名称
     */
    @TableField("model_name")
    @Schema(description = "模型名称", example = "规则引擎")
    private String modelName;

    /**
     * 模型类型（local-本地模型，api-API服务）
     */
    @TableField("model_type")
    @Schema(description = "模型类型（local/api）", example = "local")
    private String modelType;

    /**
     * 服务商标识（openai/azure/anthropic/qwen/deepseek 等）
     */
    @TableField("provider")
    @Schema(description = "服务商标识", example = "openai")
    private String provider;

    /**
     * 接入地址（本地模型为"内置规则引擎"，API 模型为 URL）
     */
    @TableField("endpoint")
    @Schema(description = "接入地址")
    private String endpoint;

    /**
     * API 密钥（敏感字段，需脱敏）
     */
    @TableField("api_key")
    @Schema(description = "API 密钥")
    private String apiKey;

    /**
     * 状态（active-启用，inactive-禁用）
     */
    @TableField("status")
    @Schema(description = "状态（active/inactive）", example = "active")
    private String status;

    /**
     * 置信度（high-高，medium-中，low-低）
     */
    @TableField("confidence")
    @Schema(description = "置信度（high/medium/low）", example = "medium")
    private String confidence;

    /**
     * 描述
     */
    @TableField("description")
    @Schema(description = "描述")
    private String description;

    /**
     * 最后同步时间
     */
    @TableField("last_sync_time")
    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    /**
     * 本地模型路径（仅 local 类型）
     */
    @TableField("model_path")
    @Schema(description = "本地模型路径（仅 local 类型）")
    private String modelPath;

    /**
     * 本地模型参数（仅 local 类型）
     */
    @TableField("model_params")
    @Schema(description = "本地模型参数（仅 local 类型）")
    private String modelParams;

    /**
     * API 超时秒数（仅 api 类型，默认 30）
     */
    @TableField("timeout")
    @Schema(description = "API 超时秒数（仅 api 类型，默认 30）")
    private Integer timeout;

    /**
     * 模型版本（仅 api 类型，如 gpt-4）
     */
    @TableField("model_version")
    @Schema(description = "模型版本（仅 api 类型）")
    private String modelVersion;

    /**
     * 默认模型名称（仅 api 类型）
     */
    @TableField("default_model")
    @Schema(description = "默认模型名称（仅 api 类型）")
    private String defaultModel;

    /**
     * 温度参数（仅 api 类型，默认 0.7）
     */
    @TableField("temperature")
    @Schema(description = "温度参数（仅 api 类型，默认 0.7）")
    private Double temperature;

    /**
     * 最大 token 数（仅 api 类型，默认 2048）
     */
    @TableField("max_tokens")
    @Schema(description = "最大 token 数（仅 api 类型，默认 2048）")
    private Integer maxTokens;

    /**
     * 上下文长度（仅 api 类型，默认 4096）
     */
    @TableField("context_length")
    @Schema(description = "上下文长度（仅 api 类型，默认 4096）")
    private Integer contextLength;

    /**
     * 重试次数（默认 3）
     */
    @TableField("max_retries")
    @Schema(description = "重试次数（默认 3）")
    private Integer maxRetries;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private String createdBy;

    /**
     * 更新人ID
     */
    @TableField("updated_by")
    @Schema(description = "更新人ID")
    private String updatedBy;

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public AIModelConfig() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getModelCode() {
        return this.modelCode;
    }

    public String getModelName() {
        return this.modelName;
    }

    public String getModelType() {
        return this.modelType;
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public String getStatus() {
        return this.status;
    }

    public String getConfidence() {
        return this.confidence;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getLastSyncTime() {
        return this.lastSyncTime;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public String getModelParams() {
        return this.modelParams;
    }

    public Integer getTimeout() {
        return this.timeout;
    }

    public String getProvider() {
        return this.provider;
    }

    public String getModelVersion() {
        return this.modelVersion;
    }

    public String getDefaultModel() {
        return this.defaultModel;
    }

    public Double getTemperature() {
        return this.temperature;
    }

    public Integer getMaxTokens() {
        return this.maxTokens;
    }

    public Integer getContextLength() {
        return this.contextLength;
    }

    public Integer getMaxRetries() {
        return this.maxRetries;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public void setModelCode(final String modelCode) {
        this.modelCode = modelCode;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    public void setModelType(final String modelType) {
        this.modelType = modelType;
    }

    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setConfidence(final String confidence) {
        this.confidence = confidence;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setLastSyncTime(final LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public void setModelPath(final String modelPath) {
        this.modelPath = modelPath;
    }

    public void setModelParams(final String modelParams) {
        this.modelParams = modelParams;
    }

    public void setTimeout(final Integer timeout) {
        this.timeout = timeout;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public void setModelVersion(final String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public void setDefaultModel(final String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public void setTemperature(final Double temperature) {
        this.temperature = temperature;
    }

    public void setMaxTokens(final Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setContextLength(final Integer contextLength) {
        this.contextLength = contextLength;
    }

    public void setMaxRetries(final Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.Override
    public String toString() {
        return "AIModelConfig(id=" + this.getId() + ", modelCode=" + this.getModelCode()
                + ", modelName=" + this.getModelName() + ", modelType=" + this.getModelType()
                + ", status=" + this.getStatus() + ", confidence=" + this.getConfidence() + ")";
    }
}
