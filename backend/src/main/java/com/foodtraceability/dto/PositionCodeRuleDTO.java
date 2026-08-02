package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 职位编码规则DTO
 */
@Schema(description = "职位编码规则")
public class PositionCodeRuleDTO {

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "职位名称关键词", example = "经理")
    private String keyword;

    @Schema(description = "职位编码", example = "MGR")
    private String code;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
