package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 资金流水查询DTO
 */
@Schema(description = "资金流水查询请求")
public class FundFlowQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "银行账户ID")
    private Long accountId;

    @Schema(description = "方向：1-收入 2-支出")
    private Integer flowDirection;

    @Schema(description = "分类：1-销售收款 2-采购付款 3-工资发放 4-税费缴纳 5-内部转账 6-其他")
    private Integer flowCategory;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "关键词搜索（对方名称/流水号）")
    private String keyword;

    @Schema(description = "当前页码")
    private Integer current = 1;

    @Schema(description = "每页大小")
    private Integer size = 20;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(Integer flowDirection) {
        this.flowDirection = flowDirection;
    }

    public Integer getFlowCategory() {
        return flowCategory;
    }

    public void setFlowCategory(Integer flowCategory) {
        this.flowCategory = flowCategory;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
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
