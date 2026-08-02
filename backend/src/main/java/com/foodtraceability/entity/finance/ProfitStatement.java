package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 利润表实体类
 * 存储月度/季度/年度利润表数据
 */
@TableName("profit_statements")
public class ProfitStatement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报表ID */
    @TableId(value = "statement_id", type = IdType.AUTO)
    private Long statementId;

    /**
     * 报表类型
     * 1-月度 2-季度 3-年度
     */
    @TableField("statement_type")
    private Integer statementType;

    /** 期间，如 "2026-04", "2026Q1", "2026" */
    @TableField("period")
    private String period;

    /** 营业收入（单位：分） */
    @TableField("revenue_amount")
    private Long revenueAmount;

    /** 营业成本（单位：分） */
    @TableField("cogs_amount")
    private Long cogsAmount;

    /** 毛利（单位：分） */
    @TableField("gross_profit")
    private Long grossProfit;

    /** 运营费用（单位：分） */
    @TableField("operating_expenses")
    private Long operatingExpenses;

    /** 净利润（单位：分） */
    @TableField("net_profit")
    private Long netProfit;

    /** 生成时间 */
    @TableField(value = "generate_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generateTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ========== Getter & Setter ==========

    public Long getStatementId() { return statementId; }
    public void setStatementId(Long statementId) { this.statementId = statementId; }

    public Integer getStatementType() { return statementType; }
    public void setStatementType(Integer statementType) { this.statementType = statementType; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public Long getRevenueAmount() { return revenueAmount; }
    public void setRevenueAmount(Long revenueAmount) { this.revenueAmount = revenueAmount; }

    public Long getCogsAmount() { return cogsAmount; }
    public void setCogsAmount(Long cogsAmount) { this.cogsAmount = cogsAmount; }

    public Long getGrossProfit() { return grossProfit; }
    public void setGrossProfit(Long grossProfit) { this.grossProfit = grossProfit; }

    public Long getOperatingExpenses() { return operatingExpenses; }
    public void setOperatingExpenses(Long operatingExpenses) { this.operatingExpenses = operatingExpenses; }

    public Long getNetProfit() { return netProfit; }
    public void setNetProfit(Long netProfit) { this.netProfit = netProfit; }

    public LocalDateTime getGenerateTime() { return generateTime; }
    public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
