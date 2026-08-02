package com.foodtraceability.dto.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购计划视图对象
 *
 * <p>状态字段 status 为后端数字编码（0~5），由前端 DataConverter 转换为字符串。</p>
 * <p>金额字段 totalAmount / estimatedPrice 以"分"为单位，由前端 DataConverter 转换为"元"。</p>
 */
@Schema(description = "采购计划视图对象")
public class PurchasePlanVO {

    /** 计划ID */
    @Schema(description = "计划ID")
    private Long planId;

    /** 计划编号 */
    @Schema(description = "计划编号")
    private String planNo;

    /** 计划日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "计划日期")
    private LocalDate planDate;

    /** 部门ID */
    @Schema(description = "部门ID")
    private Long departmentId;

    /** 部门名称 */
    @Schema(description = "部门名称")
    private String departmentName;

    /** 总金额（单位：分） */
    @Schema(description = "总金额（分）")
    private Long totalAmount;

    /** 物料项数 */
    @Schema(description = "物料项数")
    private Integer itemCount;

    /** 创建人ID */
    @Schema(description = "创建人ID")
    private Long creatorId;

    /** 创建人名称 */
    @Schema(description = "创建人名称")
    private String creatorName;

    /** 状态：0草稿 1待审批 2已审批 3执行中 4已完成 5已拒绝 */
    @Schema(description = "状态")
    private Integer status;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 审批人 */
    @Schema(description = "审批人")
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    /** 拒绝原因 */
    @Schema(description = "拒绝原因")
    private String rejectReason;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 明细列表（详情接口返回） */
    @Schema(description = "明细列表")
    private List<PurchasePlanItemVO> items;

    // ==================== Getter & Setter ====================

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getApproveBy() {
        return approveBy;
    }

    public void setApproveBy(String approveBy) {
        this.approveBy = approveBy;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
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

    public List<PurchasePlanItemVO> getItems() {
        return items;
    }

    public void setItems(List<PurchasePlanItemVO> items) {
        this.items = items;
    }

    /**
     * 采购计划明细 VO（内嵌）
     */
    @Schema(description = "采购计划明细 VO")
    public static class PurchasePlanItemVO {

        @Schema(description = "明细ID")
        private Long itemId;

        @Schema(description = "物料ID")
        private String materialId;

        @Schema(description = "物料名称")
        private String materialName;

        @Schema(description = "规格型号")
        private String specification;

        @Schema(description = "数量")
        private BigDecimal quantity;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "预估单价（分）")
        private Long estimatedPrice;

        @Schema(description = "是否临时物料")
        private Integer isTempMaterial;

        @Schema(description = "建议供应商ID")
        private Long supplierId;

        @Schema(description = "建议供应商名称")
        private String supplierName;

        @Schema(description = "备注")
        private String remark;

        // Getter & Setter
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public String getSpecification() { return specification; }
        public void setSpecification(String specification) { this.specification = specification; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Long getEstimatedPrice() { return estimatedPrice; }
        public void setEstimatedPrice(Long estimatedPrice) { this.estimatedPrice = estimatedPrice; }
        public Integer getIsTempMaterial() { return isTempMaterial; }
        public void setIsTempMaterial(Integer isTempMaterial) { this.isTempMaterial = isTempMaterial; }
        public Long getSupplierId() { return supplierId; }
        public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
