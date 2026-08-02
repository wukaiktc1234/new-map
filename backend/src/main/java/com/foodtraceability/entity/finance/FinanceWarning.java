package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;

/**
 * 财务风险预警实体类
 * 用于管理财务风险预警信息
 * @author example
 * @since 2025-12-05
 */
@TableName("finance_warnings")
public class FinanceWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预警类型：BUDGET_OVER(预算超支)、AR_OVERDUE(应收账款逾期)、INVENTORY_OVER(库存积压)、CASH_FLOW_RISK(现金流风险)、TAX_RISK(税务风险)
     */
    @TableField("warning_type")
    private String warningType;

    /**
     * 预警级别：LOW(低)、MEDIUM(中)、HIGH(高)、URGENT(紧急)
     */
    @TableField("warning_level")
    private String warningLevel;

    /**
     * 预警标题
     */
    @TableField("title")
    private String title;

    /**
     * 预警内容
     */
    @TableField("content")
    private String content;

    /**
     * 相关业务ID
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 相关业务类型
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 预警日期
     */
    @TableField("warning_date")
    private LocalDateTime warningDate;

    /**
     * 状态：UNHANDLED(未处理)、HANDLING(处理中)、RESOLVED(已解决)
     */
    @TableField("status")
    private String status;

    /**
     * 处理人
     */
    @TableField("handler")
    private String handler;

    /**
     * 处理时间
     */
    @TableField("handle_time")
    private LocalDateTime handleTime;

    /**
     * 处理结果
     */
    @TableField("handle_result")
    private String handleResult;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 逻辑删除标记：0未删除，1已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public String getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(String warningLevel) {
        this.warningLevel = warningLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public LocalDateTime getWarningDate() {
        return warningDate;
    }

    public void setWarningDate(LocalDateTime warningDate) {
        this.warningDate = warningDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleResult() {
        return handleResult;
    }

    public void setHandleResult(String handleResult) {
        this.handleResult = handleResult;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "FinanceWarning{" +
            "id=" + id +
            ", warningType='" + warningType + '\'' +
            ", warningLevel='" + warningLevel + '\'' +
            ", title='" + title + '\'' +
            ", content='" + content + '\'' +
            ", businessId='" + businessId + '\'' +
            ", businessType='" + businessType + '\'' +
            ", warningDate=" + warningDate +
            ", status='" + status + '\'' +
            ", handler='" + handler + '\'' +
            ", handleTime=" + handleTime +
            ", handleResult='" + handleResult + '\'' +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", createBy='" + createBy + '\'' +
            ", updateBy='" + updateBy + '\'' +
            ", deleted=" + deleted +
            '}';
    }
}
