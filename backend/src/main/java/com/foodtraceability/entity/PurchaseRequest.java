package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@TableName("purchase_request")
public class PurchaseRequest {
    @TableId(value = "request_id", type = IdType.ASSIGN_UUID)
    private String requestId;
    @TableField("request_no")
    private String requestNo;
    @TableField("title")
    private String title;
    @TableField("request_type")
    private String requestType;
    @TableField("department_id")
    private String departmentId;
    @TableField("department_name")
    private String departmentName;
    @TableField("store_id")
    private String storeId;
    @TableField("applicant_id")
    private String applicantId;
    @TableField("applicant_name")
    private String applicantName;
    @TableField("total_amount")
    private Long totalAmount;
    @TableField("status")
    private String status;
    @TableField("priority")
    private String priority;
    @TableField("expected_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedDate;
    @TableField("description")
    private String description;
    @TableField("receiver_type")
    private String receiverType;
    @TableField("receiver_store_id")
    private String receiverStoreId;
    @TableField("receiver_warehouse_id")
    private Long receiverWarehouseId;
    @TableField("reject_reason")
    private String rejectReason;
    @TableField("reject_count")
    private Integer rejectCount;
    @TableField("approved_by")
    private String approvedBy;
    @TableField("approved_time")
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime approvedTime;
    @TableField("budget_id")
    private String budgetId;
    @TableField("budget_status")
    private String budgetStatus;
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime createTime;
    // 显式指定列名 create_by（与 H2 建表脚本对齐，避免依赖默认驼峰映射）
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    // 显式指定列名 update_by（与 H2 建表脚本对齐，避免依赖默认驼峰映射）
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;
    // 显式指定列名 update_time（与 create_time 保持一致风格）
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
    @TableField("deleted_by")
    private String deletedBy;
    @TableField("deleted_time")
    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    private LocalDateTime deletedTime;
    @TableField(exist = false)
    private List<PurchaseRequestItem> items;

    public PurchaseRequest() {
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getRequestNo() {
        return this.requestNo;
    }

    public String getTitle() {
        return this.title;
    }

    public String getRequestType() {
        return this.requestType;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getApplicantId() {
        return this.applicantId;
    }

    public String getApplicantName() {
        return this.applicantName;
    }

    public Long getTotalAmount() {
        return this.totalAmount;
    }

    public String getStatus() {
        return this.status;
    }

    public String getPriority() {
        return this.priority;
    }

    public LocalDate getExpectedDate() {
        return this.expectedDate;
    }

    public String getDescription() {
        return this.description;
    }

    public String getReceiverType() {
        return this.receiverType;
    }

    public String getReceiverStoreId() {
        return this.receiverStoreId;
    }

    public Long getReceiverWarehouseId() {
        return this.receiverWarehouseId;
    }

    public String getRejectReason() {
        return this.rejectReason;
    }

    public Integer getRejectCount() {
        return this.rejectCount;
    }

    public String getApprovedBy() {
        return this.approvedBy;
    }

    public LocalDateTime getApprovedTime() {
        return this.approvedTime;
    }

    public String getBudgetId() {
        return this.budgetId;
    }

    public String getBudgetStatus() {
        return this.budgetStatus;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public String getCreateBy() {
        return this.createBy;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public String getDeletedBy() {
        return this.deletedBy;
    }

    public LocalDateTime getDeletedTime() {
        return this.deletedTime;
    }

    public List<PurchaseRequestItem> getItems() {
        return this.items;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public void setRequestNo(final String requestNo) {
        this.requestNo = requestNo;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setRequestType(final String requestType) {
        this.requestType = requestType;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public void setApplicantId(final String applicantId) {
        this.applicantId = applicantId;
    }

    public void setApplicantName(final String applicantName) {
        this.applicantName = applicantName;
    }

    public void setTotalAmount(final Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setPriority(final String priority) {
        this.priority = priority;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    public void setExpectedDate(final LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setReceiverType(final String receiverType) {
        this.receiverType = receiverType;
    }

    public void setReceiverStoreId(final String receiverStoreId) {
        this.receiverStoreId = receiverStoreId;
    }

    public void setReceiverWarehouseId(final Long receiverWarehouseId) {
        this.receiverWarehouseId = receiverWarehouseId;
    }

    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public void setRejectCount(final Integer rejectCount) {
        this.rejectCount = rejectCount;
    }

    public void setApprovedBy(final String approvedBy) {
        this.approvedBy = approvedBy;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setApprovedTime(final LocalDateTime approvedTime) {
        this.approvedTime = approvedTime;
    }

    public void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    public void setBudgetStatus(final String budgetStatus) {
        this.budgetStatus = budgetStatus;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    public void setDeletedBy(final String deletedBy) {
        this.deletedBy = deletedBy;
    }

    @JsonFormat(pattern = "yyyy-MM-dd\'T\'HH:mm:ss")
    public void setDeletedTime(final LocalDateTime deletedTime) {
        this.deletedTime = deletedTime;
    }

    public void setItems(final List<PurchaseRequestItem> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseRequest)) return false;
        final PurchaseRequest other = (PurchaseRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$requestId = this.getRequestId();
        final java.lang.Object other$requestId = other.getRequestId();
        if (this$requestId == null ? other$requestId != null : !this$requestId.equals(other$requestId)) return false;
        final java.lang.Object this$requestNo = this.getRequestNo();
        final java.lang.Object other$requestNo = other.getRequestNo();
        if (this$requestNo == null ? other$requestNo != null : !this$requestNo.equals(other$requestNo)) return false;
        final java.lang.Object this$title = this.getTitle();
        final java.lang.Object other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
        final java.lang.Object this$requestType = this.getRequestType();
        final java.lang.Object other$requestType = other.getRequestType();
        if (this$requestType == null ? other$requestType != null : !this$requestType.equals(other$requestType)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$applicantId = this.getApplicantId();
        final java.lang.Object other$applicantId = other.getApplicantId();
        if (this$applicantId == null ? other$applicantId != null : !this$applicantId.equals(other$applicantId)) return false;
        final java.lang.Object this$applicantName = this.getApplicantName();
        final java.lang.Object other$applicantName = other.getApplicantName();
        if (this$applicantName == null ? other$applicantName != null : !this$applicantName.equals(other$applicantName)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$expectedDate = this.getExpectedDate();
        final java.lang.Object other$expectedDate = other.getExpectedDate();
        if (this$expectedDate == null ? other$expectedDate != null : !this$expectedDate.equals(other$expectedDate)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$rejectReason = this.getRejectReason();
        final java.lang.Object other$rejectReason = other.getRejectReason();
        if (this$rejectReason == null ? other$rejectReason != null : !this$rejectReason.equals(other$rejectReason)) return false;
        final java.lang.Object this$approvedBy = this.getApprovedBy();
        final java.lang.Object other$approvedBy = other.getApprovedBy();
        if (this$approvedBy == null ? other$approvedBy != null : !this$approvedBy.equals(other$approvedBy)) return false;
        final java.lang.Object this$approvedTime = this.getApprovedTime();
        final java.lang.Object other$approvedTime = other.getApprovedTime();
        if (this$approvedTime == null ? other$approvedTime != null : !this$approvedTime.equals(other$approvedTime)) return false;
        final java.lang.Object this$budgetId = this.getBudgetId();
        final java.lang.Object other$budgetId = other.getBudgetId();
        if (this$budgetId == null ? other$budgetId != null : !this$budgetId.equals(other$budgetId)) return false;
        final java.lang.Object this$budgetStatus = this.getBudgetStatus();
        final java.lang.Object other$budgetStatus = other.getBudgetStatus();
        if (this$budgetStatus == null ? other$budgetStatus != null : !this$budgetStatus.equals(other$budgetStatus)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$deletedBy = this.getDeletedBy();
        final java.lang.Object other$deletedBy = other.getDeletedBy();
        if (this$deletedBy == null ? other$deletedBy != null : !this$deletedBy.equals(other$deletedBy)) return false;
        final java.lang.Object this$deletedTime = this.getDeletedTime();
        final java.lang.Object other$deletedTime = other.getDeletedTime();
        if (this$deletedTime == null ? other$deletedTime != null : !this$deletedTime.equals(other$deletedTime)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseRequest;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $requestId = this.getRequestId();
        result = result * PRIME + ($requestId == null ? 43 : $requestId.hashCode());
        final java.lang.Object $requestNo = this.getRequestNo();
        result = result * PRIME + ($requestNo == null ? 43 : $requestNo.hashCode());
        final java.lang.Object $title = this.getTitle();
        result = result * PRIME + ($title == null ? 43 : $title.hashCode());
        final java.lang.Object $requestType = this.getRequestType();
        result = result * PRIME + ($requestType == null ? 43 : $requestType.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $applicantId = this.getApplicantId();
        result = result * PRIME + ($applicantId == null ? 43 : $applicantId.hashCode());
        final java.lang.Object $applicantName = this.getApplicantName();
        result = result * PRIME + ($applicantName == null ? 43 : $applicantName.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $expectedDate = this.getExpectedDate();
        result = result * PRIME + ($expectedDate == null ? 43 : $expectedDate.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $rejectReason = this.getRejectReason();
        result = result * PRIME + ($rejectReason == null ? 43 : $rejectReason.hashCode());
        final java.lang.Object $approvedBy = this.getApprovedBy();
        result = result * PRIME + ($approvedBy == null ? 43 : $approvedBy.hashCode());
        final java.lang.Object $approvedTime = this.getApprovedTime();
        result = result * PRIME + ($approvedTime == null ? 43 : $approvedTime.hashCode());
        final java.lang.Object $budgetId = this.getBudgetId();
        result = result * PRIME + ($budgetId == null ? 43 : $budgetId.hashCode());
        final java.lang.Object $budgetStatus = this.getBudgetStatus();
        result = result * PRIME + ($budgetStatus == null ? 43 : $budgetStatus.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $deletedBy = this.getDeletedBy();
        result = result * PRIME + ($deletedBy == null ? 43 : $deletedBy.hashCode());
        final java.lang.Object $deletedTime = this.getDeletedTime();
        result = result * PRIME + ($deletedTime == null ? 43 : $deletedTime.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseRequest(requestId=" + this.getRequestId() + ", requestNo=" + this.getRequestNo() + ", title=" + this.getTitle() + ", requestType=" + this.getRequestType() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", applicantId=" + this.getApplicantId() + ", applicantName=" + this.getApplicantName() + ", totalAmount=" + this.getTotalAmount() + ", status=" + this.getStatus() + ", priority=" + this.getPriority() + ", expectedDate=" + this.getExpectedDate() + ", description=" + this.getDescription() + ", rejectReason=" + this.getRejectReason() + ", approvedBy=" + this.getApprovedBy() + ", approvedTime=" + this.getApprovedTime() + ", budgetId=" + this.getBudgetId() + ", budgetStatus=" + this.getBudgetStatus() + ", createTime=" + this.getCreateTime() + ", createBy=" + this.getCreateBy() + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", deletedBy=" + this.getDeletedBy() + ", deletedTime=" + this.getDeletedTime() + ", items=" + this.getItems() + ")";
    }
}
