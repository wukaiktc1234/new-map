package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 桌台创建DTO
 * 用于创建新的桌台记录
 */
@Schema(description = "桌台创建DTO")
public class DiningTableCreateDTO {

    /** 桌台编码 */
    @NotBlank(message = "桌台编码不能为空")
    @Size(max = 20, message = "桌台编码最长20字符")
    @Schema(description = "桌台编码", example = "A01", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tableCode;

    /** 桌台名称 */
    @Size(max = 50, message = "桌台名称最长50字符")
    @Schema(description = "桌台名称", example = "大厅A01桌")
    private String tableName;

    /** 区域ID */
    @Schema(description = "区域ID")
    private Long areaId;

    /** 座位数 */
    @NotNull(message = "座位数不能为空")
    @Min(value = 1, message = "座位数最少1人")
    @Max(value = 50, message = "座位数最多50人")
    @Schema(description = "座位数", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer seatsCount;

    /** 桌台类型：1大厅 2包厢 3吧台 4户外 */
    @Schema(description = "桌台类型：1大厅 2包厢 3吧台 4户外", example = "1")
    private Integer tableType;

    /** 最少建议人数 */
    @Min(value = 1, message = "最少人数不能小于1")
    @Schema(description = "最少建议人数")
    private Integer minPeople;

    /** 最多建议人数 */
    @Max(value = 50, message = "最多人数不能超过50")
    @Schema(description = "最多建议人数")
    private Integer maxPeople;

    /** 排序权重 */
    @Schema(description = "排序权重")
    private Integer sortOrder;

    // ==================== Getter & Setter ====================

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Integer getSeatsCount() {
        return seatsCount;
    }

    public void setSeatsCount(Integer seatsCount) {
        this.seatsCount = seatsCount;
    }

    public Integer getTableType() {
        return tableType;
    }

    public void setTableType(Integer tableType) {
        this.tableType = tableType;
    }

    public Integer getMinPeople() {
        return minPeople;
    }

    public void setMinPeople(Integer minPeople) {
        this.minPeople = minPeople;
    }

    public Integer getMaxPeople() {
        return maxPeople;
    }

    public void setMaxPeople(Integer maxPeople) {
        this.maxPeople = maxPeople;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
