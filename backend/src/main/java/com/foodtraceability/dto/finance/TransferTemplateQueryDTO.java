package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 结转模板查询DTO
 */
@Schema(description = "结转模板查询请求")
public class TransferTemplateQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板类型：1-损益结转 2-定期计提 3-其他")
    private Integer templateType;

    @Schema(description = "是否启用")
    private Boolean isEnabled;

    @Schema(description = "关键词搜索（模板名称）")
    private String keyword;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public Integer getTemplateType() {
        return templateType;
    }

    public void setTemplateType(Integer templateType) {
        this.templateType = templateType;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
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
