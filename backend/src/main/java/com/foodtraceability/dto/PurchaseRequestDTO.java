package com.foodtraceability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseRequestDTO {
    private String requestId;

    @NotBlank(message = "申请编号不能为空")
    @Size(max = 50, message = "申请编号长度不能超过50个字符")
    private String requestNo;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "申请类型不能为空")
    @Size(max = 50, message = "申请类型长度不能超过50个字符")
    private String requestType;

    @Size(max = 64, message = "部门ID长度不能超过64个字符")
    private String departmentId;

    @Size(max = 100, message = "部门名称长度不能超过100个字符")
    private String departmentName;

    @Size(max = 64, message = "门店ID长度不能超过64个字符")
    private String storeId;

    @Size(max = 64, message = "申请人ID长度不能超过64个字符")
    private String applicantId;

    @Size(max = 100, message = "申请人姓名长度不能超过100个字符")
    private String applicantName;
    /**
     * 总金额（单位：分）
     * 后端和数据库统一使用 Long 分，前端通过 DataConverter 转换为元
     */
    @NotNull(message = "总金额不能为空")
    @Min(value = 0, message = "总金额不能为负数")
    private Long totalAmount;

    @Size(max = 32, message = "状态长度不能超过32个字符")
    private String status;

    @Size(max = 32, message = "优先级长度不能超过32个字符")
    private String priority;

    private LocalDate expectedDate;

    @Size(max = 1000, message = "描述长度不能超过1000个字符")
    private String description;

    @Size(max = 500, message = "驳回原因长度不能超过500个字符")
    private String rejectReason;

    /** 驳回次数（达到 3 次后禁止重新提交，需管理员重置） */
    private Integer rejectCount;

    @Size(max = 64, message = "审批人长度不能超过64个字符")
    private String approvedBy;

    private LocalDateTime approvedTime;

    @Size(max = 64, message = "预算ID长度不能超过64个字符")
    private String budgetId;

    @Size(max = 32, message = "预算状态长度不能超过32个字符")
    private String budgetStatus;

    private LocalDateTime createTime;

    @Size(max = 64, message = "创建人长度不能超过64个字符")
    private String createBy;

    private LocalDateTime updateTime;

    @Valid
    private List<PurchaseRequestItemDTO> items;


    public static class PurchaseRequestItemDTO {
        private String itemId;

        @Size(max = 64, message = "申请ID长度不能超过64个字符")
        private String requestId;

        @Size(max = 64, message = "食品ID长度不能超过64个字符")
        private String foodId;

        @NotBlank(message = "食品名称不能为空")
        @Size(max = 200, message = "食品名称长度不能超过200个字符")
        private String foodName;

        @Size(max = 50, message = "食品编码长度不能超过50个字符")
        private String foodCode;

        @Size(max = 200, message = "规格长度不能超过200个字符")
        private String specification;

        @NotNull(message = "数量不能为空")
        @Min(value = 0, message = "数量不能为负数")
        private BigDecimal quantity;

        @NotBlank(message = "单位不能为空")
        @Size(max = 20, message = "单位长度不能超过20个字符")
        private String unit;

        @Min(value = 0, message = "预估单价不能为负数")
        private BigDecimal estimatedPrice;

        @Min(value = 0, message = "小计金额不能为负数")
        private BigDecimal subtotalAmount;

        @Size(max = 500, message = "备注长度不能超过500个字符")
        private String remark;

        /**
         * 计划收货方类型：STORE / WAREHOUSE
         */
        @Size(max = 20, message = "计划收货方类型不能超过20个字符")
        private String plannedReceiverType;

        /**
         * 计划收货门店ID
         */
        @Size(max = 50, message = "计划收货门店ID不能超过50个字符")
        private String plannedStoreId;

        /**
         * 计划收货仓库ID
         */
        private Long plannedWarehouseId;

        public PurchaseRequestItemDTO() {
        }

        public String getItemId() {
            return this.itemId;
        }

        public String getRequestId() {
            return this.requestId;
        }

        public String getFoodId() {
            return this.foodId;
        }

        public String getFoodName() {
            return this.foodName;
        }

        public String getFoodCode() {
            return this.foodCode;
        }

        public String getSpecification() {
            return this.specification;
        }

        public BigDecimal getQuantity() {
            return this.quantity;
        }

        public String getUnit() {
            return this.unit;
        }

        public BigDecimal getEstimatedPrice() {
            return this.estimatedPrice;
        }

        public BigDecimal getSubtotalAmount() {
            return this.subtotalAmount;
        }

        public String getRemark() {
            return this.remark;
        }

        public void setItemId(final String itemId) {
            this.itemId = itemId;
        }

        public void setRequestId(final String requestId) {
            this.requestId = requestId;
        }

        public void setFoodId(final String foodId) {
            this.foodId = foodId;
        }

        public void setFoodName(final String foodName) {
            this.foodName = foodName;
        }

        public void setFoodCode(final String foodCode) {
            this.foodCode = foodCode;
        }

        public void setSpecification(final String specification) {
            this.specification = specification;
        }

        public void setQuantity(final BigDecimal quantity) {
            this.quantity = quantity;
        }

        public void setUnit(final String unit) {
            this.unit = unit;
        }

        public void setEstimatedPrice(final BigDecimal estimatedPrice) {
            this.estimatedPrice = estimatedPrice;
        }

        public void setSubtotalAmount(final BigDecimal subtotalAmount) {
            this.subtotalAmount = subtotalAmount;
        }

        public void setRemark(final String remark) {
            this.remark = remark;
        }

        public String getPlannedReceiverType() {
            return this.plannedReceiverType;
        }

        public String getPlannedStoreId() {
            return this.plannedStoreId;
        }

        public Long getPlannedWarehouseId() {
            return this.plannedWarehouseId;
        }

        public void setPlannedReceiverType(final String plannedReceiverType) {
            this.plannedReceiverType = plannedReceiverType;
        }

        public void setPlannedStoreId(final String plannedStoreId) {
            this.plannedStoreId = plannedStoreId;
        }

        public void setPlannedWarehouseId(final Long plannedWarehouseId) {
            this.plannedWarehouseId = plannedWarehouseId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof PurchaseRequestDTO.PurchaseRequestItemDTO)) return false;
            final PurchaseRequestDTO.PurchaseRequestItemDTO other = (PurchaseRequestDTO.PurchaseRequestItemDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$itemId = this.getItemId();
            final java.lang.Object other$itemId = other.getItemId();
            if (this$itemId == null ? other$itemId != null : !this$itemId.equals(other$itemId)) return false;
            final java.lang.Object this$requestId = this.getRequestId();
            final java.lang.Object other$requestId = other.getRequestId();
            if (this$requestId == null ? other$requestId != null : !this$requestId.equals(other$requestId)) return false;
            final java.lang.Object this$foodId = this.getFoodId();
            final java.lang.Object other$foodId = other.getFoodId();
            if (this$foodId == null ? other$foodId != null : !this$foodId.equals(other$foodId)) return false;
            final java.lang.Object this$foodName = this.getFoodName();
            final java.lang.Object other$foodName = other.getFoodName();
            if (this$foodName == null ? other$foodName != null : !this$foodName.equals(other$foodName)) return false;
            final java.lang.Object this$foodCode = this.getFoodCode();
            final java.lang.Object other$foodCode = other.getFoodCode();
            if (this$foodCode == null ? other$foodCode != null : !this$foodCode.equals(other$foodCode)) return false;
            final java.lang.Object this$specification = this.getSpecification();
            final java.lang.Object other$specification = other.getSpecification();
            if (this$specification == null ? other$specification != null : !this$specification.equals(other$specification)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$unit = this.getUnit();
            final java.lang.Object other$unit = other.getUnit();
            if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) return false;
            final java.lang.Object this$estimatedPrice = this.getEstimatedPrice();
            final java.lang.Object other$estimatedPrice = other.getEstimatedPrice();
            if (this$estimatedPrice == null ? other$estimatedPrice != null : !this$estimatedPrice.equals(other$estimatedPrice)) return false;
            final java.lang.Object this$subtotalAmount = this.getSubtotalAmount();
            final java.lang.Object other$subtotalAmount = other.getSubtotalAmount();
            if (this$subtotalAmount == null ? other$subtotalAmount != null : !this$subtotalAmount.equals(other$subtotalAmount)) return false;
            final java.lang.Object this$remark = this.getRemark();
            final java.lang.Object other$remark = other.getRemark();
            if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
            final java.lang.Object this$plannedReceiverType = this.getPlannedReceiverType();
            final java.lang.Object other$plannedReceiverType = other.getPlannedReceiverType();
            if (this$plannedReceiverType == null ? other$plannedReceiverType != null : !this$plannedReceiverType.equals(other$plannedReceiverType)) return false;
            final java.lang.Object this$plannedStoreId = this.getPlannedStoreId();
            final java.lang.Object other$plannedStoreId = other.getPlannedStoreId();
            if (this$plannedStoreId == null ? other$plannedStoreId != null : !this$plannedStoreId.equals(other$plannedStoreId)) return false;
            final java.lang.Object this$plannedWarehouseId = this.getPlannedWarehouseId();
            final java.lang.Object other$plannedWarehouseId = other.getPlannedWarehouseId();
            if (this$plannedWarehouseId == null ? other$plannedWarehouseId != null : !this$plannedWarehouseId.equals(other$plannedWarehouseId)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof PurchaseRequestDTO.PurchaseRequestItemDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $itemId = this.getItemId();
            result = result * PRIME + ($itemId == null ? 43 : $itemId.hashCode());
            final java.lang.Object $requestId = this.getRequestId();
            result = result * PRIME + ($requestId == null ? 43 : $requestId.hashCode());
            final java.lang.Object $foodId = this.getFoodId();
            result = result * PRIME + ($foodId == null ? 43 : $foodId.hashCode());
            final java.lang.Object $foodName = this.getFoodName();
            result = result * PRIME + ($foodName == null ? 43 : $foodName.hashCode());
            final java.lang.Object $foodCode = this.getFoodCode();
            result = result * PRIME + ($foodCode == null ? 43 : $foodCode.hashCode());
            final java.lang.Object $specification = this.getSpecification();
            result = result * PRIME + ($specification == null ? 43 : $specification.hashCode());
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $unit = this.getUnit();
            result = result * PRIME + ($unit == null ? 43 : $unit.hashCode());
            final java.lang.Object $estimatedPrice = this.getEstimatedPrice();
            result = result * PRIME + ($estimatedPrice == null ? 43 : $estimatedPrice.hashCode());
            final java.lang.Object $subtotalAmount = this.getSubtotalAmount();
            result = result * PRIME + ($subtotalAmount == null ? 43 : $subtotalAmount.hashCode());
            final java.lang.Object $remark = this.getRemark();
            result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
            final java.lang.Object $plannedReceiverType = this.getPlannedReceiverType();
            result = result * PRIME + ($plannedReceiverType == null ? 43 : $plannedReceiverType.hashCode());
            final java.lang.Object $plannedStoreId = this.getPlannedStoreId();
            result = result * PRIME + ($plannedStoreId == null ? 43 : $plannedStoreId.hashCode());
            final java.lang.Object $plannedWarehouseId = this.getPlannedWarehouseId();
            result = result * PRIME + ($plannedWarehouseId == null ? 43 : $plannedWarehouseId.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "PurchaseRequestDTO.PurchaseRequestItemDTO(itemId=" + this.getItemId() + ", requestId=" + this.getRequestId() + ", foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", foodCode=" + this.getFoodCode() + ", specification=" + this.getSpecification() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", estimatedPrice=" + this.getEstimatedPrice() + ", subtotalAmount=" + this.getSubtotalAmount() + ", remark=" + this.getRemark() + ", plannedReceiverType=" + this.getPlannedReceiverType() + ", plannedStoreId=" + this.getPlannedStoreId() + ", plannedWarehouseId=" + this.getPlannedWarehouseId() + ")";
        }
    }

    public PurchaseRequestDTO() {
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

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public List<PurchaseRequestItemDTO> getItems() {
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

    public void setExpectedDate(final LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public void setDescription(final String description) {
        this.description = description;
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

    public void setApprovedTime(final LocalDateTime approvedTime) {
        this.approvedTime = approvedTime;
    }

    public void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    public void setBudgetStatus(final String budgetStatus) {
        this.budgetStatus = budgetStatus;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setItems(final List<PurchaseRequestItemDTO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseRequestDTO)) return false;
        final PurchaseRequestDTO other = (PurchaseRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
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
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
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
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseRequestDTO(requestId=" + this.getRequestId() + ", requestNo=" + this.getRequestNo() + ", title=" + this.getTitle() + ", requestType=" + this.getRequestType() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", applicantId=" + this.getApplicantId() + ", applicantName=" + this.getApplicantName() + ", totalAmount=" + this.getTotalAmount() + ", status=" + this.getStatus() + ", priority=" + this.getPriority() + ", expectedDate=" + this.getExpectedDate() + ", description=" + this.getDescription() + ", rejectReason=" + this.getRejectReason() + ", approvedBy=" + this.getApprovedBy() + ", approvedTime=" + this.getApprovedTime() + ", budgetId=" + this.getBudgetId() + ", budgetStatus=" + this.getBudgetStatus() + ", createTime=" + this.getCreateTime() + ", createBy=" + this.getCreateBy() + ", updateTime=" + this.getUpdateTime() + ", items=" + this.getItems() + ")";
    }
}
