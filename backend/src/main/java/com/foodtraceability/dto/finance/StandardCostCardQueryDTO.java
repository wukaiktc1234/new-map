package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 标准成本卡查询DTO
 */
@Schema(description = "标准成本卡查询请求")
public class StandardCostCardQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品名称（模糊搜索）")
    private String dishName;

    @Schema(description = "预警状态：0-正常 1-预警 2-异常")
    private Integer warningStatus;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public Integer getWarningStatus() {
        return warningStatus;
    }

    public void setWarningStatus(Integer warningStatus) {
        this.warningStatus = warningStatus;
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
