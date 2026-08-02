package com.foodtraceability.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 资产主数据查询条件DTO
 */
@Schema(description = "资产主数据查询条件")
public class AssetMasterQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "资产编号（模糊查询）")
    private String assetCode;

    @Schema(description = "资产名称（模糊查询）")
    private String assetName;

    @Schema(description = "资产类型")
    private String assetType;

    @Schema(description = "状态：idle-闲置 in_use-使用中 repairing-维修中 damaged-已损毁")
    private String status;

    @Schema(description = "所属分类ID")
    private Long categoryId;

    @Schema(description = "所属门店ID")
    private Long storeId;

    @Schema(description = "责任人ID")
    private Long custodianId;

    @Schema(description = "关键词（匹配编号/名称）")
    private String keyword;

    @Schema(description = "页码", defaultValue = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer size = 20;

    // ==================== Getter & Setter ====================

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getCustodianId() {
        return custodianId;
    }

    public void setCustodianId(Long custodianId) {
        this.custodianId = custodianId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
