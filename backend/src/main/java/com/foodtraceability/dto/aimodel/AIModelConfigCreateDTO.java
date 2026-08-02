package com.foodtraceability.dto.aimodel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * AI 模型配置创建 DTO
 * 用于接收新增 AI 模型配置的请求参数
 */
@Schema(description = "AI 模型配置创建请求")
public class AIModelConfigCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模型名称 */
    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "规则引擎")
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String modelName;

    /** 模型类型（local/api） */
    @Schema(description = "模型类型（local-本地模型，api-API服务）", requiredMode = Schema.RequiredMode.REQUIRED, example = "local")
    @NotBlank(message = "模型类型不能为空")
    @Pattern(regexp = "^(local|api)$", message = "模型类型只能为 local 或 api")
    private String modelType;

    /** 接入地址 */
    @Schema(description = "接入地址（本地模型为\"内置规则引擎\"，API 模型为 URL）")
    @Size(max = 500, message = "接入地址长度不能超过500个字符")
    private String endpoint;

    /** API 密钥 */
    @Schema(description = "API 密钥")
    @Size(max = 255, message = "API 密钥长度不能超过255个字符")
    private String apiKey;

    /** 置信度（high/medium/low，默认 medium） */
    @Schema(description = "置信度（high/medium/low）", example = "medium")
    @Pattern(regexp = "^(high|medium|low)$", message = "置信度只能为 high、medium 或 low")
    private String confidence;

    /** 描述 */
    @Schema(description = "描述")
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /** 本地模型路径（仅 local 类型） */
    @Schema(description = "本地模型路径（仅 local 类型）")
    @Size(max = 500, message = "模型路径长度不能超过500个字符")
    private String modelPath;

    /** 本地模型参数（仅 local 类型） */
    @Schema(description = "本地模型参数（仅 local 类型）")
    private String modelParams;

    /** API 超时秒数（仅 api 类型，默认 30） */
    @Schema(description = "API 超时秒数（仅 api 类型，默认 30）", example = "30")
    private Integer timeout;

    /** 服务商标识 */
    @Schema(description = "服务商标识", example = "openai")
    @Size(max = 64, message = "服务商标识长度不能超过64个字符")
    private String provider;

    /** 模型版本（仅 api 类型） */
    @Schema(description = "模型版本（仅 api 类型）", example = "gpt-4")
    @Size(max = 128, message = "模型版本长度不能超过128个字符")
    private String modelVersion;

    /** 默认模型名称（仅 api 类型） */
    @Schema(description = "默认模型名称（仅 api 类型）")
    @Size(max = 128, message = "默认模型名称长度不能超过128个字符")
    private String defaultModel;

    /** 温度参数（仅 api 类型，默认 0.7） */
    @Schema(description = "温度参数（仅 api 类型，默认 0.7）", example = "0.7")
    private Double temperature;

    /** 最大 token 数（仅 api 类型，默认 2048） */
    @Schema(description = "最大 token 数（仅 api 类型，默认 2048）", example = "2048")
    private Integer maxTokens;

    /** 上下文长度（仅 api 类型，默认 4096） */
    @Schema(description = "上下文长度（仅 api 类型，默认 4096）", example = "4096")
    private Integer contextLength;

    /** 重试次数（默认 3） */
    @Schema(description = "重试次数（默认 3）", example = "3")
    private Integer maxRetries;

    // ==================== Getter & Setter ====================

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
}
