package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 税率配置查询DTO
 */
@Schema(description = "税率配置查询请求")
public class TaxRateConfigQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "税种：1-增值税 2-城建税 3-教育费附加 4-地方教育附加 5-企业所得税 6-个人所得税 7-印花税")
    private Integer taxType;

    @Schema(description = "纳税人类型：1-一般纳税人 2-小规模纳税人")
    private Integer taxpayerType;

    @Schema(description = "是否生效")
    private Boolean isActive;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public Integer getTaxType() {
        return taxType;
    }

    public void setTaxType(Integer taxType) {
        this.taxType = taxType;
    }

    public Integer getTaxpayerType() {
        return taxpayerType;
    }

    public void setTaxpayerType(Integer taxpayerType) {
        this.taxpayerType = taxpayerType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
