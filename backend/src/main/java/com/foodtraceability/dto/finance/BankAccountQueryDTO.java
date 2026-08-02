package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 银行账户查询DTO
 */
@Schema(description = "银行账户查询请求")
public class BankAccountQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户类型：1-基本户 2-一般户 3-备用金 4-其他")
    private Integer accountType;

    @Schema(description = "状态：1-启用 0-停用")
    private Integer status;

    @Schema(description = "关键词搜索（账户名称/银行名称）")
    private String keyword;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
