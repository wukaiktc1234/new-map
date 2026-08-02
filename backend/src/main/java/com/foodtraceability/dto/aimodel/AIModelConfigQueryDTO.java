package com.foodtraceability.dto.aimodel;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * AI 模型配置查询 DTO（分页参数 + 过滤条件）
 */
@Schema(description = "AI 模型配置查询 DTO")
public class AIModelConfigQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码（默认1） */
    @Schema(description = "当前页码", example = "1")
    private Integer page = 1;

    /** 每页条数（默认10） */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    /** 模型类型（local/api） */
    @Schema(description = "模型类型（local/api）")
    private String modelType;

    /** 服务商标识 */
    @Schema(description = "服务商标识")
    private String provider;

    /** 状态（active/inactive） */
    @Schema(description = "状态（active/inactive）")
    private String status;

    /** 搜索关键词（匹配 modelName 或 endpoint） */
    @Schema(description = "搜索关键词（匹配模型名称或接入地址）")
    private String keyword;

    // ==================== Getter & Setter ====================

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
