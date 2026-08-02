package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀请码费用记录实体类（财务占位符）
 * 对应数据库表：invitation_expense_record
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@TableName("invitation_expense_record")
@Schema(description = "邀请码费用记录实体")
public class InvitationExpenseRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "费用记录ID")
    private Long id;
    @Schema(description = "批次ID")
    private String batchId;
    @Schema(description = "档案ID")
    private Long archiveId;
    @Schema(description = "费用类型：EMAIL-邮件费用, SMS-短信费用")
    private String expenseType;
    @Schema(description = "数量")
    private Integer quantity;
    @Schema(description = "单价")
    private BigDecimal unitPrice;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "部门ID")
    private String departmentId;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "状态：PENDING-待同步, SYNCED-已同步, FAILED-同步失败")
    private String status;
    @Schema(description = "财务系统ID（对接后回填）")
    private String financialSystemId;
    @Schema(description = "同步时间")
    private LocalDateTime syncTime;
    @Schema(description = "同步失败原因")
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    // 费用类型常量
    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_SMS = "SMS";
    // 状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_FAILED = "FAILED";

    public InvitationExpenseRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public String getBatchId() {
        return this.batchId;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public String getExpenseType() {
        return this.expenseType;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public String getStatus() {
        return this.status;
    }

    public String getFinancialSystemId() {
        return this.financialSystemId;
    }

    public LocalDateTime getSyncTime() {
        return this.syncTime;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setBatchId(final String batchId) {
        this.batchId = batchId;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setExpenseType(final String expenseType) {
        this.expenseType = expenseType;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(final BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setFinancialSystemId(final String financialSystemId) {
        this.financialSystemId = financialSystemId;
    }

    public void setSyncTime(final LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvitationExpenseRecord)) return false;
        final InvitationExpenseRecord other = (InvitationExpenseRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$batchId = this.getBatchId();
        final java.lang.Object other$batchId = other.getBatchId();
        if (this$batchId == null ? other$batchId != null : !this$batchId.equals(other$batchId)) return false;
        final java.lang.Object this$expenseType = this.getExpenseType();
        final java.lang.Object other$expenseType = other.getExpenseType();
        if (this$expenseType == null ? other$expenseType != null : !this$expenseType.equals(other$expenseType)) return false;
        final java.lang.Object this$unitPrice = this.getUnitPrice();
        final java.lang.Object other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !this$unitPrice.equals(other$unitPrice)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$financialSystemId = this.getFinancialSystemId();
        final java.lang.Object other$financialSystemId = other.getFinancialSystemId();
        if (this$financialSystemId == null ? other$financialSystemId != null : !this$financialSystemId.equals(other$financialSystemId)) return false;
        final java.lang.Object this$syncTime = this.getSyncTime();
        final java.lang.Object other$syncTime = other.getSyncTime();
        if (this$syncTime == null ? other$syncTime != null : !this$syncTime.equals(other$syncTime)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvitationExpenseRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $batchId = this.getBatchId();
        result = result * PRIME + ($batchId == null ? 43 : $batchId.hashCode());
        final java.lang.Object $expenseType = this.getExpenseType();
        result = result * PRIME + ($expenseType == null ? 43 : $expenseType.hashCode());
        final java.lang.Object $unitPrice = this.getUnitPrice();
        result = result * PRIME + ($unitPrice == null ? 43 : $unitPrice.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $financialSystemId = this.getFinancialSystemId();
        result = result * PRIME + ($financialSystemId == null ? 43 : $financialSystemId.hashCode());
        final java.lang.Object $syncTime = this.getSyncTime();
        result = result * PRIME + ($syncTime == null ? 43 : $syncTime.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvitationExpenseRecord(id=" + this.getId() + ", batchId=" + this.getBatchId() + ", archiveId=" + this.getArchiveId() + ", expenseType=" + this.getExpenseType() + ", quantity=" + this.getQuantity() + ", unitPrice=" + this.getUnitPrice() + ", totalAmount=" + this.getTotalAmount() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", status=" + this.getStatus() + ", financialSystemId=" + this.getFinancialSystemId() + ", syncTime=" + this.getSyncTime() + ", errorMessage=" + this.getErrorMessage() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
