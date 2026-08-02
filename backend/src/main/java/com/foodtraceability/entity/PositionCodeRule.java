package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.foodtraceability.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@TableName("position_code_rules")
@Schema(description = "职位编码规则表")
public class PositionCodeRule extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("keyword")
    private String keyword;

    @TableField("code")
    private String code;

    @TableField("format_template")
    private String formatTemplate;

    @TableField("sort_order")
    private Integer sortOrder;

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

    public String getFormatTemplate() {
        return formatTemplate;
    }

    public void setFormatTemplate(String formatTemplate) {
        this.formatTemplate = formatTemplate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
