package com.foodtraceability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购申请创建请求DTO
 */
public class PurchaseRequestCreateDTO {
    /**
     * 申请标题
     */
    @NotBlank(message = "申请标题不能为空")
    @Size(min = 2, max = 100, message = "标题长度必须在2到100个字符之间")
    private String title;
    /**
     * 申请类型：routine(日常)/emergency(紧急)/project(项目)
     */
    private String requestType;
    /**
     * 申请部门ID
     */
    private String departmentId;
    /**
     * 申请部门名称
     */
    private String departmentName;
    /**
     * 申请所属门店ID
     */
    private String storeId;
    /**
     * 申请人ID
     */
    private String applicantId;
    /**
     * 申请人姓名
     */
    private String applicantName;
    /**
     * 优先级：low(低)/normal(普通)/high(高)/urgent(紧急)
     */
    private String priority;
    /**
     * 期望到货日期
     */
    @Future(message = "期望到货日期必须是未来的日期")
    private LocalDate expectedDate;
    /**
     * 申请说明
     */
    @Size(max = 500, message = "申请说明不能超过500个字符")
    private String description;
    /**
     * 关联预算ID
     */
    private String budgetId;
    /**
     * 申请明细列表
     */
    @NotEmpty(message = "申请明细不能为空")
    @Valid
    private List<PurchaseRequestItemDTO> items;


    /**
     * 采购申请明细DTO
     */
    public static class PurchaseRequestItemDTO {
        /**
         * 商品ID
         */
        private String foodId;
        /**
         * 商品名称
         */
        @NotBlank(message = "商品名称不能为空")
        @Size(max = 100, message = "商品名称不能超过100个字符")
        private String foodName;
        /**
         * 商品编码
         */
        private String foodCode;
        /**
         * 规格型号
         */
        @Size(max = 100, message = "规格型号不能超过100个字符")
        private String specification;
        /**
         * 申请数量
         */
        @NotNull(message = "申请数量不能为空")
        @DecimalMin(value = "0.01", message = "申请数量必须大于0")
        @Digits(integer = 10, fraction = 2, message = "数量格式不正确")
        private BigDecimal quantity;
        /**
         * 单位
         */
        @Size(max = 20, message = "单位不能超过20个字符")
        private String unit;
        /**
         * 预估单价
         */
        @DecimalMin(value = "0", message = "预估单价不能为负数")
        @Digits(integer = 10, fraction = 2, message = "价格格式不正确")
        private BigDecimal estimatedPrice;
        /**
         * 备注
         */
        @Size(max = 500, message = "备注不能超过500个字符")
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

        /**
         * 商品ID
         */
        public String getFoodId() {
            return this.foodId;
        }

        /**
         * 商品名称
         */
        public String getFoodName() {
            return this.foodName;
        }

        /**
         * 商品编码
         */
        public String getFoodCode() {
            return this.foodCode;
        }

        /**
         * 规格型号
         */
        public String getSpecification() {
            return this.specification;
        }

        /**
         * 申请数量
         */
        public BigDecimal getQuantity() {
            return this.quantity;
        }

        /**
         * 单位
         */
        public String getUnit() {
            return this.unit;
        }

        /**
         * 预估单价
         */
        public BigDecimal getEstimatedPrice() {
            return this.estimatedPrice;
        }

        /**
         * 备注
         */
        public String getRemark() {
            return this.remark;
        }

        /**
         * 计划收货方类型
         */
        public String getPlannedReceiverType() {
            return this.plannedReceiverType;
        }

        /**
         * 计划收货门店ID
         */
        public String getPlannedStoreId() {
            return this.plannedStoreId;
        }

        /**
         * 计划收货仓库ID
         */
        public Long getPlannedWarehouseId() {
            return this.plannedWarehouseId;
        }

        /**
         * 商品ID
         */
        public void setFoodId(final String foodId) {
            this.foodId = foodId;
        }

        /**
         * 商品名称
         */
        public void setFoodName(final String foodName) {
            this.foodName = foodName;
        }

        /**
         * 商品编码
         */
        public void setFoodCode(final String foodCode) {
            this.foodCode = foodCode;
        }

        /**
         * 规格型号
         */
        public void setSpecification(final String specification) {
            this.specification = specification;
        }

        /**
         * 申请数量
         */
        public void setQuantity(final BigDecimal quantity) {
            this.quantity = quantity;
        }

        /**
         * 单位
         */
        public void setUnit(final String unit) {
            this.unit = unit;
        }

        /**
         * 预估单价
         */
        public void setEstimatedPrice(final BigDecimal estimatedPrice) {
            this.estimatedPrice = estimatedPrice;
        }

        /**
         * 备注
         */
        public void setRemark(final String remark) {
            this.remark = remark;
        }

        /**
         * 计划收货方类型
         */
        public void setPlannedReceiverType(final String plannedReceiverType) {
            this.plannedReceiverType = plannedReceiverType;
        }

        /**
         * 计划收货门店ID
         */
        public void setPlannedStoreId(final String plannedStoreId) {
            this.plannedStoreId = plannedStoreId;
        }

        /**
         * 计划收货仓库ID
         */
        public void setPlannedWarehouseId(final Long plannedWarehouseId) {
            this.plannedWarehouseId = plannedWarehouseId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof PurchaseRequestCreateDTO.PurchaseRequestItemDTO)) return false;
            final PurchaseRequestCreateDTO.PurchaseRequestItemDTO other = (PurchaseRequestCreateDTO.PurchaseRequestItemDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
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
            final java.lang.Object this$remark = this.getRemark();
            final java.lang.Object other$remark = other.getRemark();
            if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof PurchaseRequestCreateDTO.PurchaseRequestItemDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
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
            final java.lang.Object $remark = this.getRemark();
            result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "PurchaseRequestCreateDTO.PurchaseRequestItemDTO(foodId=" + this.getFoodId() + ", foodName=" + this.getFoodName() + ", foodCode=" + this.getFoodCode() + ", specification=" + this.getSpecification() + ", quantity=" + this.getQuantity() + ", unit=" + this.getUnit() + ", estimatedPrice=" + this.getEstimatedPrice() + ", remark=" + this.getRemark() + ")";
        }
    }

    public PurchaseRequestCreateDTO() {
    }

    /**
     * 申请标题
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * 申请类型：routine(日常)/emergency(紧急)/project(项目)
     */
    public String getRequestType() {
        return this.requestType;
    }

    /**
     * 申请部门ID
     */
    public String getDepartmentId() {
        return this.departmentId;
    }

    /**
     * 申请部门名称
     */
    public String getDepartmentName() {
        return this.departmentName;
    }

    /**
     * 申请所属门店ID
     */
    public String getStoreId() {
        return this.storeId;
    }

    /**
     * 申请人ID
     */
    public String getApplicantId() {
        return this.applicantId;
    }

    /**
     * 申请人姓名
     */
    public String getApplicantName() {
        return this.applicantName;
    }

    /**
     * 优先级：low(低)/normal(普通)/high(高)/urgent(紧急)
     */
    public String getPriority() {
        return this.priority;
    }

    /**
     * 期望到货日期
     */
    public LocalDate getExpectedDate() {
        return this.expectedDate;
    }

    /**
     * 申请说明
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 关联预算ID
     */
    public String getBudgetId() {
        return this.budgetId;
    }

    /**
     * 申请明细列表
     */
    public List<PurchaseRequestItemDTO> getItems() {
        return this.items;
    }

    /**
     * 申请标题
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * 申请类型：routine(日常)/emergency(紧急)/project(项目)
     */
    public void setRequestType(final String requestType) {
        this.requestType = requestType;
    }

    /**
     * 申请部门ID
     */
    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * 申请部门名称
     */
    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    /**
     * 申请所属门店ID
     */
    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    /**
     * 申请人ID
     */
    public void setApplicantId(final String applicantId) {
        this.applicantId = applicantId;
    }

    /**
     * 申请人姓名
     */
    public void setApplicantName(final String applicantName) {
        this.applicantName = applicantName;
    }

    /**
     * 优先级：low(低)/normal(普通)/high(高)/urgent(紧急)
     */
    public void setPriority(final String priority) {
        this.priority = priority;
    }

    /**
     * 期望到货日期
     */
    public void setExpectedDate(final LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    /**
     * 申请说明
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * 关联预算ID
     */
    public void setBudgetId(final String budgetId) {
        this.budgetId = budgetId;
    }

    /**
     * 申请明细列表
     */
    public void setItems(final List<PurchaseRequestItemDTO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PurchaseRequestCreateDTO)) return false;
        final PurchaseRequestCreateDTO other = (PurchaseRequestCreateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
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
        final java.lang.Object this$priority = this.getPriority();
        final java.lang.Object other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !this$priority.equals(other$priority)) return false;
        final java.lang.Object this$expectedDate = this.getExpectedDate();
        final java.lang.Object other$expectedDate = other.getExpectedDate();
        if (this$expectedDate == null ? other$expectedDate != null : !this$expectedDate.equals(other$expectedDate)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$budgetId = this.getBudgetId();
        final java.lang.Object other$budgetId = other.getBudgetId();
        if (this$budgetId == null ? other$budgetId != null : !this$budgetId.equals(other$budgetId)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PurchaseRequestCreateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
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
        final java.lang.Object $priority = this.getPriority();
        result = result * PRIME + ($priority == null ? 43 : $priority.hashCode());
        final java.lang.Object $expectedDate = this.getExpectedDate();
        result = result * PRIME + ($expectedDate == null ? 43 : $expectedDate.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $budgetId = this.getBudgetId();
        result = result * PRIME + ($budgetId == null ? 43 : $budgetId.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PurchaseRequestCreateDTO(title=" + this.getTitle() + ", requestType=" + this.getRequestType() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", applicantId=" + this.getApplicantId() + ", applicantName=" + this.getApplicantName() + ", priority=" + this.getPriority() + ", expectedDate=" + this.getExpectedDate() + ", description=" + this.getDescription() + ", budgetId=" + this.getBudgetId() + ", items=" + this.getItems() + ")";
    }
}
