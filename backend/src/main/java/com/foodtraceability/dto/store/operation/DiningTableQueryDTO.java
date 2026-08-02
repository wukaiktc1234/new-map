package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 桌台查询DTO
 * 用于桌台列表的条件查询和分页
 */
@Schema(description = "桌台查询DTO")
public class DiningTableQueryDTO {

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 状态：1空闲 2用餐中 3预订 4维护中 5停用 */
    @Schema(description = "状态：1空闲 2用餐中 3预订 4维护中 5停用")
    private Integer status;

    /** 桌台类型：1大厅 2包厢 3吧台 4户外 */
    @Schema(description = "桌台类型：1大厅 2包厢 3吧台 4户外")
    private Integer tableType;

    /** 关键词（编码或名称） */
    @Schema(description = "关键词（编码或名称）")
    private String keyword;

    /** 页码 */
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    // ==================== Getter & Setter ====================

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getTableType() {
        return tableType;
    }

    public void setTableType(Integer tableType) {
        this.tableType = tableType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

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
}
