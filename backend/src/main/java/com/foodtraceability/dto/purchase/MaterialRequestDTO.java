package com.foodtraceability.dto.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物资需求提报视图对象
 *
 * <p>状态字段 status 为后端数字编码（0~4），由前端 DataConverter 转换为字符串。</p>
 * <p>金额字段 totalAmount / estimatedPrice / subtotalAmount 以"分"为单位，由前端 DataConverter 转换为"元"。</p>
 * <p>ID 字段为 Long 类型，由前端 DataConverter 转换为 string（避免大数精度丢失）。</p>
 */
@Schema(description = "物资需求提报视图对象")
public class MaterialRequestDTO {

    /** 提报ID */
    @Schema(description = "提报ID")
    private Long requestId;

    /** 提报单号 */
    @Schema(description = "提报单号")
    private String requestNo;

    /** 需求标题 */
    @Schema(description = "需求标题")
    private String title;

    /** 提报门店 */
    @Schema(description = "提报门店")
    private String storeName;

    /** 申请人ID */
    @Schema(description = "申请人ID")
    private Long applicantId;

    /** 申请人姓名 */
    @Schema(description = "申请人姓名")
    private String applicantName;

    /** 期望到货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "期望到货日期")
    private LocalDate expectedDate;

    /** 状态：0草稿 1待审核 2已审核 3已驳回 4已转采购申请 */
    @Schema(description = "状态")
    private Integer status;

    /** 总金额（单位：分） */
    @Schema(description = "总金额（分）")
    private Long totalAmount;

    /** 转换后的采购申请单号 */
    @Schema(description = "转换后的采购申请单号")
    private String convertedRequestNo;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

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
    private List<MaterialRequestItemVO> items;

    // ==================== Getter & Setter ====================

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getConvertedRequestNo() {
        return convertedRequestNo;
    }

    public void setConvertedRequestNo(String convertedRequestNo) {
        this.convertedRequestNo = convertedRequestNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public List<MaterialRequestItemVO> getItems() {
        return items;
    }

    public void setItems(List<MaterialRequestItemVO> items) {
        this.items = items;
    }

    /**
     * 物资需求提报明细 VO（内嵌）
     */
    @Schema(description = "物资需求提报明细 VO")
    public static class MaterialRequestItemVO {

        @Schema(description = "明细ID")
        private Long itemId;

        @Schema(description = "提报ID")
        private Long requestId;

        @Schema(description = "物料ID")
        private Long materialId;

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

        @Schema(description = "小计金额（分）")
        private Long subtotalAmount;

        @Schema(description = "备注")
        private String remark;

        // Getter & Setter
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
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
        public Long getSubtotalAmount() { return subtotalAmount; }
        public void setSubtotalAmount(Long subtotalAmount) { this.subtotalAmount = subtotalAmount; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
